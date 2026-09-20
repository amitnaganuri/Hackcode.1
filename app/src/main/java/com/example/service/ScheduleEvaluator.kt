package com.example.service

import java.util.Calendar
import java.util.Locale

/**
 * Decides whether a rule's schedule covers a given instant.
 *
 * Schedules are stored on [com.example.data.model.BlockedAppRule] as display text
 * ("9:00 AM – 1:00 PM • Mon–Fri", "All Day • Daily Permanent Shield"). Rather than
 * migrate the persisted model and the UI that renders that text, this parses the text
 * into a structured window. Parsing is deliberately tolerant: anything it cannot
 * interpret is treated as "always active", so an unparseable schedule fails towards
 * blocking rather than silently disabling a rule the user believes is protecting them.
 */
class ScheduleEvaluator(private val clock: () -> Long = System::currentTimeMillis) {

    data class Window(
        /** Minutes from midnight; null means the whole day. */
        val startMinuteOfDay: Int?,
        val endMinuteOfDay: Int?,
        /** [Calendar.DAY_OF_WEEK] values this schedule applies to. */
        val days: Set<Int>
    ) {
        val isAllDay: Boolean get() = startMinuteOfDay == null || endMinuteOfDay == null
    }

    fun isActiveNow(scheduleText: String): Boolean = isActiveAt(scheduleText, clock())

    fun isActiveAt(scheduleText: String, atMillis: Long): Boolean {
        val window = parse(scheduleText)
        val calendar = Calendar.getInstance().apply { timeInMillis = atMillis }

        if (calendar.get(Calendar.DAY_OF_WEEK) !in window.days) return false
        if (window.isAllDay) return true

        val minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val start = window.startMinuteOfDay!!
        val end = window.endMinuteOfDay!!

        // An end at or before the start means the window runs past midnight.
        return if (end > start) {
            minuteOfDay in start until end
        } else {
            minuteOfDay >= start || minuteOfDay < end
        }
    }

    fun parse(scheduleText: String): Window {
        val text = scheduleText.lowercase(Locale.US)
        val days = parseDays(text)

        if (text.contains("all day") || text.contains("permanent")) {
            return Window(null, null, days)
        }

        val times = TIME_PATTERN.findAll(scheduleText).take(2).toList()
        if (times.size < 2) return Window(null, null, days)

        return Window(times[0].toMinuteOfDay(), times[1].toMinuteOfDay(), days)
    }

    private fun parseDays(lowercaseText: String): Set<Int> = when {
        lowercaseText.contains("weekend") -> WEEKEND
        lowercaseText.contains("weekday") -> WEEKDAYS
        // "Mon–Fri" and its variants, whichever dash character was used.
        MON_FRI_PATTERN.containsMatchIn(lowercaseText) -> WEEKDAYS
        else -> ALL_DAYS
    }

    private fun MatchResult.toMinuteOfDay(): Int {
        val hour12 = groupValues[1].toInt() % 12
        val minute = groupValues[2].toInt()
        val isPm = groupValues[3].equals("PM", ignoreCase = true)
        return (hour12 + if (isPm) 12 else 0) * 60 + minute
    }

    private companion object {
        val TIME_PATTERN = Regex("""(\d{1,2}):(\d{2})\s*(AM|PM)""", RegexOption.IGNORE_CASE)
        val MON_FRI_PATTERN = Regex("""mon\s*[-–—]\s*fri""")

        val ALL_DAYS = setOf(
            Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
        )
        val WEEKDAYS = setOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY
        )
        val WEEKEND = setOf(Calendar.SATURDAY, Calendar.SUNDAY)
    }
}
