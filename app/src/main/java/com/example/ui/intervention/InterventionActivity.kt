package com.example.ui.intervention

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.FocusGuardApplication
import com.example.service.BlockSuppressor
import com.example.ui.screens.chamber.ActiveFocusChamberScreen
import com.example.ui.theme.FocusGuardTheme
import com.example.viewmodel.FocusGuardViewModel

/**
 * Full-screen host for the existing intervention UI.
 *
 * The accessibility service cannot render Compose itself, so when a blocked app is
 * detected it launches this Activity, which shows the unmodified
 * [ActiveFocusChamberScreen]. No new visual design is introduced here — this is purely
 * a container so the same screen reachable from the in-app "chamber" route can also be
 * presented over another application.
 */
class InterventionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // The hardware/gesture back gesture must leave the same way the button does,
        // otherwise finishing this task drops the user on the launcher.
        onBackPressedDispatcher.addCallback(this) { dismiss() }

        setContent {
            FocusGuardTheme {
                ActiveFocusChamberScreen(
                    viewModel = viewModel(factory = FocusGuardViewModel.Factory),
                    onReturnToFocus = { dismiss() },
                    onNavigateToEditor = { dismiss() }
                )
            }
        }
    }

    /** launchMode is singleTask, so a repeat block arrives here rather than in onCreate. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    /**
     * Leaves the intervention.
     *
     * For a content-level block the service has already steered the app out of the
     * short-form feed, so the user is returned to that app — blocking Reels should not
     * cost them the rest of the app. This Activity runs in its own task, so simply
     * finishing would drop them on the launcher; the app's own task has to be brought
     * back to the front explicitly.
     *
     * A whole-app block has nowhere safe to return to, so it goes to the launcher.
     */
    private fun dismiss() {
        val isContentBlock = intent?.getBooleanExtra(EXTRA_IS_CONTENT_BLOCK, false) ?: false
        val isRepeatBlock = intent?.getBooleanExtra(EXTRA_IS_REPEAT_BLOCK, false) ?: false
        val blockedPackage = intent?.getStringExtra(EXTRA_BLOCKED_PACKAGE)

        // The grace period starts now, as the user returns, rather than when the block
        // fired: someone reading the intervention easily outlasts a few seconds, and an
        // expired window would re-block them the instant they land back in the app.
        if (!blockedPackage.isNullOrBlank()) {
            val duration = if (isRepeatBlock) {
                BlockSuppressor.LOOP_ESCAPE_MILLIS
            } else {
                BlockSuppressor.RETURN_GRACE_MILLIS
            }
            FocusGuardApplication.container()?.blockSuppressor?.suppress(blockedPackage, duration)
        }

        // Returning into the app is only safe the first time. On a repeat the app has
        // already proven it restores the blocked surface, so going back would simply
        // start the loop again; the launcher is the reliable exit.
        if (isContentBlock && !isRepeatBlock && !blockedPackage.isNullOrBlank()) {
            if (returnToApp(blockedPackage)) {
                finish()
                return
            }
        }

        goHome()
        finish()
    }

    /**
     * Sends the user back to the blocked app's *home* screen.
     *
     * Resuming the existing task simply restored the screen that was just blocked —
     * the reel, the story — so dismissing the intervention put the user straight back
     * where they started. CLEAR_TASK drops that stack and launches the app fresh, which
     * is its home screen, keeping the rest of the app available without reopening the
     * feed. Returns false if the app cannot be launched.
     */
    private fun returnToApp(blockedPackage: String): Boolean = try {
        val launchIntent = packageManager.getLaunchIntentForPackage(blockedPackage)
        if (launchIntent == null) {
            false
        } else {
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
            startActivity(launchIntent)
            true
        }
    } catch (e: Exception) {
        Log.w(TAG, "Could not return to $blockedPackage", e)
        false
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }

    companion object {
        private const val TAG = "FocusGuardIntervention"

        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
        const val EXTRA_BLOCKED_APP_NAME = "blocked_app_name"
        const val EXTRA_IS_CONTENT_BLOCK = "is_content_block"
        const val EXTRA_IS_REPEAT_BLOCK = "is_repeat_block"

        fun intent(
            context: Context,
            blockedPackage: String,
            blockedAppName: String,
            isContentBlock: Boolean = false,
            isRepeatBlock: Boolean = false
        ): Intent =
            Intent(context, InterventionActivity::class.java).apply {
                putExtra(EXTRA_BLOCKED_PACKAGE, blockedPackage)
                putExtra(EXTRA_BLOCKED_APP_NAME, blockedAppName)
                putExtra(EXTRA_IS_CONTENT_BLOCK, isContentBlock)
                putExtra(EXTRA_IS_REPEAT_BLOCK, isRepeatBlock)
                // NEW_TASK is required when starting from a Service context;
                // CLEAR_TOP avoids stacking a second intervention over an existing one.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
    }
}
