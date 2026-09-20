package com.example.data.model

enum class BlockMode {
    CONTENT_LEVEL,
    HARD_BLOCK,
    DAILY_ALLOWANCE
}

data class BlockedAppRule(
    val id: String,
    val appName: String,
    val packageName: String,
    val blockMode: BlockMode,
    val filterLabel: String,
    val scheduleText: String,
    val isEnabled: Boolean = true,
    val attemptsBlockedCount: Int = 0,
    val usedMinutes: Int = 0,
    val totalAllowedMinutes: Int = 0,
    val category: String = "Social"
)

data class FocusSession(
    val id: String = "session_active",
    val title: String = "Deep Work",
    val targetGoal: String = "Finish my DSA practice before 7 PM.",
    val remainingSeconds: Int = 41 * 60 + 27,
    val totalDurationSeconds: Int = 60 * 60,
    val isRunning: Boolean = true,
    val isPaused: Boolean = false,
    val blockedApps: List<String> = listOf("Instagram (Reels)", "YouTube (Shorts)", "TikTok (Full)"),
    val endTimeText: String = "10:45 AM"
) {
    val formattedRemainingTime: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

    val progressFraction: Float
        get() = if (totalDurationSeconds > 0) {
            1f - (remainingSeconds.toFloat() / totalDurationSeconds)
        } else 0f
}

data class InterventionConfig(
    val targetApp: String = "Instagram Reels",
    val customQuote: String = "“You promised yourself you'd finish your DSA practice before 7 PM.”",
    val subMessage: String = "Your future self will thank you for closing this.",
    val anchorLabel: String = "Personal Anchor",
    val anchorImageUrl: String = "https://lh3.googleusercontent.com/aida-public/AB6AXuDESc1fd_Wm_4448JK6b2fSMx4mxP0vgXDgYdHpdFrTCQUARqwGXm7XDpSzlPLI5x4R1BqiqF54C6i38AQp-VLBitCiovRKv0M2j5izU9h-_UBsGq9UyMl1cDuiO7fr3UtYTPV13cZ33QQuQlGNBxmeQYkd7gM6jePirbpUOSyWY0rlSxI3KtOHLtgwUEgCyFa-6wHgeH9y0m1JzRGihQ1MVefPmepTYth3TqOL-tjakCtuwHjE89ordA",
    val countdownSeconds: Int = 7,
    val activeTargetName: String = "Master Binary Trees",
    val activeTargetBadge: String = "60m Deep Work"
)

data class AppInterventionStat(
    val appName: String,
    val subtext: String,
    val attemptsCount: Int,
    val timeFormatted: String = "",
    val reductionPercentText: String = "",
    val progressFraction: Float = 0.35f
)

data class WeeklyBalanceDay(
    val dayLabel: String,
    val focusFraction: Float,
    val impulseFraction: Float,
    val isHighlighted: Boolean = false
)

data class DailyStats(
    val focusScorePercent: Int = 82,
    val focusedTodayMinutes: Int = 158, // 2h 38m
    val recoveredMinutesToday: Int = 47,
    val blockedAttemptsToday: Int = 12,
    val totalBlockedAllTime: Int = 47,
    val activeScreenTimeText: String = "4h 12m",
    val screenTimeReductionPercent: Int = 28,
    val screenTimeComparisonText: String = "You reclaimed 1h 40m compared to last Tuesday",
    val deepFocusWeeklyText: String = "3h 26m",
    val recoveredWeeklyText: String = "54m",
    val blockedWeeklyCount: Int = 47,
    val streakDays: Int = 7,
    val streakProtectedPercent: Int = 85,
    val todayGoalText: String = "Finish my DSA practice before 7 PM."
)
