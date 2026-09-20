package com.example

import com.example.service.ScreenLearner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The learned signature decides whether a rule blocks one screen or an entire app, so
 * the diff that produces it is worth pinning down precisely.
 */
class ScreenLearnerTest {

    private var now = 0L
    private val learner = ScreenLearner { now }

    private fun runSession(vararg screens: Set<String>): List<String> {
        learner.start("rule_1", "com.example.app", durationMillis = 100)
        screens.forEach { learner.record("com.example.app", it) }
        now = 200
        return learner.finishIfDue()?.signals.orEmpty()
    }

    @Test
    fun `keeps only ids unique to the final screen`() {
        val landing = setOf("toolbar", "tab_bar", "feed_list", "story_tray")
        val target = setOf("toolbar", "tab_bar", "story_viewer", "story_progress")

        val signals = runSession(landing, target)

        assertEquals(listOf("story_viewer", "story_progress"), signals)
    }

    @Test
    fun `reports nothing when the user never navigated`() {
        // Only one screen seen: nothing distinguishes it, and saving it would block the
        // whole app. An empty result is what tells the service to ask for a retry.
        val signals = runSession(setOf("toolbar", "feed_list", "tab_bar"))

        assertTrue(signals.isEmpty())
    }

    @Test
    fun `drops generic app shell ids even when they are unique to the last screen`() {
        val signals = runSession(
            setOf("feed_list"),
            setOf("toolbar", "app_bar_layout", "bottom_navigation", "spotlight_player")
        )

        assertEquals(listOf("spotlight_player"), signals)
    }

    @Test
    fun `ignores samples from a different package`() {
        learner.start("rule_1", "com.example.app", durationMillis = 100)
        learner.record("com.example.app", setOf("feed_list"))
        learner.record("com.other.app", setOf("unrelated_view"))
        learner.record("com.example.app", setOf("reels_viewer"))
        now = 200

        assertEquals(listOf("reels_viewer"), learner.finishIfDue()?.signals)
    }

    @Test
    fun `session stays open until its window elapses`() {
        learner.start("rule_1", "com.example.app", durationMillis = 100)
        now = 50
        assertTrue(learner.finishIfDue() == null)
        assertTrue(learner.isActive)
    }
}
