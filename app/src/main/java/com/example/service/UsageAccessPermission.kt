package com.example.service

import android.app.AppOpsManager
import android.os.Build
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import android.util.Log

/**
 * Reports whether "Usage access" has been granted.
 *
 * FocusGuard measures screen time from accessibility events, so this permission is not
 * required for anything the app currently does. It is surfaced honestly as optional
 * rather than shown as granted when it is not, which is what the Settings screen used
 * to do.
 */
object UsageAccessPermission {

    private const val TAG = "FocusGuardUsagePerm"

    fun isGranted(context: Context): Boolean = try {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        // unsafeCheckOpNoThrow only exists from API 29. Calling it on an older device
        // throws NoSuchMethodError, which is why an app can install everywhere and
        // still crash on some phones; checkOpNoThrow is the equivalent before that.
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
        Log.w(TAG, "Could not read usage-access state", e)
        false
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Could not open usage access settings", e)
        }
    }
}
