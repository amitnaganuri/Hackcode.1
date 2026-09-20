package com.example.data.repository

import com.example.data.local.FocusGuardPreferences
import com.example.data.local.PersistedSession
import com.example.data.model.AppInterventionStat
import com.example.data.model.BlockMode
import com.example.data.model.BlockedAppRule
import com.example.data.model.DailyStats
import com.example.data.model.DailyUsageSnapshot
import com.example.data.analytics.FocusAnalytics
import com.example.data.model.DistractionAttempt
import com.example.data.model.FocusSessionRecord
import com.example.data.model.FocusSession
import com.example.data.model.InterventionConfig
import com.example.data.model.WeeklyBalanceDay
import kotlinx.coroutines.CompletableDeferred
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

    /** When on, rules cannot be changed while a focus session is running. */
    private val _isStrictModeEnabled = MutableStateFlow(true)
    val isStrictModeEnabled: StateFlow<Boolean> = _isStrictModeEnabled.asStateFlow()

    private val _profileName = MutableStateFlow(DEFAULT_PROFILE_NAME)
    val profileName: StateFlow<String> = _profileName.asStateFlow()

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

    /** Foreground time per package per day. Today's entry drives allowance rules. */
    private val _usageHistory = MutableStateFlow<List<DailyUsageSnapshot>>(emptyList())
    val usageHistory: StateFlow<List<DailyUsageSnapshot>> = _usageHistory.asStateFlow()

    /** Focus sessions that have finished, the basis of every focus-time figure. */
    private val _sessionLog = MutableStateFlow<List<FocusSessionRecord>>(emptyList())
    val sessionLog: StateFlow<List<FocusSessionRecord>> = _sessionLog.asStateFlow()

    /** Interceptions recorded by the blocking engine, newest first. */
    private val _distractionAttempts = MutableStateFlow<List<DistractionAttempt>>(emptyList())
    val distractionAttempts: StateFlow<List<DistractionAttempt>> = _distractionAttempts.asStateFlow()

    private var timerJob: Job? = null

    /**
     * Completes once state has been read from disk.
     *
     * The accessibility service begins recording foreground time the moment it
     * connects, which can be before hydration finishes. Without this gate those early
     * writes started from an empty list and the persisted history was overwritten,
     * silently wiping the day's screen time.
     */
    private val hydrationComplete = CompletableDeferred<Unit>()

    /** Guards against filing the same completed session more than once. */
    private val completedSessionIds = mutableSetOf<String>()

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
        _isStrictModeEnabled.value = preferences.strictModeFlow(true).first()
        _profileName.value = preferences.profileNameFlow(DEFAULT_PROFILE_NAME).first()

        val goal = preferences.goalFlow(DailyStats().todayGoalText).first()
        _dailyStats.update { it.copy(todayGoalText = goal) }

        _persistedSession.value = preferences.sessionFlow().first()
        recomputeSession()

        _distractionAttempts.value = preferences.attemptsFlow().first()
        refreshAttemptStats()

        // History is kept so the dashboard can compare days; the allowance counters
        // still only ever read today's entry, so they reset at midnight on their own.
        _usageHistory.value = mergeByDate(preferences.usageHistoryFlow().first())
        _sessionLog.value = preferences.sessionLogFlow().first()
        refreshRuleUsage()
        refreshAttemptStats()

        hydrationComplete.complete(Unit)

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
                    // The persisted rules carry whatever usedMinutes was last written.
                    // Without re-applying live usage here, this collector would keep
                    // stomping the counter back to a stale value and the "Usage Today"
                    // figure would never move.
                    refreshRuleUsage()
                }
        }
    }

    // ------------------------------------------------------------------- timer

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var tick = 0
            while (true) {
                recomputeSession()
                // Focus time earned by a running session should be visible on Home
                // while it runs, not only once it ends. Recomputed every few seconds
                // rather than every tick because it walks the whole attempt log.
                if (tick % STATS_REFRESH_TICKS == 0) refreshAttemptStats()
                tick++
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

        // The timer reaching zero is the only moment a session completes on its own,
        // and it has to be filed exactly once or the focus total would keep growing.
        if (remaining <= 0 && !completedSessionIds.contains(persisted.id)) {
            completedSessionIds.add(persisted.id)
            recordFinishedSession(completed = true)
            refreshAttemptStats()
        }

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

    /**
     * Stores a screen signature captured on this device for [ruleId].
     *
     * Called from the accessibility service when a "Teach this screen" session ends.
     */
    fun applyLearnedSignals(ruleId: String, signals: List<String>) {
        if (signals.isEmpty()) return
        _rules.update { list ->
            list.map { rule ->
                if (rule.id == ruleId) rule.copy(learnedViewIds = signals) else rule
            }
        }
        persistRules()
    }

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

    fun setStrictMode(enabled: Boolean) {
        _isStrictModeEnabled.value = enabled
        scope.launch { preferences.saveStrictMode(enabled) }
    }

    fun setProfileName(name: String) {
        val trimmed = name.trim().ifBlank { DEFAULT_PROFILE_NAME }
        _profileName.value = trimmed
        scope.launch { preferences.saveProfileName(trimmed) }
    }

    /**
     * True when rules must not be edited right now.
     *
     * Strict Mode exists to stop a rule being switched off the moment it becomes
     * inconvenient, so it only bites while a focus session is actually running.
     */
    fun areRuleChangesLocked(): Boolean {
        if (!_isStrictModeEnabled.value) return false
        val session = _activeSession.value ?: return false
        return session.isRunning && !session.isPaused
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
        recordFinishedSession(completed = false)
        _persistedSession.value = null
        _activeSession.value = null
        refreshAttemptStats()
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

    // ------------------------------------------------------------ app usage

    /**
     * Adds foreground time for a package. Called by the accessibility service whenever
     * the user leaves an app, and periodically while they stay in one.
     */
    fun addForegroundTime(packageName: String, millis: Long) {
        if (millis <= 0L) return
        scope.launch {
            hydrationComplete.await()
            applyForegroundTime(packageName, millis)
        }
    }

    private fun applyForegroundTime(packageName: String, millis: Long) {
        val today = todayKey()
        _usageHistory.update { history ->
            val existing = history.firstOrNull { it.dateKey == today }
                ?: DailyUsageSnapshot(today)
            val updated = existing.plus(packageName, millis)
            mergeByDate(listOf(updated) + history.filterNot { it.dateKey == today })
                .take(MAX_USAGE_DAYS)
        }
        refreshRuleUsage()
        refreshAttemptStats()

        val historySnapshot = _usageHistory.value
        val rulesSnapshot = _rules.value
        scope.launch {
            preferences.saveUsageHistory(historySnapshot)
            // Persist the rules too, so the counter survives a restart instead of
            // reverting to the value from the last rule edit.
            preferences.saveRules(rulesSnapshot)
        }
    }

    /**
     * Collapses any duplicate day entries into one.
     *
     * A day must appear at most once or every total built from the history counts that
     * day twice. Duplicates can survive a write that raced with hydration, so the list
     * is normalised on the way in rather than trusted.
     */
    private fun mergeByDate(history: List<DailyUsageSnapshot>): List<DailyUsageSnapshot> =
        history.groupBy { it.dateKey }
            .map { (dateKey, sameDay) ->
                if (sameDay.size == 1) {
                    sameDay.first()
                } else {
                    val merged = mutableMapOf<String, Long>()
                    sameDay.forEach { snapshot ->
                        snapshot.millisByPackage.forEach { (key, millis) ->
                            merged[key] = (merged[key] ?: 0L) + millis
                        }
                    }
                    DailyUsageSnapshot(dateKey, merged)
                }
            }
            .sortedByDescending { it.dateKey }

    fun todayUsage(): DailyUsageSnapshot =
        _usageHistory.value.firstOrNull { it.dateKey == todayKey() }
            ?: DailyUsageSnapshot(todayKey())

    fun usedMinutesToday(packageName: String): Int = todayUsage().minutesFor(packageName)

    /** Mirrors today's usage onto the rules so the allowance progress bars are real. */
    private fun refreshRuleUsage() {
        val usage = todayUsage()
        _rules.update { list ->
            list.map { rule ->
                val used = usage.minutesFor(rule.packageName)
                if (rule.usedMinutes == used) rule else rule.copy(usedMinutes = used)
            }
        }
    }

    private fun todayKey(): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return format.format(java.util.Date(clock()))
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
        scope.launch {
            hydrationComplete.await()
            applyDistractionAttempt(
                packageName, appName, ruleId, ruleLabel, blockMode, sessionId, nowMillis
            )
        }
    }

    private fun applyDistractionAttempt(
        packageName: String,
        appName: String,
        ruleId: String,
        ruleLabel: String,
        blockMode: BlockMode,
        sessionId: String?,
        nowMillis: Long
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

    /** Recomputes every dashboard figure from the real logs. */
    private fun refreshAttemptStats() {
        val attempts = _distractionAttempts.value
        val now = clock()

        _dailyStats.value = FocusAnalytics.dailyStats(
            base = _dailyStats.value,
            attempts = attempts,
            sessions = _sessionLog.value,
            usageHistory = _usageHistory.value,
            rules = _rules.value,
            runningSessionSeconds = elapsedSecondsOfRunningSession(now),
            nowMillis = now
        )

        _appInterventions.value = FocusAnalytics.appInterventions(attempts)
        _weeklyBalance.value = FocusAnalytics.weeklyBalance(attempts, _sessionLog.value, now)
    }

    /** Focus already banked by the session in progress, so Home updates live. */
    private fun elapsedSecondsOfRunningSession(nowMillis: Long): Int {
        val persisted = _persistedSession.value ?: return 0
        val remaining = persisted.remainingSecondsAt(nowMillis)
        return (persisted.totalDurationSeconds - remaining).coerceAtLeast(0)
    }

    /** Files a finished session so its focus time counts towards every figure. */
    private fun recordFinishedSession(completed: Boolean) {
        val persisted = _persistedSession.value ?: return
        val now = clock()
        val remaining = persisted.remainingSecondsAt(now)
        val focusedSeconds = (persisted.totalDurationSeconds - remaining).coerceAtLeast(0)
        if (focusedSeconds <= 0) return

        val record = FocusSessionRecord(
            id = persisted.id,
            startedAtEpochMillis = persisted.endsAtEpochMillis -
                persisted.totalDurationSeconds * 1000L,
            endedAtEpochMillis = now,
            focusedSeconds = focusedSeconds,
            completed = completed
        )

        _sessionLog.update { (listOf(record) + it).take(MAX_SESSION_RECORDS) }
        val snapshot = _sessionLog.value
        scope.launch { preferences.saveSessionLog(snapshot) }
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
        const val DEFAULT_PROFILE_NAME = "Guardian"
        const val MAX_STORED_ATTEMPTS = 500
        const val MAX_SESSION_RECORDS = 500
        const val MAX_USAGE_DAYS = 60

        /** Seconds between dashboard recomputes while a session runs. */
        const val STATS_REFRESH_TICKS = 5

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
