package com.example.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.util.Log

/**
 * Reports the real enabled/disabled state of [FocusAccessibilityService].
 *
 * The status is read from the system rather than cached, so the UI can never claim a
 * permission the user has not actually granted — a revoked service must show as off the
 * moment the user returns from Settings.
 */
object AccessibilityPermission {

    private const val TAG = "FocusGuardA11yPerm"

    enum class Status {
        /** Service is enabled and running. */
        GRANTED,

        /** Service is declared but the user has not switched it on. */
        DENIED
    }

    fun status(context: Context): Status =
        if (isServiceEnabled(context)) Status.GRANTED else Status.DENIED

    fun isServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context.applicationContext, FocusAccessibilityService::class.java)

        val enabledServices = try {
            Settings.Secure.getString(
                context.applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not read enabled accessibility services", e)
            null
        } ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabledServices) }
        while (splitter.hasNext()) {
            val component = ComponentName.unflattenFromString(splitter.next())
            if (component == expected) return true
        }
        return false
    }

    /**
     * Opens the system Accessibility settings. FocusGuard cannot grant this itself —
     * the user must switch the service on, which is the correct trust boundary for a
     * permission this broad.
     */
    fun openAccessibilitySettings(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Could not open accessibility settings", e)
        }
    }
}
