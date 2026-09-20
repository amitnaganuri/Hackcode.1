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
    val contentType: String? = null
)
