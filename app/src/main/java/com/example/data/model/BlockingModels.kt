package com.example.data.model

import com.squareup.moshi.JsonClass

/**
 * A single recorded interception: the user opened a blocked app and FocusGuard stepped in.
 *
 * Persisted as JSON alongside the rest of the Phase 1 state. Room is still not warranted
 * here — Phase 3 introduces querying and aggregation, and that is the point at which a
 * real table earns its keep.
 */
@JsonClass(generateAdapter = true)
data class DistractionAttempt(
    val id: String,
    val packageName: String,
    val appName: String,
    val timestampEpochMillis: Long,
    val ruleId: String,
    /** The rule's filter label at the time of the block, e.g. "Entire App Blocked". */
    val ruleLabel: String,
    /** Id of the focus session that was running, or null if blocked outside a session. */
    val sessionId: String?,
    val blockMode: BlockMode
)

/**
 * Structured outcome of evaluating one foreground package against the current rules.
 *
 * Carries the [reason] even when nothing is blocked so the decision path is visible in
 * Logcat. Silent "nothing happened" is the hardest kind of blocking bug to diagnose.
 */
data class BlockDecision(
    val shouldBlock: Boolean,
    val packageName: String,
    val rule: BlockedAppRule?,
    val sessionId: String?,
    val reason: Reason
) {
    enum class Reason {
        BLOCKED,
        IGNORED_PACKAGE,
        NO_MATCHING_RULE,
        RULE_DISABLED,
        NO_ACTIVE_SESSION,
        OUTSIDE_SCHEDULE,
        /** A daily-allowance rule matched but today's budget is not spent yet. */
        WITHIN_ALLOWANCE,
        /** Short-form content is on screen but its own daily budget is not spent. */
        WITHIN_CONTENT_ALLOWANCE,
        /** Content-level rule matched but the short-form feed is not on screen. */
        CONTENT_NOT_DETECTED,
        /** The global Shorts & Reels shield is switched off. */
        SHIELD_DISABLED,
        /** Rule matched but its mode is not enforced. */
        MODE_NOT_SUPPORTED_YET
    }

    companion object {
        fun allow(packageName: String, reason: Reason, rule: BlockedAppRule? = null) =
            BlockDecision(false, packageName, rule, null, reason)

        fun block(packageName: String, rule: BlockedAppRule, sessionId: String?) =
            BlockDecision(true, packageName, rule, sessionId, Reason.BLOCKED)
    }
}

/**
 * Result contract for the per-platform content detectors (InstagramDetector,
 * YouTubeDetector, TikTokDetector) introduced in Phase 7.
 *
 * Declared now so the blocking architecture has a stable shape to grow into.
 * Phase 2 performs whole-app blocking only and never produces one of these.
 */
data class ContentDetectionResult(
    val detected: Boolean,
    val contentType: String? = null,
    /** Id of the [ContentTarget] that matched, used to key its usage budget. */
    val contentId: String? = null
)

/**
 * Foreground time accumulated per package for a single calendar day.
 *
 * Daily-allowance rules need to know how long an app has actually been used today.
 * Keyed by date so the counters reset naturally at midnight without a scheduled job:
 * a snapshot whose [dateKey] is not today is simply discarded on next read.
 */
@JsonClass(generateAdapter = true)
data class DailyUsageSnapshot(
    /** Local calendar day, formatted yyyy-MM-dd. */
    val dateKey: String,
    val millisByPackage: Map<String, Long> = emptyMap()
) {
    fun millisFor(packageName: String): Long = millisByPackage[packageName] ?: 0L

    fun minutesFor(packageName: String): Int = (millisFor(packageName) / 60_000L).toInt()

    fun plus(packageName: String, millis: Long): DailyUsageSnapshot =
        copy(millisByPackage = millisByPackage + (packageName to (millisFor(packageName) + millis)))
}
