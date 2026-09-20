package com.example.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.FocusGuardApplication
import com.example.data.model.BlockDecision
import com.example.data.repository.FocusGuardRepository
import com.example.ui.intervention.InterventionActivity

/**
 * Observes foreground window changes and hands each package to [BlockingEngine].
 *
 * This class is intentionally thin. It knows how to read the system and how to perform
 * effects (leave the app, show the intervention); every decision about *whether* to
 * block lives in the engine, which has no Android dependencies.
 */
class FocusAccessibilityService : AccessibilityService() {

    private var repository: FocusGuardRepository? = null
    private var blockingEngine: BlockingEngine? = null

    private var lastHandledPackage: String? = null
    private var lastInterventionAtMillis = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val container = FocusGuardApplication.container()
        if (container == null) {
            // Should not happen: Application.onCreate always runs before a service in
            // the same process. Logged rather than crashed so accessibility stays alive.
            Log.e(TAG, "AppContainer unavailable; blocking disabled for this connection")
            return
        }
        repository = container.repository
        blockingEngine = container.blockingEngine
        Log.i(TAG, "FocusGuard accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        val engine = blockingEngine ?: return

        val decision = engine.evaluate(packageName)

        if (!decision.shouldBlock) {
            // Reset the guard as soon as the user leaves the blocked app, so returning
            // to it triggers a fresh intervention.
            if (decision.reason != BlockDecision.Reason.IGNORED_PACKAGE) {
                lastHandledPackage = null
            }
            Log.d(TAG, "Allow $packageName (${decision.reason})")
            return
        }

        val rule = decision.rule ?: return
        val now = System.currentTimeMillis()
        val isRepeat = packageName == lastHandledPackage &&
            now - lastInterventionAtMillis < INTERVENTION_COOLDOWN_MILLIS
        if (isRepeat) return

        lastHandledPackage = packageName
        lastInterventionAtMillis = now

        Log.i(TAG, "BLOCK $packageName via rule ${rule.id} (${rule.filterLabel})")

        repository?.recordDistractionAttempt(
            packageName = packageName,
            appName = rule.appName,
            ruleId = rule.id,
            ruleLabel = rule.filterLabel,
            blockMode = rule.blockMode,
            sessionId = decision.sessionId,
            nowMillis = now
        )

        showIntervention(packageName, rule.appName)
    }

    /**
     * Presents the intervention over the blocked app.
     *
     * Deliberately does NOT call performGlobalAction(GLOBAL_ACTION_HOME) first.
     * That action is asynchronous, so it lands *after* this Activity starts and
     * dismisses the intervention again, leaving the user on the launcher and
     * re-triggering the block in a loop.
     *
     * The intervention is a full-screen Activity in its own task, so it covers the
     * blocked app on its own. If the user backs out of it, the blocked app returns to
     * the foreground, which emits a fresh window event and blocks it again — the shield
     * stays up rather than being a one-shot.
     */
    private fun showIntervention(packageName: String, appName: String) {
        try {
            startActivity(InterventionActivity.intent(this, packageName, appName))
        } catch (e: Exception) {
            Log.e(TAG, "Could not show intervention for $packageName", e)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "FocusAccessibilityService interrupted")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        Log.i(TAG, "FocusGuard accessibility service disconnected")
        lastHandledPackage = null
        return super.onUnbind(intent)
    }

    private companion object {
        const val TAG = "FocusGuardA11y"

        /**
         * Android emits several window-state events per app launch. Without a cooldown
         * a single launch would stack multiple interventions and log duplicate attempts.
         */
        const val INTERVENTION_COOLDOWN_MILLIS = 3000L
    }
}
