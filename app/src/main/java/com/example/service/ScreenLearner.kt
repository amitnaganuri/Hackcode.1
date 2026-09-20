package com.example.service

/**
 * Captures the signature of a screen the user wants blocked.
 *
 * Built-in detection relies on knowing each app's internal view ids, which cannot be
 * verified for every app and version and which the vendors rename freely. Rather than
 * keep guessing, this lets the user point FocusGuard at a screen on their own device:
 * they open the app, navigate to the screen, and the ids actually present are recorded.
 *
 * The recording is a diff. The first sample taken after the app comes to the foreground
 * is its landing screen; the last sample is the screen the user navigated to. Ids unique
 * to the last sample are what distinguish that screen, so matching them will not fire on
 * the rest of the app — the failure mode that once blocked YouTube outright.
 */
class ScreenLearner(private val clock: () -> Long = System::currentTimeMillis) {

    data class Session(
        val ruleId: String,
        val packageName: String,
        val endsAtMillis: Long
    )

    data class Result(
        val ruleId: String,
        val packageName: String,
        val signals: List<String>
    )

    @Volatile
    private var session: Session? = null

    /** Union of every distinct screen seen before the current one. */
    private val seenEarlier = mutableSetOf<String>()
    private var latest: Set<String> = emptySet()
    private var distinctScreens = 0

    @Volatile
    private var pendingResult: Result? = null

    val isActive: Boolean get() = session != null

    fun activePackage(): String? = session?.packageName

    fun start(ruleId: String, packageName: String, durationMillis: Long) {
        session = Session(ruleId, packageName, clock() + durationMillis)
        seenEarlier.clear()
        latest = emptySet()
        distinctScreens = 0
        pendingResult = null
    }

    fun cancel() {
        session = null
        seenEarlier.clear()
        latest = emptySet()
        distinctScreens = 0
    }

    /** Records one sample of the ids visible in the target app. */
    fun record(packageName: String, viewIds: Collection<String>) {
        val current = session ?: return
        if (packageName != current.packageName) return
        if (viewIds.isEmpty()) return

        val filtered = viewIds.filterNot { it in FRAMEWORK_IDS }.toSet()
        if (filtered.isEmpty()) return

        if (filtered == latest) return

        // The screen changed, so everything shown until now belongs to earlier screens.
        seenEarlier.addAll(latest)
        latest = filtered
        distinctScreens++
    }

    /**
     * Ends the session if its window has elapsed, producing the learned signature.
     *
     * An empty signature means the user never navigated: only one screen was seen, so
     * nothing distinguishes it from the rest of the app. Saving that would block the
     * whole app, so it is reported as a failure for the user to retry instead.
     */
    fun finishIfDue(): Result? {
        val current = session ?: return null
        if (clock() < current.endsAtMillis) return null

        // Only ids unique to the final screen identify it. Anything that also appeared
        // on an earlier screen — the toolbar, the tab bar, the app's shell — would make
        // the signature match the whole app and block it outright.
        //
        // Seeing a single screen means the user opened the app already on it and never
        // navigated, so there is nothing to diff against and no way to tell that screen
        // apart from the app itself. Refusing is the only safe answer.
        val signals = if (distinctScreens < 2) {
            emptyList()
        } else {
            (latest - seenEarlier).take(MAX_SIGNALS).toList()
        }

        session = null
        seenEarlier.clear()
        latest = emptySet()
        distinctScreens = 0

        val result = Result(current.ruleId, current.packageName, signals)
        pendingResult = result
        return result
    }

    fun consumeResult(): Result? {
        val result = pendingResult
        pendingResult = null
        return result
    }

    companion object {
        /**
         * Ids every Android app has. They identify nothing, and including them would
         * make a learned signature match the entire app.
         */
        /** Ids that identify no particular screen; see [sanitise]. */
        val FRAMEWORK_IDS = setOf(
            "content",
            "action_bar_root",
            "decor_content_parent",
            "action_bar_container",
            "action_bar",
            "action_bar_title",
            "action_mode_bar_stub",
            "navigationBarBackground",
            "statusBarBackground",
            "rootView",
            "root_view",
            "container",
            "fragment_container",
            "fragment_container_view",
            "parentPanel",
            "contentPanel",
            // App shell present on every screen of most apps.
            "toolbar",
            "appbar",
            "app_bar",
            "app_bar_layout",
            "appbar_layout",
            "coordinator",
            "coordinator_layout",
            "drawer_layout",
            "bottom_navigation",
            "bottom_navigation_bar",
            "navigation_bar",
            "tabs",
            "tab_layout",
            "overflow_action_button"
        )

        /** Enough to identify a screen without bloating the persisted rule. */
        const val MAX_SIGNALS = 12

        /**
         * Strips ids that appear on every screen of an app.
         *
         * Applied when a signature is used, not only when it is captured, so a
         * signature stored by an earlier build cannot block a whole app.
         */
        fun sanitise(signals: List<String>): List<String> =
            signals.filterNot { it in FRAMEWORK_IDS }
    }
}
