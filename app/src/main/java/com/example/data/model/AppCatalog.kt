package com.example.data.model

/**
 * Real Android package names for the apps FocusGuard can block.
 *
 * The blocking engine matches the foreground app by package name, so a fabricated
 * package would make a rule silently never fire. Every app offered in the UI must
 * resolve to a real package through this catalog.
 */
object AppCatalog {

    data class KnownApp(
        val displayName: String,
        val packageName: String,
        val category: String
    )

    val knownApps: List<KnownApp> = listOf(
        KnownApp("Instagram", "com.instagram.android", "Social"),
        KnownApp("YouTube", "com.google.android.youtube", "Video"),
        KnownApp("TikTok", "com.zhiliaoapp.musically", "Entertainment"),
        KnownApp("Twitter / X", "com.twitter.android", "Social"),
        KnownApp("Reddit", "com.reddit.frontpage", "Social"),
        KnownApp("Facebook", "com.facebook.katana", "Social"),
        KnownApp("Snapchat", "com.snapchat.android", "Social"),
        KnownApp("Chrome", "com.android.chrome", "Browser"),
        KnownApp("Netflix", "com.netflix.mediaclient", "Entertainment")
    )

    private val byDisplayName: Map<String, KnownApp> =
        knownApps.associateBy { it.displayName.lowercase() }

    fun packageNameFor(displayName: String): String? =
        byDisplayName[displayName.lowercase()]?.packageName

    fun categoryFor(displayName: String): String =
        byDisplayName[displayName.lowercase()]?.category ?: "Social"

    fun displayNameFor(packageName: String): String? =
        knownApps.firstOrNull { it.packageName == packageName }?.displayName

    /** Display names offered in the "Add Block" sheet. */
    fun selectableAppNames(): List<String> = knownApps.map { it.displayName }
}
