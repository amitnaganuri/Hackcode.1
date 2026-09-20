package com.example.data.local

import com.squareup.moshi.JsonClass

/**
 * Wall-clock representation of a focus session.
 *
 * The UI model [com.example.data.model.FocusSession] carries a `remainingSeconds`
 * countdown, which cannot survive process death or a frozen background process.
 * What we persist instead is the absolute instant the session ends; remaining time
 * is always derived from the current clock, so the countdown stays correct across
 * app restarts, device sleep and backgrounding.
 */
@JsonClass(generateAdapter = true)
data class PersistedSession(
    val id: String,
    val title: String,
    val targetGoal: String,
    val totalDurationSeconds: Int,
    val endsAtEpochMillis: Long,
    /** Non-null while paused; the instant the user paused. */
    val pausedAtEpochMillis: Long? = null,
    val blockedApps: List<String> = emptyList()
) {
    val isPaused: Boolean get() = pausedAtEpochMillis != null

    fun remainingSecondsAt(nowMillis: Long): Int {
        val reference = pausedAtEpochMillis ?: nowMillis
        val remainingMillis = endsAtEpochMillis - reference
        return (remainingMillis / 1000L).coerceAtLeast(0L).toInt()
    }

    /** Shifts the end instant forward by the time spent paused. */
    fun resumedAt(nowMillis: Long): PersistedSession {
        val pausedAt = pausedAtEpochMillis ?: return this
        return copy(
            endsAtEpochMillis = endsAtEpochMillis + (nowMillis - pausedAt),
            pausedAtEpochMillis = null
        )
    }
}
