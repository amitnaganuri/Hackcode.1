package com.example.service

/**
 * Short-lived "leave this app alone" windows, shared between the accessibility service
 * and the intervention screen.
 *
 * Content-level blocking interrupts a surface *inside* an app the user is allowed to
 * keep using, so after an intervention they need a moment to navigate somewhere else.
 * Without that moment the sequence is: block, dismiss, land back on the same feed,
 * block again — a loop only force-closing the app escapes.
 *
 * The window has to start when the user actually returns to the app, not when the block
 * happened, because a person reading the intervention can easily take longer than the
 * grace period. The Activity and the service run in the same process, so this object is
 * shared through the app container rather than sent as a broadcast.
 */
class BlockSuppressor(private val clock: () -> Long = System::currentTimeMillis) {

    @Volatile
    private var suppressedKey: String? = null

    @Volatile
    private var suppressedUntilMillis = 0L

    fun suppress(key: String, durationMillis: Long) {
        suppressedKey = key
        suppressedUntilMillis = clock() + durationMillis
    }

    fun isSuppressed(key: String): Boolean =
        key == suppressedKey && clock() < suppressedUntilMillis

    fun clear() {
        suppressedKey = null
        suppressedUntilMillis = 0L
    }

    companion object {
        /** Covers the app settling after the service navigates it out of a feed. */
        const val POST_BLOCK_MILLIS = 3_000L

        /** Breathing room once the user is back in the app, to navigate away. */
        const val RETURN_GRACE_MILLIS = 8_000L

        /**
         * Applied when the same surface is blocked twice in quick succession, which
         * means navigating away did not work. A longer pause guarantees the user can
         * always reach a way out instead of bouncing between app and intervention.
         */
        const val LOOP_ESCAPE_MILLIS = 20_000L

        /** Two blocks of the same surface inside this window count as a loop. */
        const val LOOP_WINDOW_MILLIS = 20_000L
    }
}
