package com.example.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.view.accessibility.AccessibilityEvent
import com.example.FocusGuardApplication
import com.example.data.model.BlockDecision
import com.example.data.model.BlockMode
import com.example.data.model.ContentCatalog
import com.example.data.model.ContentDetectionResult
import com.example.data.model.ContentTarget
import com.example.data.repository.FocusGuardRepository
import com.example.service.detection.ContentDetectionEngine
import com.example.service.detection.NodeScanner
import com.example.ui.intervention.InterventionActivity

/**
 * Observes foreground window changes and hands each package to [BlockingEngine].
 *
 * This class is intentionally thin. It knows how to read the system and how to perform
 * effects (leave the app, show the intervention); every decision about *whether* to
 * block lives in the engine, which has no Android dependencies.
 */
class FocusAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val detectionEngine = ContentDetectionEngine()
    private var lastContentScanAtMillis = 0L

    /** Shared with the intervention screen; see [BlockSuppressor]. */
    private var suppressor: BlockSuppressor? = null

    /** Active while the user is teaching FocusGuard a screen. */
    private var screenLearner: ScreenLearner? = null

    /** Last time each content surface was blocked, for detecting a block loop. */
    private val lastContentBlockAt = mutableMapOf<String, Long>()

    /** Content surface currently on screen, and when it appeared. */
    private var currentContentKey: String? = null
    private var contentSinceMillis = 0L
    private var lastProbeAtMillis = 0L

    private var repository: FocusGuardRepository? = null
    private var blockingEngine: BlockingEngine? = null

    private var lastHandledPackage: String? = null
    private var lastInterventionAtMillis = 0L

    /** Package currently in the foreground, and when it got there. */
    private var foregroundPackage: String? = null
    private var foregroundSinceMillis = 0L

    private var usageTicker: Job? = null

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
        suppressor = container.blockSuppressor
        screenLearner = container.screenLearner
        seedForegroundFromActiveWindow()
        startUsageTicker()
        Log.i(TAG, "FocusGuard accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val isWindowChange = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        val isContentChange = event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        if (!isWindowChange && !isContentChange) return

        val packageName = event.packageName?.toString() ?: return
        val engine = blockingEngine ?: return
        val now = System.currentTimeMillis()

        // While teaching, record what the screen exposes and block nothing: an
        // intervention here would interrupt the very navigation being captured.
        if (handleLearning(packageName)) return

        // The grace window after a block stops the intervention re-firing while the app
        // is still showing the blocked screen. It must NOT be a blanket pause on the
        // app: sitting out the window and opening the next reel used to slip straight
        // through. So the decision is still computed while suppressed, and the moment
        // the blocked content is off screen the window is dropped.
        val wasSuppressed = suppressor?.isSuppressed(packageName) == true

        // Content-changed fires constantly while a feed scrolls. Only re-scan the node
        // tree for apps that actually have a detector, and never more than once per
        // throttle window, so event delivery for the whole device stays responsive.
        if (isContentChange) {
            if (!hasContentRuleFor(packageName)) return
            if (now - lastContentScanAtMillis < CONTENT_SCAN_THROTTLE_MILLIS) return
            lastContentScanAtMillis = now
        }

        // Bank the time spent in the app the user is leaving before judging the new one,
        // so a daily allowance is measured against real foreground time.
        if (isWindowChange) {
            trackForegroundChange(packageName, now)
        }

        val decision = engine.evaluate(packageName, now) { targets ->
            detectContent(packageName, targets)
        }

        if (!decision.shouldBlock) {
            // Reset the guard as soon as the user leaves the blocked app, so returning
            // to it triggers a fresh intervention.
            if (decision.reason != BlockDecision.Reason.IGNORED_PACKAGE) {
                lastHandledPackage = null
            }
            // Off the blocked screen: end the grace window now so the next reel is
            // caught immediately instead of riding out the remaining seconds.
            if (wasSuppressed) {
                suppressor?.clear()
                Log.d(TAG, "Left blocked content in $packageName; grace window cleared")
            }
            Log.d(TAG, "Allow $packageName (${decision.reason})")
            return
        }

        // Still on the blocked screen inside the grace window: stay quiet so the
        // intervention does not loop, but keep the window open.
        if (wasSuppressed) return

        handleBlock(packageName, decision)
    }

    private fun handleBlock(packageName: String, decision: BlockDecision) {
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

        val isContentBlock = rule.blockMode == BlockMode.CONTENT_LEVEL

        // A second block of the same surface soon after the first means navigating away
        // did not work — a tab-based feed such as Snapchat Spotlight does not respond to
        // BACK, and reopening the app restores the same tab. Escalating breaks the loop.
        var isRepeatBlock = false
        if (isContentBlock) {
            // A rule maps to a fixed set of content targets, so the rule id is a
            // precise enough key for spotting a repeat of the same surface.
            val contentKey = packageName + "::" + rule.id
            val previous = lastContentBlockAt[contentKey]
            isRepeatBlock = previous != null &&
                now - previous < BlockSuppressor.LOOP_WINDOW_MILLIS
            lastContentBlockAt[contentKey] = now
        }

        suppressor?.suppress(
            packageName,
            if (isRepeatBlock) BlockSuppressor.LOOP_ESCAPE_MILLIS
            else BlockSuppressor.POST_BLOCK_MILLIS
        )

        if (isContentBlock && !isRepeatBlock) {
            // Steer the app out of the feed so dismissing the intervention lands the
            // user back in the usable part of the app. Skipped on a repeat, where this
            // has already been shown not to help.
            performGlobalAction(GLOBAL_ACTION_BACK)
        }

        showIntervention(packageName, rule.appName, isContentBlock, isRepeatBlock)
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
    private fun showIntervention(
        packageName: String,
        appName: String,
        isContentBlock: Boolean,
        isRepeatBlock: Boolean
    ) {
        // The BACK action above is asynchronous. Starting the Activity in the same
        // breath would race it and the navigation would land on the intervention
        // instead of the app, so a content block waits for BACK to settle first.
        val startDelay = if (isContentBlock) BACK_SETTLE_DELAY_MILLIS else 0L
        serviceScope.launch {
            if (startDelay > 0L) delay(startDelay)
            try {
                startActivity(
                    InterventionActivity.intent(
                        this@FocusAccessibilityService,
                        packageName,
                        appName,
                        isContentBlock,
                        isRepeatBlock
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Could not show intervention for $packageName", e)
            }
        }
    }

    /**
     * Accumulates foreground time for the package being left.
     *
     * Android only notifies us on window changes, so without the ticker below a user who
     * sits in one app forever would never have their allowance counted.
     */
    private fun trackForegroundChange(newPackage: String, nowMillis: Long) {
        val previous = foregroundPackage
        if (previous == newPackage) return
        flushForegroundTime(nowMillis)
        flushContentTime(nowMillis)
        foregroundPackage = newPackage
        foregroundSinceMillis = nowMillis
    }

    private fun flushForegroundTime(nowMillis: Long) {
        val previous = foregroundPackage ?: return
        // Switching apps also ends whatever content surface was on screen.
        if (previous != foregroundPackage) flushContentTime(nowMillis)
        val elapsed = nowMillis - foregroundSinceMillis
        if (elapsed <= 0L) return
        if (previous == packageName) return
        if (previous in DefaultBlockingEngine.DEFAULT_IGNORED_PACKAGES) return
        repository?.addForegroundTime(previous, elapsed)
        foregroundSinceMillis = nowMillis
    }

    /**
     * True when an enabled content rule targets this package.
     *
     * Content-changed events fire constantly, so scanning is limited to apps the user
     * actually asked to filter rather than to a fixed list of known apps.
     */
    private fun hasContentRuleFor(packageName: String): Boolean {
        val rules = repository?.rules?.value ?: return false
        return rules.any {
            it.isEnabled &&
                it.blockMode == BlockMode.CONTENT_LEVEL &&
                it.packageName.equals(packageName, ignoreCase = true)
        }
    }

    /**
     * Records screen signatures while a teach session is running.
     *
     * Returns true when learning is active, so the caller skips evaluation entirely.
     */
    private fun handleLearning(packageName: String): Boolean {
        val learner = screenLearner ?: return false
        if (!learner.isActive) return false

        if (packageName == learner.activePackage()) {
            learner.record(packageName, NodeScanner.collectViewIds(rootInActiveWindow))
        }

        learner.finishIfDue()?.let { result ->
            if (result.signals.isEmpty()) {
                Log.w(TAG, "Teach session captured nothing for " + result.packageName)
                showToast("Nothing unique found. Open the app first, then navigate to the screen while the timer runs.")
            } else {
                Log.i(TAG, "Learned " + result.signals.size + " signals: " + result.signals)
                repository?.applyLearnedSignals(result.ruleId, result.signals)
                showToast("Learned this screen (" + result.signals.size + " signals). It is blocked now.")
            }
        }
        return true
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Asks the matching detector whether any of [targets] is on screen.
     *
     * Returns null when the app has no detector, which the engine reads as "no content
     * verdict available" rather than "nothing detected". Detection is also where
     * per-content viewing time is banked, because this is the only place that knows
     * which surface is actually being looked at.
     */
    private fun detectContent(
        packageName: String,
        targets: List<ContentTarget>
    ): ContentDetectionResult? {
        // Scanning is generic: it looks for the ids and tabs the caller asked about.
        // Gating it on a per-app detector class meant a screen the user taught on their
        // own device could never match unless that app happened to be in the catalogue,
        // which defeated the point of teaching one.
        val result = try {
            detectionEngine.detect(rootInActiveWindow, targets)
        } catch (e: Exception) {
            Log.w(TAG, "Content detection failed for $packageName", e)
            return null
        }

        val now = System.currentTimeMillis()
        trackContentTime(packageName, result, now)

        // A rule is watching this app but nothing matched. Report what the screen does
        // expose, so the catalogue can be corrected from real data rather than guesswork.
        if (!result.detected &&
            targets.isNotEmpty() &&
            now - lastProbeAtMillis > PROBE_INTERVAL_MILLIS
        ) {
            lastProbeAtMillis = now
            Log.i(
                PROBE_TAG,
                packageName + " no match for [" + targets.joinToString(",") { it.id } +
                    "] :: " + NodeScanner.describeScreen(rootInActiveWindow)
            )
        }

        return result
    }

    /**
     * Accumulates time spent inside a specific content surface.
     *
     * Kept separate from whole-app foreground time so a "30 minutes of Reels" budget
     * measures Reels, not Instagram as a whole.
     */
    private fun trackContentTime(
        packageName: String,
        result: ContentDetectionResult,
        nowMillis: Long
    ) {
        val key = if (result.detected && result.contentId != null) {
            ContentCatalog.usageKey(packageName, result.contentId)
        } else {
            null
        }

        if (key == currentContentKey) {
            // Still on the same surface: bank the slice since the last check.
            if (key != null) {
                val elapsed = nowMillis - contentSinceMillis
                if (elapsed > 0L) {
                    repository?.addForegroundTime(key, elapsed)
                    contentSinceMillis = nowMillis
                }
            }
            return
        }

        flushContentTime(nowMillis)
        currentContentKey = key
        contentSinceMillis = nowMillis
    }

    private fun flushContentTime(nowMillis: Long) {
        val key = currentContentKey ?: return
        val elapsed = nowMillis - contentSinceMillis
        if (elapsed > 0L) repository?.addForegroundTime(key, elapsed)
        currentContentKey = null
    }

    /**
     * Reads the package that owns the active window.
     *
     * Needed because window-change events only tell us about transitions. When the
     * service starts (first enable, app reinstall, system restart) the user is usually
     * already inside some app, and without this the service would not know which app
     * that is until they next switched — so usage would stop accruing and a daily
     * allowance would never fire.
     */
    private fun resolveActiveWindowPackage(): String? = try {
        rootInActiveWindow?.packageName?.toString()
    } catch (e: Exception) {
        Log.w(TAG, "Could not read active window", e)
        null
    }

    /** Adopts whatever app is on screen right now as the current foreground app. */
    private fun seedForegroundFromActiveWindow() {
        val current = resolveActiveWindowPackage() ?: return
        foregroundPackage = current
        foregroundSinceMillis = System.currentTimeMillis()
        Log.d(TAG, "Seeded foreground package as $current")
    }

    /**
     * Re-checks the app the user is sitting in. Without this, an allowance would only be
     * enforced at the moment they switched apps, which could be long past the limit.
     */
    private fun startUsageTicker() {
        usageTicker?.cancel()
        usageTicker = serviceScope.launch {
            while (true) {
                delay(USAGE_TICK_MILLIS)
                val now = System.currentTimeMillis()

                // A teach session must end on time even if the app has gone quiet.
                screenLearner?.let { learner ->
                    if (learner.isActive) {
                        learner.activePackage()?.let { target ->
                            if (foregroundPackage == target) {
                                learner.record(target, NodeScanner.collectViewIds(rootInActiveWindow))
                            }
                        }
                        learner.finishIfDue()?.let { result ->
                            if (result.signals.isNotEmpty()) {
                                repository?.applyLearnedSignals(result.ruleId, result.signals)
                                showToast(
                                    "Learned this screen (" + result.signals.size +
                                        " signals). It is blocked now."
                                )
                            } else {
                                showToast("Nothing unique found. Open the app first, then navigate to the screen while the timer runs.")
                            }
                        }
                        continue
                    }
                }

                // Fall back to the active window if no transition has been seen yet.
                val current = foregroundPackage ?: resolveActiveWindowPackage()?.also {
                    foregroundPackage = it
                    foregroundSinceMillis = now
                } ?: continue

                flushForegroundTime(now)
                val tickSuppressed = suppressor?.isSuppressed(current) == true
                val decision = blockingEngine?.evaluate(current, now) { targets ->
                    detectContent(current, targets)
                } ?: continue
                if (!decision.shouldBlock && tickSuppressed) {
                    suppressor?.clear()
                }
                if (tickSuppressed) continue
                Log.d(TAG, "Tick $current -> ${decision.reason}")
                if (decision.shouldBlock) {
                    handleBlock(current, decision)
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "FocusAccessibilityService interrupted")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        Log.i(TAG, "FocusGuard accessibility service disconnected")
        val now = System.currentTimeMillis()
        flushForegroundTime(now)
        flushContentTime(now)
        lastContentBlockAt.clear()
        usageTicker?.cancel()
        usageTicker = null
        foregroundPackage = null
        lastHandledPackage = null
        return super.onUnbind(intent)
    }

    private companion object {
        const val TAG = "FocusGuardA11y"

        /** Diagnostic tag: `adb logcat -s FocusGuardProbe:*` shows unmatched screens. */
        const val PROBE_TAG = "FocusGuardProbe"
        const val PROBE_INTERVAL_MILLIS = 4000L

        /**
         * Android emits several window-state events per app launch. Without a cooldown
         * a single launch would stack multiple interventions and log duplicate attempts.
         */
        const val INTERVENTION_COOLDOWN_MILLIS = 3000L

        /** How often an app the user is sitting in is re-checked against its allowance. */
        const val USAGE_TICK_MILLIS = 15_000L

        /** Minimum gap between node-tree scans while a feed is scrolling. */
        const val CONTENT_SCAN_THROTTLE_MILLIS = 700L


        /** Time for the global BACK action to take effect before showing the screen. */
        const val BACK_SETTLE_DELAY_MILLIS = 400L
    }
}
