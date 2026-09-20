package com.example.service.detection

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.model.ContentDetectionResult
import com.example.data.model.ContentTarget

/**
 * Scans the accessibility tree for any of the content surfaces the user chose to block.
 *
 * Platform quirks live in [com.example.data.model.ContentCatalog] as data, so supporting
 * a new app or a renamed view id is a catalogue edit rather than a code change. The
 * per-app detector classes below exist so a platform that ever needs genuinely different
 * logic has an obvious place to put it.
 */
class ContentDetectionEngine {

    /**
     * Returns the first target found on screen, or a not-detected result.
     *
     * Order follows the caller's list, so the most specific surfaces should come first.
     */
    fun detect(root: AccessibilityNodeInfo?, targets: List<ContentTarget>): ContentDetectionResult {
        if (root == null || targets.isEmpty()) return NOT_DETECTED

        for (target in targets) {
            val matchedId = NodeScanner.findViewId(root, target.viewIdFragments)
            if (matchedId != null) {
                Log.d(TAG, target.label + " matched view id: " + matchedId)
                return ContentDetectionResult(
                    detected = true,
                    contentType = target.label,
                    contentId = target.id
                )
            }

            // Fallback for apps that draw their feeds without exposing view ids.
            val matchedTab = NodeScanner.findSelectedTab(root, target.selectedTabLabels)
            if (matchedTab != null) {
                Log.d(TAG, target.label + " matched selected tab: " + matchedTab)
                return ContentDetectionResult(
                    detected = true,
                    contentType = target.label,
                    contentId = target.id
                )
            }
        }
        return NOT_DETECTED
    }

    private companion object {
        const val TAG = "FocusGuardDetect"
        val NOT_DETECTED = ContentDetectionResult(detected = false)
    }
}

/**
 * Per-app detectors.
 *
 * Each declares the package it understands and delegates to the shared engine with that
 * app's selected targets. Keeping them as named types makes the supported platforms
 * explicit and gives any future app-specific handling a home.
 */
abstract class PackageContentDetector(
    private val engine: ContentDetectionEngine = ContentDetectionEngine()
) : ContentDetector {

    override fun detect(root: AccessibilityNodeInfo?): ContentDetectionResult =
        detect(root, emptyList())

    fun detect(root: AccessibilityNodeInfo?, targets: List<ContentTarget>): ContentDetectionResult =
        engine.detect(root, targets)
}

class InstagramDetector : PackageContentDetector() {
    override val packageName: String = com.example.data.model.ContentCatalog.INSTAGRAM
}

class YouTubeDetector : PackageContentDetector() {
    override val packageName: String = com.example.data.model.ContentCatalog.YOUTUBE
}

class TikTokDetector : PackageContentDetector() {
    override val packageName: String = com.example.data.model.ContentCatalog.TIKTOK
}

class FacebookDetector : PackageContentDetector() {
    override val packageName: String = com.example.data.model.ContentCatalog.FACEBOOK
}

class SnapchatDetector : PackageContentDetector() {
    override val packageName: String = com.example.data.model.ContentCatalog.SNAPCHAT
}

class TwitterDetector : PackageContentDetector() {
    override val packageName: String = com.example.data.model.ContentCatalog.TWITTER
}

class RedditDetector : PackageContentDetector() {
    override val packageName: String = com.example.data.model.ContentCatalog.REDDIT
}

/**
 * Looks up the detector for a package.
 *
 * Adding a platform means adding one detector here plus its catalogue entries; the
 * engine and the service stay untouched.
 */
class ContentDetectorRegistry(
    detectors: List<PackageContentDetector> = listOf(
        InstagramDetector(),
        YouTubeDetector(),
        TikTokDetector(),
        FacebookDetector(),
        SnapchatDetector(),
        TwitterDetector(),
        RedditDetector()
    )
) {
    private val byPackage: Map<String, PackageContentDetector> =
        detectors.associateBy { it.packageName }

    fun detectorFor(packageName: String): PackageContentDetector? = byPackage[packageName]

    fun hasDetectorFor(packageName: String): Boolean = byPackage.containsKey(packageName)
}
