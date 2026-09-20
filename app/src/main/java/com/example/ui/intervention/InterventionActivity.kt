package com.example.ui.intervention

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
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
        setContent {
            FocusGuardTheme {
                ActiveFocusChamberScreen(
                    viewModel = viewModel(factory = FocusGuardViewModel.Factory),
                    // Leaving the intervention returns the user to the launcher rather
                    // than to the blocked app they just came from.
                    onReturnToFocus = { goHome() },
                    onNavigateToEditor = { goHome() }
                )
            }
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        finish()
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
        const val EXTRA_BLOCKED_APP_NAME = "blocked_app_name"

        fun intent(context: Context, blockedPackage: String, blockedAppName: String): Intent =
            Intent(context, InterventionActivity::class.java).apply {
                putExtra(EXTRA_BLOCKED_PACKAGE, blockedPackage)
                putExtra(EXTRA_BLOCKED_APP_NAME, blockedAppName)
                // NEW_TASK is required when starting from a Service context;
                // CLEAR_TOP avoids stacking a second intervention over an existing one.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
    }
}
