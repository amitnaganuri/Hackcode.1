package com.example

import com.example.data.analytics.FocusAnalytics
import com.example.data.model.BlockMode
import com.example.data.model.BlockedAppRule
import com.example.data.model.DailyStats
import com.example.data.model.DailyUsageSnapshot
import com.example.data.model.DistractionAttempt
import com.example.data.model.FocusSessionRecord
import com.example.data.model.Timeframe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every figure on the dashboard comes from here, and a wrong number is worse than a
 * missing one, so the arithmetic is pinned down rather than eyeballed on a screenshot.
 */
class FocusAnalyticsTest {

    private val dayMillis = 24L * 60 * 60 * 1000

    /** Midday, so day-boundary arithmetic is never ambiguous in a test. */
    private val now = FocusAnalytics.startOfDay(System.currentTimeMillis()) + 12 * 60 * 60 * 1000

    private fun attemptAt(millis: Long, app: String = "Instagram") = DistractionAttempt(
        id = "a$millis$app",
        packageName = "com.instagram.android",
        appName = app,
        timestampEpochMillis = millis,
        ruleId = "rule_1",
        ruleLabel = "Reels",
        sessionId = null,
        blockMode = BlockMode.CONTENT_LEVEL
    )

    private fun sessionEndingAt(millis: Long, focusedSeconds: Int, completed: Boolean = true) =
        FocusSessionRecord(
            id = "s$millis",
            startedAtEpochMillis = millis - focusedSeconds * 1000L,
            endedAtEpochMillis = millis,
            focusedSeconds = focusedSeconds,
            completed = completed
        )

    // ------------------------------------------------------------ focus score

    @Test
    fun `focus score is focus time as a share of time that mattered`() {
        assertEquals(75, FocusAnalytics.focusScore(focusedMinutes = 30, distractedMinutes = 10))
    }

    @Test
    fun `focus score is zero with no activity rather than a placeholder`() {
        assertEquals(0, FocusAnalytics.focusScore(focusedMinutes = 0, distractedMinutes = 0))
    }

    // ----------------------------------------------------------- daily stats

    @Test
    fun `daily stats count only today and ignore a rule-free app`() {
        val attempts = listOf(
            attemptAt(now - 60_000),
            attemptAt(now - 120_000),
            attemptAt(now - 3 * dayMillis) // older, must not count towards today
        )
        val sessions = listOf(sessionEndingAt(now - 60_000, focusedSeconds = 1_800))
        val usage = listOf(
            DailyUsageSnapshot(
                dateKey = FocusAnalytics.dateKey(now),
                millisByPackage = mapOf(
                    "com.instagram.android" to 10 * 60_000L,
                    "com.unblocked.app" to 40 * 60_000L
                )
            )
        )
        val rules = listOf(
            BlockedAppRule(
                id = "rule_1",
                appName = "Instagram",
                packageName = "com.instagram.android",
                blockMode = BlockMode.CONTENT_LEVEL,
                filterLabel = "Reels",
                scheduleText = "All Day"
            )
        )

        val stats = FocusAnalytics.dailyStats(
            base = DailyStats(),
            attempts = attempts,
            sessions = sessions,
            usageHistory = usage,
            rules = rules,
            runningSessionSeconds = 0,
            nowMillis = now
        )

        assertEquals(2, stats.blockedAttemptsToday)
        assertEquals(3, stats.totalBlockedAllTime)
        assertEquals(30, stats.focusedTodayMinutes)
        assertEquals(2 * FocusAnalytics.MINUTES_RECOVERED_PER_INTERCEPTION, stats.recoveredMinutesToday)
        // 30 focused against 10 distracted; the unruled app is excluded.
        assertEquals(75, stats.focusScorePercent)
        // Screen time covers every app, not only the blocked ones.
        assertEquals("50m", stats.activeScreenTimeText)
    }

    @Test
    fun `a running session counts towards today's focus time`() {
        val stats = FocusAnalytics.dailyStats(
            base = DailyStats(),
            attempts = emptyList(),
            sessions = emptyList(),
            usageHistory = emptyList(),
            rules = emptyList(),
            runningSessionSeconds = 600,
            nowMillis = now
        )

        assertEquals(10, stats.focusedTodayMinutes)
    }

    @Test
    fun `per-content usage keys are not double counted as screen time`() {
        val snapshot = DailyUsageSnapshot(
            dateKey = FocusAnalytics.dateKey(now),
            millisByPackage = mapOf(
                "com.instagram.android" to 20 * 60_000L,
                "com.instagram.android::instagram_reels" to 15 * 60_000L
            )
        )

        assertEquals(20, FocusAnalytics.totalMinutes(snapshot))
    }

    // -------------------------------------------------------------- insights

    @Test
    fun `timeframe narrows which attempts are counted`() {
        val attempts = listOf(
            attemptAt(now),
            attemptAt(now - 2 * dayMillis),
            attemptAt(now - 20 * dayMillis)
        )

        fun blockedFor(timeframe: Timeframe) = FocusAnalytics.insights(
            timeframe = timeframe,
            attempts = attempts,
            sessions = emptyList(),
            usageHistory = emptyList(),
            runningSessionSeconds = 0,
            nowMillis = now
        ).blockedCount

        assertEquals(1, blockedFor(Timeframe.TODAY))
        assertEquals(2, blockedFor(Timeframe.THIS_WEEK))
        assertEquals(3, blockedFor(Timeframe.THIS_MONTH))
    }

    @Test
    fun `screen time comparison says so when there is no prior history`() {
        val insights = FocusAnalytics.insights(
            timeframe = Timeframe.THIS_WEEK,
            attempts = emptyList(),
            sessions = emptyList(),
            usageHistory = listOf(
                DailyUsageSnapshot(FocusAnalytics.dateKey(now), mapOf("a" to 60_000L))
            ),
            runningSessionSeconds = 0,
            nowMillis = now
        )

        assertEquals(0, insights.screenTimeChangePercent)
        assertTrue(insights.screenTimeComparisonText.contains("Not enough history"))
    }

    // ---------------------------------------------------------------- streak

    @Test
    fun `streak counts consecutive active days and stops at the first gap`() {
        val attempts = listOf(
            attemptAt(now),
            attemptAt(now - dayMillis),
            // no activity two days ago
            attemptAt(now - 3 * dayMillis)
        )

        assertEquals(2, FocusAnalytics.streakDays(attempts, emptyList(), now))
    }

    @Test
    fun `streak is zero when nothing has happened`() {
        assertEquals(0, FocusAnalytics.streakDays(emptyList(), emptyList(), now))
    }

    // -------------------------------------------------------- weekly balance

    @Test
    fun `weekly balance returns seven days with today highlighted last`() {
        val balance = FocusAnalytics.weeklyBalance(
            attempts = listOf(attemptAt(now)),
            sessions = listOf(sessionEndingAt(now, focusedSeconds = 600)),
            nowMillis = now
        )

        assertEquals(7, balance.size)
        assertTrue(balance.last().isHighlighted)
        assertEquals(1, balance.count { it.isHighlighted })
        // Today is the only active day, so it defines the full-height bar.
        assertEquals(1f, balance.last().focusFraction, 0.001f)
    }

    // -------------------------------------------------------------- formatting

    @Test
    fun `minutes format as hours and minutes`() {
        assertEquals("0m", FocusAnalytics.formatMinutes(0))
        assertEquals("45m", FocusAnalytics.formatMinutes(45))
        assertEquals("2h", FocusAnalytics.formatMinutes(120))
        assertEquals("2h 38m", FocusAnalytics.formatMinutes(158))
    }
}
