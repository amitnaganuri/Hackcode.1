package com.example.data.analytics

import com.example.data.model.DailyUsageSnapshot
import com.example.data.model.AppInterventionStat
import com.example.data.model.BlockedAppRule
import com.example.data.model.DailyStats
import com.example.data.model.DistractionAttempt
import com.example.data.model.FocusSessionRecord
import com.example.data.model.InsightsStats
import com.example.data.model.Timeframe
import com.example.data.model.WeeklyBalanceDay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Turns the raw logs into the numbers the dashboard shows.
 *
 * Deliberately free of Android and of the repository so every figure can be pinned down
 * in tests — these values are the whole product claim, and a wrong one is worse than a
 * missing one. Nothing here invents data: when there is no history a figure reads zero
 * rather than showing a placeholder.
 */
object FocusAnalytics {

    /**
     * Minutes credited back per interception.
     *
     * An interception cannot measure the scroll that never happened, so this is an
     * explicit, conservative estimate rather than a measurement, and it is the single
     * place that assumption lives.
     */
    const val MINUTES_RECOVERED_PER_INTERCEPTION = 3

    // ------------------------------------------------------------------ Home

    fun dailyStats(
        base: DailyStats,
        attempts: List<DistractionAttempt>,
        sessions: List<FocusSessionRecord>,
        usageHistory: List<DailyUsageSnapshot>,
        rules: List<BlockedAppRule>,
        runningSessionSeconds: Int,
        nowMillis: Long
    ): DailyStats {
        val todayStart = startOfDay(nowMillis)
        val attemptsToday = attempts.count { it.timestampEpochMillis >= todayStart }

        val focusedTodayMinutes =
            (focusedSecondsBetween(sessions, todayStart, nowMillis) + runningSessionSeconds) / 60

        val todayUsage = usageHistory.firstOrNull { it.dateKey == dateKey(nowMillis) }
        val ruledPackages = rules.filter { it.isEnabled }.map { it.packageName }.toSet()
        val distractedMillis = todayUsage
            ?.millisByPackage
            ?.entries
            // Whole-app entries only: a "package::content" key would count the same
            // minutes a second time.
            ?.filter { entry -> !entry.key.contains("::") && entry.key in ruledPackages }
            ?.sumOf { entry -> entry.value }
            ?: 0L
        val distractedMinutes = (distractedMillis / 60_000L).toInt()

        return base.copy(
            focusScorePercent = focusScore(focusedTodayMinutes, distractedMinutes),
            focusedTodayMinutes = focusedTodayMinutes,
            recoveredMinutesToday = attemptsToday * MINUTES_RECOVERED_PER_INTERCEPTION,
            blockedAttemptsToday = attemptsToday,
            totalBlockedAllTime = attempts.size,
            activeScreenTimeText = formatMinutes(totalMinutes(todayUsage)),
            streakDays = streakDays(attempts, sessions, nowMillis)
        )
    }

    /**
     * Focus time as a share of the time that mattered today.
     *
     * "Time that mattered" is focus time plus time inside apps the user chose to put a
     * rule on, so idle time and unrelated apps neither help nor hurt the score.
     */
    fun focusScore(focusedMinutes: Int, distractedMinutes: Int): Int {
        val total = focusedMinutes + distractedMinutes
        if (total <= 0) return 0
        return ((focusedMinutes.toFloat() / total) * 100).toInt().coerceIn(0, 100)
    }

    // -------------------------------------------------------------- Insights

    fun insights(
        timeframe: Timeframe,
        attempts: List<DistractionAttempt>,
        sessions: List<FocusSessionRecord>,
        usageHistory: List<DailyUsageSnapshot>,
        runningSessionSeconds: Int,
        nowMillis: Long
    ): InsightsStats {
        val windowStart = startOfDay(nowMillis) - (timeframe.days - 1) * DAY_MILLIS
        val windowAttempts = attempts.filter { it.timestampEpochMillis >= windowStart }
        val focusSeconds =
            focusedSecondsBetween(sessions, windowStart, nowMillis) + runningSessionSeconds

        val windowKeys = (0 until timeframe.days).map { dateKey(nowMillis - it * DAY_MILLIS) }
        val windowUsage = usageHistory.filter { it.dateKey in windowKeys }
        val windowMinutes = windowUsage.sumOf { totalMinutes(it) }

        // Compare against the same number of days immediately before the window.
        val priorKeys = (timeframe.days until timeframe.days * 2)
            .map { dateKey(nowMillis - it * DAY_MILLIS) }
        val priorUsage = usageHistory.filter { it.dateKey in priorKeys }
        val priorMinutes = priorUsage.sumOf { totalMinutes(it) }

        val changePercent = when {
            priorUsage.isEmpty() || priorMinutes == 0 -> 0
            else -> (((windowMinutes - priorMinutes).toFloat() / priorMinutes) * 100).toInt()
        }

        val comparison = when {
            priorUsage.isEmpty() || priorMinutes == 0 ->
                "Not enough history yet to compare."
            windowMinutes < priorMinutes ->
                "You reclaimed " + formatMinutes(priorMinutes - windowMinutes) +
                    " compared with the previous " + timeframe.days + " days."
            windowMinutes > priorMinutes ->
                "That is " + formatMinutes(windowMinutes - priorMinutes) +
                    " more than the previous " + timeframe.days + " days."
            else -> "Exactly the same as the previous " + timeframe.days + " days."
        }

        // Focus in the equivalent window immediately before this one.
        val priorWindowStart = windowStart - timeframe.days * DAY_MILLIS
        val priorFocusSeconds = focusedSecondsBetween(sessions, priorWindowStart, windowStart)
        val focusChangePercent = if (priorFocusSeconds <= 0) {
            0
        } else {
            (((focusSeconds - priorFocusSeconds).toFloat() / priorFocusSeconds) * 100).toInt()
        }

        return InsightsStats(
            timeframe = timeframe,
            screenTimeText = formatMinutes(windowMinutes),
            screenTimeChangePercent = changePercent,
            screenTimeComparisonText = comparison,
            deepFocusText = formatMinutes(focusSeconds / 60),
            deepFocusChangePercent = focusChangePercent,
            recoveredMinutes = windowAttempts.size * MINUTES_RECOVERED_PER_INTERCEPTION,
            recoveredText = formatMinutes(
                windowAttempts.size * MINUTES_RECOVERED_PER_INTERCEPTION
            ),
            blockedCount = windowAttempts.size,
            streakDays = streakDays(attempts, sessions, nowMillis),
            protectedPercent = protectedPercent(sessions, timeframe, nowMillis),
            appInterventions = appInterventions(windowAttempts),
            weeklyBalance = weeklyBalance(attempts, sessions, nowMillis)
        )
    }

    fun appInterventions(attempts: List<DistractionAttempt>): List<AppInterventionStat> {
        val grouped = attempts.groupBy { it.appName }.entries.sortedByDescending { it.value.size }
        val max = grouped.firstOrNull()?.value?.size ?: 0
        return grouped.map { (appName, forApp) ->
            AppInterventionStat(
                appName = appName,
                subtext = forApp.size.toString() + " attempts intercepted",
                attemptsCount = forApp.size,
                timeFormatted = formatMinutes(
                    forApp.size * MINUTES_RECOVERED_PER_INTERCEPTION
                ),
                reductionPercentText = "",
                progressFraction = if (max > 0) forApp.size.toFloat() / max else 0f
            )
        }
    }

    /** Focus against impulse for each of the last seven days, oldest first. */
    fun weeklyBalance(
        attempts: List<DistractionAttempt>,
        sessions: List<FocusSessionRecord>,
        nowMillis: Long
    ): List<WeeklyBalanceDay> {
        val days = (6 downTo 0).map { offset ->
            val dayMillis = nowMillis - offset * DAY_MILLIS
            val start = startOfDay(dayMillis)
            val end = start + DAY_MILLIS
            Triple(
                dayMillis,
                focusedSecondsBetween(sessions, start, end) / 60,
                attempts.count { it.timestampEpochMillis in start until end }
            )
        }

        val maxFocus = days.maxOfOrNull { it.second } ?: 0
        val maxAttempts = days.maxOfOrNull { it.third } ?: 0
        val todayKey = dateKey(nowMillis)

        return days.map { (dayMillis, focusMinutes, attemptCount) ->
            WeeklyBalanceDay(
                dayLabel = dayLabel(dayMillis),
                focusFraction = if (maxFocus > 0) focusMinutes.toFloat() / maxFocus else 0f,
                impulseFraction = if (maxAttempts > 0) attemptCount.toFloat() / maxAttempts else 0f,
                isHighlighted = dateKey(dayMillis) == todayKey
            )
        }
    }

    /** Consecutive days up to today on which the user focused or was intercepted. */
    fun streakDays(
        attempts: List<DistractionAttempt>,
        sessions: List<FocusSessionRecord>,
        nowMillis: Long
    ): Int {
        var streak = 0
        var offset = 0
        while (offset < MAX_STREAK_LOOKBACK) {
            val start = startOfDay(nowMillis - offset * DAY_MILLIS)
            val end = start + DAY_MILLIS
            val active = sessions.any { it.endedAtEpochMillis in start until end } ||
                attempts.any { it.timestampEpochMillis in start until end }
            if (!active) break
            streak++
            offset++
        }
        return streak
    }

    /** Share of days in the window that carried at least one completed focus session. */
    fun protectedPercent(
        sessions: List<FocusSessionRecord>,
        timeframe: Timeframe,
        nowMillis: Long
    ): Int {
        val days = timeframe.days
        val protectedDays = (0 until days).count { offset ->
            val start = startOfDay(nowMillis - offset * DAY_MILLIS)
            val end = start + DAY_MILLIS
            sessions.any { it.completed && it.endedAtEpochMillis in start until end }
        }
        return ((protectedDays.toFloat() / days) * 100).toInt().coerceIn(0, 100)
    }

    // ----------------------------------------------------------- equivalents

    /**
     * Recovered minutes expressed as everyday activities.
     *
     * The card promises "real-world activities unlocked from saved screen time", so
     * each figure is that saved time divided by a plain, stated duration rather than a
     * number chosen to look impressive.
     */
    fun pagesRead(recoveredMinutes: Int): Int = recoveredMinutes / MINUTES_PER_PAGE

    fun workouts(recoveredMinutes: Int): Int = recoveredMinutes / MINUTES_PER_WORKOUT

    fun studyBlocks(recoveredMinutes: Int): Int = recoveredMinutes / MINUTES_PER_STUDY_BLOCK

    /** Roughly a page of a paperback. */
    private const val MINUTES_PER_PAGE = 2

    private const val MINUTES_PER_WORKOUT = 30

    /** One pomodoro. */
    private const val MINUTES_PER_STUDY_BLOCK = 25

    // --------------------------------------------------------------- helpers

    fun focusedSecondsBetween(
        sessions: List<FocusSessionRecord>,
        startMillis: Long,
        endMillis: Long
    ): Int = sessions
        .filter { it.endedAtEpochMillis in startMillis until endMillis }
        .sumOf { it.focusedSeconds }

    fun totalMinutes(snapshot: DailyUsageSnapshot?): Int {
        if (snapshot == null) return 0
        // Composite keys ("package::content") count the same time again, so only
        // whole-app entries are summed for a screen-time total.
        val wholeApp = snapshot.millisByPackage.filterKeys { !it.contains("::") }
        return (wholeApp.values.sum() / 60_000L).toInt()
    }

    fun formatMinutes(minutes: Int): String = when {
        minutes <= 0 -> "0m"
        minutes < 60 -> minutes.toString() + "m"
        minutes % 60 == 0 -> (minutes / 60).toString() + "h"
        else -> (minutes / 60).toString() + "h " + (minutes % 60).toString() + "m"
    }

    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun dateKey(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))

    private fun dayLabel(millis: Long): String =
        SimpleDateFormat("EEE", Locale.getDefault()).format(Date(millis)).take(1).uppercase()

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000
    private const val MAX_STREAK_LOOKBACK = 365
}
