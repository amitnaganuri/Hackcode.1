package com.example.service.detection

import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.model.ContentDetectionResult

/**
 * Recognises short-form feeds inside an app that is otherwise allowed.
 *
 * Content detection is best effort. App vendors rename view ids without notice, so a
 * detector that stops matching must fail *open* (report nothing detected) rather than
 * block the whole app — the user would otherwise lose messaging and search too.
 * Detectors are kept one-per-app so a change in one platform cannot affect the others.
 */
interface ContentDetector {

    /** Package this detector understands. */
    val packageName: String

    fun detect(root: AccessibilityNodeInfo?): ContentDetectionResult
}

/**
 * Shared node-tree scanning.
 *
 * Runs on the accessibility callback thread, so traversal is bounded: a deep or wide
 * tree must never stall event delivery for the whole device.
 */
object NodeScanner {

    private const val MAX_NODES = 600

    /** How many distinct ids/labels the diagnostic probe reports. */
    private const val PROBE_LIMIT = 25

    /** Upper bound when capturing a screen signature. */
    private const val MAX_COLLECTED_IDS = 120
    private const val MAX_DEPTH = 24

    /**
     * Returns the first view id resource name containing one of [idFragments], or null.
     *
     * Returning the matched id rather than a boolean means a misfire can be diagnosed
     * from Logcat instead of guessed at.
     */
    fun findViewId(root: AccessibilityNodeInfo?, idFragments: List<String>): String? =
        findFirst(root) { node ->
            val viewId = node.viewIdResourceName?.lowercase() ?: return@findFirst null
            if (idFragments.any { viewId.contains(it) }) viewId else null
        }

    /**
     * Returns the label of a *selected* navigation node matching one of [labels].
     *
     * Only selected nodes count: an unselected "Spotlight" button exists on every
     * Snapchat screen, while a selected one means the user is looking at Spotlight.
     */
    /** Every view id on screen, short form (without the package prefix). */
    fun collectViewIds(root: AccessibilityNodeInfo?): Set<String> {
        if (root == null) return emptySet()
        val ids = LinkedHashSet<String>()
        findFirst(root) { node ->
            node.viewIdResourceName?.substringAfter("/")?.let {
                if (ids.size < MAX_COLLECTED_IDS) ids.add(it)
            }
            null // never matches, so the walk covers the tree
        }
        return ids
    }

    fun findSelectedTab(root: AccessibilityNodeInfo?, labels: List<String>): String? {
        if (labels.isEmpty()) return null
        return findFirst(root) { node ->
            if (!node.isSelected) return@findFirst null
            val candidates = listOfNotNull(
                node.text?.toString()?.trim()?.lowercase(),
                node.contentDescription?.toString()?.trim()?.lowercase()
            )
            candidates.firstOrNull { candidate -> labels.any { candidate == it } }
        }
    }

    /**
     * Collects a sample of what the current screen exposes.
     *
     * Detection depends on app internals that change between releases, so when a rule
     * is active but nothing matches there has to be a way to see what the screen really
     * offers instead of guessing at ids.
     */
    fun describeScreen(root: AccessibilityNodeInfo?): String {
        if (root == null) return "no active window"
        val viewIds = LinkedHashSet<String>()
        val selectedLabels = LinkedHashSet<String>()

        findFirst(root) { node ->
            node.viewIdResourceName?.substringAfter("/")?.let {
                if (viewIds.size < PROBE_LIMIT) viewIds.add(it)
            }
            if (node.isSelected) {
                listOfNotNull(
                    node.text?.toString()?.trim(),
                    node.contentDescription?.toString()?.trim()
                ).filter { it.isNotEmpty() }
                    .forEach { if (selectedLabels.size < PROBE_LIMIT) selectedLabels.add(it) }
            }
            null // never matches, so the walk covers the tree up to the node budget
        }

        return "selectedTabs=[" + selectedLabels.joinToString(",") +
            "] viewIds=[" + viewIds.joinToString(",") + "]"
    }

    fun findFirst(
        root: AccessibilityNodeInfo?,
        selector: (AccessibilityNodeInfo) -> String?
    ): String? {
        if (root == null) return null
        var visited = 0

        fun walk(node: AccessibilityNodeInfo, depth: Int): String? {
            if (depth > MAX_DEPTH || visited >= MAX_NODES) return null
            visited++
            selector(node)?.let { return it }
            for (index in 0 until node.childCount) {
                val child = try {
                    node.getChild(index)
                } catch (e: Exception) {
                    null
                } ?: continue
                walk(child, depth + 1)?.let { return it }
            }
            return null
        }

        return try {
            walk(root, 0)
        } catch (e: Exception) {
            // A stale node throws once the window moves on; treat as "not detected"
            // so a transient failure never blocks an app the user is allowed to use.
            null
        }
    }
}
