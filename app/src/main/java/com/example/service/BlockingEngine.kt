package com.example.service

import com.example.data.model.BlockDecision
import com.example.data.model.ContentCatalog
import com.example.data.model.ContentDetectionResult
import com.example.data.model.ContentTarget
import com.example.data.model.BlockMode
import com.example.data.model.BlockedAppRule
import com.example.data.repository.FocusGuardRepository

/**
 * Decides whether the app currently in the foreground should be interrupted.
 *
 * Deliberately free of Android framework types so the whole policy is testable and so
 * [FocusAccessibilityService] stays a thin adapter: the service observes the system and
 * performs effects, the engine decides.
 */
interface BlockingEngine {
    fun evaluate(
        packageName: String,
        nowMillis: Long = System.currentTimeMillis(),
        /**
         * Scans the current screen for the given targets. Supplied by the service
         * because only it can reach the accessibility tree; the engine decides *which*
         * targets to look for.
         */
        detectContent: (List<ContentTarget>) -> ContentDetectionResult? = { null }
    ): BlockDecision
}

/**
 * Phase 2 policy — whole-app blocking only.
 *
 * A package is blocked when all of the following hold:
 *  1. it is not FocusGuard itself, the launcher, or system UI;
 *  2. an enabled rule targets its package name;
 *  3. the rule's schedule covers this instant;
 *  4. the rule either blocks the whole app, is a daily allowance whose budget for
 *     today is already spent, or is a content-level rule whose short-form feed the
 *     detector has just reported on screen.
 *
 * A focus session is deliberately NOT required. A whole-app block is a standing shield
 * the user switched on; gating it behind a session made the feature appear broken.
 * The running session id, when there is one, is still recorded on the attempt.
 */
class DefaultBlockingEngine(
    private val repository: FocusGuardRepository,
    private val ownPackageName: String,
    private val scheduleEvaluator: ScheduleEvaluator = ScheduleEvaluator(),
    private val launcherPackages: Set<String> = DEFAULT_IGNORED_PACKAGES
) : BlockingEngine {

    override fun evaluate(
        packageName: String,
        nowMillis: Long,
        detectContent: (List<ContentTarget>) -> ContentDetectionResult?
    ): BlockDecision {
        if (packageName == ownPackageName || packageName in launcherPackages) {
            return BlockDecision.allow(packageName, BlockDecision.Reason.IGNORED_PACKAGE)
        }

        // A package can legitimately carry several rules (for example a whole-app block
        // plus a daily allowance). Narrowing step by step, rather than taking the first
        // match, means a blocking rule is never masked by an unrelated one.
        val matching = repository.rules.value
            .filter { it.packageName.equals(packageName, ignoreCase = true) }
        if (matching.isEmpty()) {
            return BlockDecision.allow(packageName, BlockDecision.Reason.NO_MATCHING_RULE)
        }

        val enabled = matching.filter { it.isEnabled }
        if (enabled.isEmpty()) {
            return BlockDecision.allow(packageName, BlockDecision.Reason.RULE_DISABLED, matching.first())
        }

        // Recorded on the attempt when a session happens to be running, so Insights can
        // still tell session-time interceptions from everyday ones.
        val session = repository.activeSession.value
        val sessionId = session?.takeIf { it.isRunning && !it.isPaused }?.id

        val inSchedule = enabled.filter {
            scheduleEvaluator.isActiveAt(it.scheduleText, nowMillis)
        }
        if (inSchedule.isEmpty()) {
            return BlockDecision.allow(
                packageName,
                BlockDecision.Reason.OUTSIDE_SCHEDULE,
                enabled.first()
            )
        }

        // A whole-app block is a standing shield: if the user enabled it and the
        // schedule covers now, it is enforced whether or not a focus session is running.
        // Requiring a session made "Block Entire Application" look broken.
        inSchedule.firstOrNull { it.blockMode == BlockMode.HARD_BLOCK }?.let { rule ->
            return BlockDecision.block(packageName, rule, sessionId)
        }

        // A daily allowance blocks only once today's budget is spent. Usage is measured
        // by the accessibility service, so this reflects real foreground time.
        inSchedule.firstOrNull { rule ->
            rule.blockMode == BlockMode.DAILY_ALLOWANCE &&
                rule.totalAllowedMinutes > 0 &&
                repository.usedMinutesToday(rule.packageName) >= rule.totalAllowedMinutes
        }?.let { rule ->
            return BlockDecision.block(packageName, rule, sessionId)
        }

        // Content-level rules keep the app usable and block only the short-form feed,
        // so they need the detector's verdict for the window currently on screen.
        val contentRules = inSchedule.filter { it.blockMode == BlockMode.CONTENT_LEVEL }
        if (contentRules.isNotEmpty()) {
            // The global shield is the user's master off-switch for this mode.
            if (!repository.shortsAndReelsMasterShield.value) {
                return BlockDecision.allow(
                    packageName,
                    BlockDecision.Reason.SHIELD_DISABLED,
                    contentRules.first()
                )
            }

            for (rule in contentRules) {
                val targets = targetsForRule(rule)
                if (targets.isEmpty()) continue

                val detection = detectContent(targets) ?: continue
                if (!detection.detected) continue

                // A content budget lets the user watch a set amount before the shield
                // closes, rather than being blocked on sight.
                if (rule.contentAllowanceMinutes > 0 && detection.contentId != null) {
                    val usageKey = ContentCatalog.usageKey(rule.packageName, detection.contentId)
                    val used = repository.usedMinutesToday(usageKey)
                    if (used < rule.contentAllowanceMinutes) {
                        return BlockDecision.allow(
                            packageName,
                            BlockDecision.Reason.WITHIN_CONTENT_ALLOWANCE,
                            rule
                        )
                    }
                }

                return BlockDecision.block(packageName, rule, sessionId)
            }

            return BlockDecision.allow(
                packageName,
                BlockDecision.Reason.CONTENT_NOT_DETECTED,
                contentRules.first()
            )
        }

        // Matched a rule, but nothing warrants blocking right now.
        val reason = if (inSchedule.any { it.blockMode == BlockMode.DAILY_ALLOWANCE }) {
            BlockDecision.Reason.WITHIN_ALLOWANCE
        } else {
            BlockDecision.Reason.MODE_NOT_SUPPORTED_YET
        }
        return BlockDecision.allow(packageName, reason, inSchedule.first())
    }

    /**
     * Resolves everything a rule should look for.
     *
     * A screen the user taught FocusGuard on their own device is checked first: it was
     * observed on the real app, so it beats a catalogue entry written from published
     * naming that may be wrong or outdated for their version.
     *
     * Rules saved before per-content selection existed carry no ids, so they fall back
     * to the app's short-form feeds, which is exactly what they used to block.
     */
    private fun targetsForRule(rule: BlockedAppRule): List<ContentTarget> {
        // Sanitised on use as well as on capture: a signature saved by an earlier
        // build may contain app-shell ids that would match every screen.
        val learnedIds = ScreenLearner.sanitise(rule.learnedViewIds)
        val learned = if (learnedIds.isNotEmpty()) {
            listOf(
                ContentTarget(
                    id = LEARNED_TARGET_PREFIX + rule.id,
                    packageName = rule.packageName,
                    label = "Learned screen",
                    description = "Captured on this device",
                    viewIdFragments = learnedIds
                )
            )
        } else {
            emptyList()
        }

        val catalogue = if (rule.blockedContentIds.isEmpty()) {
            ContentCatalog.targetsFor(rule.packageName).filter { it.isShortForm }
        } else {
            rule.blockedContentIds.mapNotNull { ContentCatalog.byId(it) }
        }

        return learned + catalogue
    }

    companion object {
        const val LEARNED_TARGET_PREFIX = "learned_"

        /**
         * Windows belonging to the system shell. Interrupting these would fight the OS
         * and can trap the user in a loop with no way out.
         */
        val DEFAULT_IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.settings",
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher3",
            "com.google.android.inputmethod.latin",
            "android"
        )
    }
}
