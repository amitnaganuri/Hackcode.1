package com.example.data.repository

import com.example.data.model.AppInterventionStat
import com.example.data.model.BlockMode
import com.example.data.model.BlockedAppRule
import com.example.data.model.DailyStats
import com.example.data.model.FocusSession
import com.example.data.model.InterventionConfig
import com.example.data.model.WeeklyBalanceDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FocusGuardRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _rules = MutableStateFlow(
        listOf(
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
    )
    val rules: StateFlow<List<BlockedAppRule>> = _rules.asStateFlow()

    private val _shortsAndReelsMasterShield = MutableStateFlow(true)
    val shortsAndReelsMasterShield: StateFlow<Boolean> = _shortsAndReelsMasterShield.asStateFlow()

    private val _activeSession = MutableStateFlow<FocusSession?>(FocusSession())
    val activeSession: StateFlow<FocusSession?> = _activeSession.asStateFlow()

    private val _interventionConfig = MutableStateFlow(InterventionConfig())
    val interventionConfig: StateFlow<InterventionConfig> = _interventionConfig.asStateFlow()

    private val _dailyStats = MutableStateFlow(DailyStats())
    val dailyStats: StateFlow<DailyStats> = _dailyStats.asStateFlow()

    private val _weeklyBalance = MutableStateFlow(
        listOf(
            WeeklyBalanceDay("M", 0.65f, 0.45f),
            WeeklyBalanceDay("T", 0.80f, 0.30f),
            WeeklyBalanceDay("W", 0.95f, 0.20f, isHighlighted = true),
            WeeklyBalanceDay("T", 0.70f, 0.40f),
            WeeklyBalanceDay("F", 0.55f, 0.60f),
            WeeklyBalanceDay("S", 0.85f, 0.15f),
            WeeklyBalanceDay("S", 0.40f, 0.10f)
        )
    )
    val weeklyBalance: StateFlow<List<WeeklyBalanceDay>> = _weeklyBalance.asStateFlow()

    private val _appInterventions = MutableStateFlow(
        listOf(
            AppInterventionStat("Instagram", "18 attempts intercepted", 18, "1h 12m", "↓ 34% drop", 0.38f),
            AppInterventionStat("YouTube", "14 attempts intercepted", 14, "48m", "↓ 21% drop", 0.26f),
            AppInterventionStat("TikTok", "7 attempts intercepted", 7, "32m", "↓ 41% drop", 0.16f)
        )
    )
    val appInterventions: StateFlow<List<AppInterventionStat>> = _appInterventions.asStateFlow()

    private var timerJob: Job? = null

    init {
        startTimerLoop()
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                _activeSession.update { current ->
                    if (current != null && current.isRunning && !current.isPaused) {
                        if (current.remainingSeconds > 0) {
                            current.copy(remainingSeconds = current.remainingSeconds - 1)
                        } else {
                            current.copy(isRunning = false)
                        }
                    } else current
                }
            }
        }
    }

    fun toggleShortsAndReelsMasterShield() {
        _shortsAndReelsMasterShield.update { !it }
    }

    fun toggleRule(ruleId: String) {
        _rules.update { list ->
            list.map { rule ->
                if (rule.id == ruleId) rule.copy(isEnabled = !rule.isEnabled) else rule
            }
        }
    }

    fun addRule(rule: BlockedAppRule) {
        _rules.update { it + rule }
    }

    fun updateGoal(newGoal: String) {
        _dailyStats.update { it.copy(todayGoalText = newGoal) }
        _activeSession.update { it?.copy(targetGoal = newGoal) }
    }

    fun startDeepFocus(durationMinutes: Int) {
        val totalSecs = durationMinutes * 60
        _activeSession.value = FocusSession(
            id = "session_${System.currentTimeMillis()}",
            title = "Deep Work",
            targetGoal = _dailyStats.value.todayGoalText,
            remainingSeconds = totalSecs,
            totalDurationSeconds = totalSecs,
            isRunning = true,
            isPaused = false,
            blockedApps = listOf("Instagram (Reels)", "YouTube (Shorts)", "TikTok (Full)"),
            endTimeText = calculateEndTimeText(durationMinutes)
        )
    }

    fun endActiveSession() {
        _activeSession.value = null
    }

    fun togglePauseSession() {
        _activeSession.update { it?.copy(isPaused = !it.isPaused) }
    }

    fun updateInterventionConfig(updated: InterventionConfig) {
        _interventionConfig.value = updated
    }

    private fun calculateEndTimeText(addedMinutes: Int): String {
        val now = java.util.Calendar.getInstance()
        now.add(java.util.Calendar.MINUTE, addedMinutes)
        val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return format.format(now.time)
    }
}
