package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One launchable application installed on this device.
 *
 * [icon] is pre-rasterised on a background thread so the picker can render it directly;
 * it is null when the icon could not be loaded, and callers fall back to a generic mark.
 */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap? = null
)

/**
 * Lists the apps the user can choose to block.
 *
 * Replaces the previous hard-coded catalogue of nine apps, which meant anything not on
 * that list could never be blocked. Only activities that appear in the launcher are
 * returned, which is both what the user recognises as "an app" and the narrow slice the
 * manifest `<queries>` element grants visibility to.
 */
class InstalledAppsProvider(
    private val context: Context,
    private val ownPackageName: String = context.packageName
) {

    suspend fun loadInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolved = try {
            packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        } catch (e: Exception) {
            Log.e(TAG, "Could not query installed apps", e)
            return@withContext emptyList()
        }

        resolved.asSequence()
            .mapNotNull { info -> info.activityInfo?.packageName }
            // FocusGuard must not be blockable; that would lock the user out of the app
            // holding the only switch to turn blocking off.
            .filter { it != ownPackageName }
            .distinct()
            .mapNotNull { packageName -> toInstalledApp(packageManager, packageName) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun toInstalledApp(
        packageManager: PackageManager,
        packageName: String
    ): InstalledApp? = try {
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        InstalledApp(
            packageName = packageName,
            label = packageManager.getApplicationLabel(appInfo).toString(),
            icon = loadIcon(packageManager, packageName)
        )
    } catch (e: PackageManager.NameNotFoundException) {
        // Uninstalled between the query and here; skip rather than fail the whole list.
        null
    } catch (e: Exception) {
        Log.w(TAG, "Skipping $packageName", e)
        null
    }

    private fun loadIcon(packageManager: PackageManager, packageName: String): ImageBitmap? = try {
        packageManager.getApplicationIcon(packageName)
            .toBitmap(width = ICON_PX, height = ICON_PX)
            .asImageBitmap()
    } catch (e: Exception) {
        Log.w(TAG, "No icon for $packageName", e)
        null
    }

    private companion object {
        const val TAG = "InstalledApps"

        /** Large enough for the 44dp card logo on xxhdpi without wasting memory. */
        const val ICON_PX = 144
    }
}
