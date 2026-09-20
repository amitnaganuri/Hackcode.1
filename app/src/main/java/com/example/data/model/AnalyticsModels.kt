package com.example.data.model

import com.squareup.moshi.JsonClass

/**
 * A focus session that has finished.
 *
 * Without this the app could tell you a session was running but never how much focus
 * time you had actually accumulated, which is what every figure on the dashboard is
 * built from.
 */
@JsonClass(generateAdapter = true)
data class FocusSessionRecord(
    val id: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    /** Time actually spent focused, excluding any paused stretches. */
    val focusedSeconds: Int,
    /** True when the timer reached zero rather than being ended early. */
    val completed: Boolean
)

/** How far back a figure on the Insights screen is measured. */
enum class Timeframe(val label: String, val days: Int) {
    TODAY("Today", 1),
    THIS_WEEK("This Week", 7),
    THIS_MONTH("This Month", 30);

    companion object {
        fun fromLabel(label: String): Timeframe =
            entries.firstOrNull { it.label == label } ?: THIS_WEEK
    }
}

/**
 * Everything the Insights screen shows, already resolved for the selected timeframe.
 *
 * Kept separate from [DailyStats] because Home always talks about today while Insights
 * follows the Today / This Week / This Month selector.
 */
data class InsightsStats(
    val timeframe: Timeframe = Timeframe.THIS_WEEK,
    val screenTimeText: String = "0m",
    val screenTimeChangePercent: Int = 0,
    val screenTimeComparisonText: String = "Not enough history yet to compare.",
    val deepFocusText: String = "0m",
    val deepFocusChangePercent: Int = 0,
    val recoveredMinutes: Int = 0,
    val recoveredText: String = "0m",
    val blockedCount: Int = 0,
    val streakDays: Int = 0,
    val protectedPercent: Int = 0,
    val appInterventions: List<AppInterventionStat> = emptyList(),
    val weeklyBalance: List<WeeklyBalanceDay> = emptyList()
)
