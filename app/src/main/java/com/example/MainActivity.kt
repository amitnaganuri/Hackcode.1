package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.service.AccessibilityPermission
import com.example.service.UsageAccessPermission
import com.example.ui.navigation.FocusGuardApp
import com.example.ui.onboarding.PermissionOnboardingScreen
import com.example.ui.theme.FocusGuardTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      FocusGuardTheme {
        val context = LocalContext.current

        // Permission state is read from the system on every resume rather than cached.
        // The user leaves to grant it in system Settings and comes straight back, so a
        // value captured once would be wrong exactly when it matters.
        var isAccessibilityGranted by remember {
          mutableStateOf(AccessibilityPermission.isServiceEnabled(context))
        }
        var isUsageAccessGranted by remember {
          mutableStateOf(UsageAccessPermission.isGranted(context))
        }

        // Set when the user chooses to look around without granting. Deliberately not
        // persisted: if the permission is still missing next launch they are asked
        // again, while granting it means this screen is never shown again.
        var skippedThisSession by remember { mutableStateOf(false) }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
          val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
              isAccessibilityGranted = AccessibilityPermission.isServiceEnabled(context)
              isUsageAccessGranted = UsageAccessPermission.isGranted(context)
            }
          }
          lifecycleOwner.lifecycle.addObserver(observer)
          onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        if (isAccessibilityGranted || skippedThisSession) {
          FocusGuardApp(
            // moveTaskToBack leaves the app running so a capture window keeps going.
            onMinimise = { moveTaskToBack(true) }
          )
        } else {
          PermissionOnboardingScreen(
            isAccessibilityGranted = isAccessibilityGranted,
            isUsageAccessGranted = isUsageAccessGranted,
            onGrantAccessibility = { AccessibilityPermission.openAccessibilitySettings(context) },
            onGrantUsageAccess = { UsageAccessPermission.openUsageAccessSettings(context) },
            onContinueAnyway = { skippedThisSession = true }
          )
        }
      }
    }
  }
}
