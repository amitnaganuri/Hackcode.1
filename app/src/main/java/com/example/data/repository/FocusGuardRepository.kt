package com.example.data.repository

import com.example.data.local.FocusGuardPreferences
import com.example.data.local.PersistedSession
import com.example.data.model.AppInterventionStat
import com.example.data.model.BlockMode
import com.example.data.model.BlockedAppRule
import com.example.data.model.DailyStats
import com.example.data.model.DistractionAttempt
import com.example.data.model.FocusSession
import com.example.data.model.InterventionConfig
import com.example.data.model.WeeklyBalanceDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Single source of truth for FocusGuard state.
 *
 * State is held in memory as [MutableStateFlow] so that synchronous `.value` reads stay
 * available (the blocking engine needs them from an AccessibilityService callback), and
 * every mutation is written through to [FocusGuardPreferences]. On construction the
 * in-memory state is hydrated from disk; until that completes the seeded defaults are
 * shown, which is visually identical to the persisted first-run values.
 */
class FocusGuardRepository(
    private val preferences: FocusGuardPreferences,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis
) {

    private val _rules = MutableStateFlow(defaultRules)
    val rules: StateFlow<List<BlockedAppRule>> = _rules.asStateFlow()

    private val _shortsAndReelsMasterShield = MutableStateFlow(true)
    val shortsAndReelsMasterShield: StateFlow<Boolean> = _shortsAndReelsMasterShield.asStateFlow()

    /**
     * Wall-clock backed session. Null when no session is configured. The UI-facing
     * [FocusSession] is recomputed from this on every tick.
     */
    private val _persistedSession = MutableStateFlow<PersistedSession?>(null)

    private val _activeSession = MutableStateFlow<FocusSession?>(null)
    val activeSession: StateFlow<FocusSession?> = _activeSession.asStateFlow()

    private val _interventionConfig = MutableStateFlow(InterventionConfig())
    val interventionConfig: StateFlow<InterventionConfig> = _interventionConfig.asStateFlow()

    private val _dailyStats = MutableStateFlow(DailyStats())
    val dailyStats: StateFlow<DailyStats> = _dailyStats.asStateFlow()

    private val _weeklyBalance = MutableStateFlow(defaultWeeklyBalance)
    val weeklyBalance: StateFlow<List<WeeklyBalanceDay>> = _weeklyBalance.asStateFlow()

    private val _appInterventions = MutableStateFlow(defaultAppInterventions)
    val appInterventions: StateFlow<List<AppInterventionStat>> = _appInterventions.asStateFlow()

    /** Interceptions recorded by the blocking engine, newest first. */
    private val _distractionAttempts = MutableStateFlow<List<DistractionAttempt>>(emptyList())
    val distractionAttempts: StateFlow<List<DistractionAttempt>> = _distractionAttempts.asStateFlow()

    private var timerJob: Job? = null

    init {
        scope.launch { hydrate() }
        startTimerLoop()
    }

    // ---------------------------------------------------------------- hydration

    private suspend fun hydrate() {
        // Seed the demo rule set exactly once, so a user who deletes every rule does not
        // get them resurrected on the next launch.
        if (!preferences.seededFlow().first()) {
            preferences.saveRules(defaultRules)
            preferences.markSeeded()
        }

        _rules.value = preferences.rulesFlow(defaultRules).first()
        _shortsAndReelsMasterShield.value = preferences.masterShieldFlow(true).first()
        _interventionConfig.value = preferences.interventionFlow(InterventionConfig()).first()

        val goal = preferences.goalFlow(DailyStats().todayGoalText).first()
        _dailyStats.update { it.copy(todayGoalText = goal) }

        _persistedSession.value = preferences.sessionFlow().first()
        recomputeSession()

        _distractionAttempts.value = preferences.attemptsFlow().first()
        refreshAttemptStats()

        // Keep in-memory state aligned with any other writer of the same DataStore file.
        // The accessibility service in later phases runs in this process but may hold its
        // own repository reference.
        scope.launch {
            combine(
                preferences.rulesFlow(defaultRules),
                preferences.masterShieldFlow(true)
            ) { rules, shield -> rules to shield }
                .collect { (rules, shield) ->
                    _rules.value = rules
                    _shortsAndReelsMasterShield.value = shield
                }
        }
    }

    // ------------------------------------------------------------------- timer

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                recomputeSession()
                delay(1000)
            }
        }
    }

    /**
     * Derives the UI session from the persisted end instant. Because remaining time is
     * computed from the clock rather than decremented, the countdown stays accurate
     * across backgrounding, doze and process death.
     */
    private fun recomputeSession() {
        val persisted = _persistedSession.value
        if (persisted == null) {
            _activeSession.value = null
            return
        }
        val remaining = persisted.remainingSecondsAt(clock())
        _activeSession.value = FocusSession(
            id = persisted.id,
            title = persisted.title,
            targetGoal = persisted.targetGoal,
            remainingSeconds = remaining,
            totalDurationSeconds = persisted.totalDurationSeconds,
            isRunning = remaining > 0,
            isPaused = persisted.isPaused,
            blockedApps = persisted.blockedApps,
            endTimeText = formatClockTime(persisted.endsAtEpochMillis)
        )
    }

    // -------------------------------------------------------------- rules CRUD

    fun addRule(rule: BlockedAppRule) {
        _rules.update { it + rule }
        persistRules()
    }

    fun updateRule(updated: BlockedAppRule) {
        _rules.update { list -> list.map { if (it.id == updated.id) updated else it } }
        persistRules()
    }

    fun deleteRule(ruleId: String) {
        _rules.update { list -> list.filterNot { it.id == ruleId } }
        persistRules()
    }

    fun toggleRule(ruleId: String) {
        _rules.update { list ->
            list.map { rule -> if (rule.id == ruleId) rule.copy(isEnabled = !rule.isEnabled) else rule }
        }
        persistRules()
    }

    fun ruleById(ruleId: String): BlockedAppRule? = _rules.value.firstOrNull { it.id == ruleId }

    private fun persistRules() {
        val snapshot = _rules.value
        scope.launch { preferences.saveRules(snapshot) }
    }

    // ------------------------------------------------------------------ shield

    fun toggleShortsAndReelsMasterShield() {
        val next = !_shortsAndReelsMasterShield.value
        _shortsAndReelsMasterShield.value = next
        scope.launch { preferences.saveMasterShield(next) }
    }

    // -------------------------------------------------------------------- goal

    fun updateGoal(newGoal: String) {
        _dailyStats.update { it.copy(todayGoalText = newGoal) }
        _persistedSession.update { it?.copy(targetGoal = newGoal) }
        recomputeSession()
        val session = _persistedSession.value
        scope.launch {
            preferences.saveGoal(newGoal)
            preferences.saveSession(session)
        }
    }

    // ----------------------------------------------------------------- session

    fun startDeepFocus(durationMinutes: Int) {
        val now = clock()
        val totalSeconds = durationMinutes * 60
        val session = PersistedSession(
            id = "session_" + now,
            title = "Deep Work",
            targetGoal = _dailyStats.value.todayGoalText,
            totalDurationSeconds = totalSeconds,
            endsAtEpochMillis = now + totalSeconds * 1000L,
            pausedAtEpochMillis = null,
            blockedApps = _rules.value
                .filter { it.isEnabled }
                .map { it.appName + " (" + it.filterLabel + ")" }
        )
        _persistedSession.value = session
        recomputeSession()
        scope.launch { preferences.saveSession(session) }
    }

    fun endActiveSession() {
        _persistedSession.value = null
        _activeSession.value = null
        scope.launch { preferences.saveSession(null) }
    }

    fun togglePauseSession() {
        val now = clock()
        _persistedSession.update { current ->
            when {
                current == null -> null
                current.isPaused -> current.resumedAt(now)
                else -> current.copy(pausedAtEpochMillis = now)
            }
        }
        recomputeSession()
        val session = _persistedSession.value
        scope.launch { preferences.saveSession(session) }
    }

    // ------------------------------------------------------- distraction log

    /**
     * Records an interception and bumps the matching rule's counter.
     *
     * Called from the accessibility service, so it must be safe off the main thread and
     * must not block: state is updated in memory immediately and persisted on the
     * application scope.
     */
    fun recordDistractionAttempt(
        packageName: String,
        appName: String,
        ruleId: String,
        ruleLabel: String,
        blockMode: BlockMode,
        sessionId: String?,
        nowMillis: Long = clock()
    ) {
        val attempt = DistractionAttempt(
            id = "attempt_" + nowMillis,
            packageName = packageName,
            appName = appName,
            timestampEpochMillis = nowMillis,
            ruleId = ruleId,
            ruleLabel = ruleLabel,
            sessionId = sessionId,
            blockMode = blockMode
        )

        // Newest first, capped so the persisted JSON cannot grow without bound.
        _distractionAttempts.update { (listOf(attempt) + it).take(MAX_STORED_ATTEMPTS) }
        _rules.update { list ->
            list.map { rule ->
                if (rule.id == ruleId) {
                    rule.copy(attemptsBlockedCount = rule.attemptsBlockedCount + 1)
                } else rule
            }
        }
        refreshAttemptStats()

        val attempts = _distractionAttempts.value
        val rules = _rules.value
        scope.launch {
            preferences.saveAttempts(attempts)
            preferences.saveRules(rules)
        }
    }

    /** Recomputes the stats the dashboard derives from the attempt log. */
    private fun refreshAttemptStats() {
        val startOfDay = startOfToday(clock())
        val todayCount = _distractionAttempts.value.count { it.timestampEpochMillis >= startOfDay }
        _dailyStats.update {
            it.copy(
                blockedAttemptsToday = todayCount,
                totalBlockedAllTime = _distractionAttempts.value.size
            )
        }
    }

    private fun startOfToday(nowMillis: Long): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    // ------------------------------------------------------------ intervention

    fun updateInterventionConfig(updated: InterventionConfig) {
        _interventionConfig.value = updated
        scope.launch { preferences.saveIntervention(updated) }
    }

    // ----------------------------------------------------------------- helpers

    private fun formatClockTime(epochMillis: Long): String {
        val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return format.format(java.util.Date(epochMillis))
    }

    private companion object {
        const val MAX_STORED_ATTEMPTS = 500

        val defaultRules = listOf(
            BlockedAppRule(
                id = "rule_instagram",
                appName = "Instagram",
                packageName = "com.instagram.android",
                blockMode = BlockMode.CONTENT_LEVEL,
                filterLabel = "Reels & Explore Only",
                scheduleText = "9:00 AM – 1:00 PM • Mon–Fri",
                isEnabled = true,
                attemptsBlockedCount = 27,
                category = "Social"
            ),
            BlockedAppRule(
                id = "rule_youtube",
                appName = "YouTube",
                packageName = "com.google.android.youtube",
                blockMode = BlockMode.CONTENT_LEVEL,
                filterLabel = "Shorts Only",
                scheduleText = "9:00 AM – 6:00 PM • Weekdays",
                isEnabled = true,
                attemptsBlockedCount = 14,
                category = "Video"
            ),
            BlockedAppRule(
                id = "rule_tiktok",
                appName = "TikTok",
                packageName = "com.zhiliaoapp.musically",
                blockMode = BlockMode.HARD_BLOCK,
                filterLabel = "Entire App Blocked",
                scheduleText = "All Day • Daily Permanent Shield",
                isEnabled = true,
                attemptsBlockedCount = 6,
                category = "Entertainment"
            ),
            BlockedAppRule(
                id = "rule_twitter",
                appName = "Twitter / X",
                packageName = "com.twitter.android",
                blockMode = BlockMode.DAILY_ALLOWANCE,
                filterLabel = "20 min / day allowance",
                scheduleText = "Daily Usage Cap",
                isEnabled = true,
                usedMinutes = 14,
                totalAllowedMinutes = 20,
                attemptsBlockedCount = 0,
                category = "Social"
            )
        )

        val defaultWeeklyBalance = listOf(
            WeeklyBalanceDay("M", 0.65f, 0.45f),
            WeeklyBalanceDay("T", 0.80f, 0.30f),
            WeeklyBalanceDay("W", 0.95f, 0.20f, isHighlighted = true),
            WeeklyBalanceDay("T", 0.70f, 0.40f),
            WeeklyBalanceDay("F", 0.55f, 0.60f),
            WeeklyBalanceDay("S", 0.85f, 0.15f),
            WeeklyBalanceDay("S", 0.40f, 0.10f)
        )

        val defaultAppInterventions = listOf(
            AppInterventionStat("Instagram", "18 attempts intercepted", 18, "1h 12m", "↓ 34% drop", 0.38f),
            AppInterventionStat("YouTube", "14 attempts intercepted", 14, "48m", "↓ 21% drop", 0.26f),
            AppInterventionStat("TikTok", "7 attempts intercepted", 7, "32m", "↓ 41% drop", 0.16f)
        )
    }
}
