package com.example.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Placeholder AccessibilityService architecture ready for production blocking engine.
 * When enabled, inspects current window changes and detects Shorts/Reels nodes.
 */
class FocusAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString()

        Log.d("FocusAccessibility", "Event from $packageName, class: $className")
        // TODO: In full production, evaluate package via BlockingEngine and launch Intervention Activity/Overlay
    }

    override fun onInterrupt() {
        Log.d("FocusAccessibility", "FocusAccessibilityService interrupted")
    }
}
