package com.example.service

import com.example.data.model.BlockDecision
import com.example.data.model.BlockMode
import com.example.data.repository.FocusGuardRepository

/**
 * Decides whether the app currently in the foreground should be interrupted.
 *
 * Deliberately free of Android framework types so the whole policy is testable and so
 * [FocusAccessibilityService] stays a thin adapter: the service observes the system and
 * performs effects, the engine decides.
 */
interface BlockingEngine {
    fun evaluate(packageName: String, nowMillis: Long = System.currentTimeMillis()): BlockDecision
}

/**
 * Phase 2 policy — whole-app blocking only.
 *
 * A package is blocked when all of the following hold:
 *  1. it is not FocusGuard itself, the launcher, or system UI;
 *  2. an enabled rule targets its package name;
 *  3. the rule's mode is [BlockMode.HARD_BLOCK] (content-level modes arrive in Phase 7);
 *  4. a focus session is currently running;
 *  5. the rule's schedule covers this instant.
 *
 * Condition 4 makes the focus session the master switch: ending a session stops all
 * blocking immediately. That is the behaviour the Phase 2 acceptance tests describe.
 * If always-on shielding outside a session is wanted later, this is the single line to
 * relax, and the reason codes already distinguish the case.
 */
class DefaultBlockingEngine(
    private val repository: FocusGuardRepository,
    private val ownPackageName: String,
    private val scheduleEvaluator: ScheduleEvaluator = ScheduleEvaluator(),
    private val launcherPackages: Set<String> = DEFAULT_IGNORED_PACKAGES
) : BlockingEngine {

    override fun evaluate(packageName: String, nowMillis: Long): BlockDecision {
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

        // Phase 2 enforces whole-app blocking only; content-level and allowance modes
        // are evaluated in later phases.
        val hardBlocks = enabled.filter { it.blockMode == BlockMode.HARD_BLOCK }
        if (hardBlocks.isEmpty()) {
            return BlockDecision.allow(
                packageName,
                BlockDecision.Reason.MODE_NOT_SUPPORTED_YET,
                enabled.first()
            )
        }

        val session = repository.activeSession.value
        if (session == null || !session.isRunning || session.isPaused) {
            return BlockDecision.allow(
                packageName,
                BlockDecision.Reason.NO_ACTIVE_SESSION,
                hardBlocks.first()
            )
        }

        val scheduled = hardBlocks.firstOrNull {
            scheduleEvaluator.isActiveAt(it.scheduleText, nowMillis)
        } ?: return BlockDecision.allow(
            packageName,
            BlockDecision.Reason.OUTSIDE_SCHEDULE,
            hardBlocks.first()
        )

        return BlockDecision.block(packageName, scheduled, session.id)
    }

    companion object {
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
