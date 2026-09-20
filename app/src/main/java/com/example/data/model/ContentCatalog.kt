package com.example.data.model

/**
 * One blockable surface inside an app, for example YouTube Shorts or Instagram Reels.
 *
 * [viewIdFragments] are matched, case-insensitively, against the `viewIdResourceName`
 * of nodes in the accessibility tree. They must name surfaces that exist *only* while
 * that content is on screen. A fragment that also matches a tab button or a tray on the
 * home screen would block the whole app the moment it opened — that bug cost a working
 * YouTube once already.
 */
data class ContentTarget(
    val id: String,
    val packageName: String,
    val label: String,
    val description: String,
    val viewIdFragments: List<String>,
    /**
     * Navigation labels that identify this surface when its tab is *selected*.
     *
     * Some apps — Snapchat especially — draw their feeds in custom surfaces that expose
     * no view ids at all, so id matching alone never fires. Android still marks the
     * active navigation item as selected, which says where the user actually is.
     * Requiring `isSelected` is what keeps this from matching a tab button that is
     * merely present, the mistake that once blocked YouTube the moment it opened.
     */
    val selectedTabLabels: List<String> = emptyList(),
    /** Short-form feeds are pre-ticked when a content rule is created. */
    val isShortForm: Boolean = false
)

/**
 * The per-app menu of blockable content.
 *
 * Only YouTube Shorts has been verified against a real app on the test device. Every
 * other entry is written from each platform's published view-id naming and should be
 * treated as best effort: if a surface stops matching, the app simply stays usable
 * rather than being blocked wholesale, and the ids here are the single place to adjust.
 */
object ContentCatalog {

    const val YOUTUBE = "com.google.android.youtube"
    const val INSTAGRAM = "com.instagram.android"
    const val TIKTOK = "com.zhiliaoapp.musically"
    const val FACEBOOK = "com.facebook.katana"
    const val SNAPCHAT = "com.snapchat.android"
    const val TWITTER = "com.twitter.android"
    const val REDDIT = "com.reddit.frontpage"

    val targets: List<ContentTarget> = listOf(
        // ---------------------------------------------------------------- YouTube
        ContentTarget(
            id = "youtube_shorts",
            selectedTabLabels = listOf("shorts"),
            packageName = YOUTUBE,
            label = "Shorts",
            description = "Vertical short-form video feed",
            // Verified on device: reel_player_underlay matches while Shorts is playing.
            viewIdFragments = listOf(
                "reel_recycler",
                "reel_player_page_container",
                "reel_player_underlay",
                "reel_watch_player",
                "shorts_video_container"
            ),
            isShortForm = true
        ),
        ContentTarget(
            id = "youtube_long_video",
            packageName = YOUTUBE,
            label = "Long videos",
            description = "The normal watch page",
            viewIdFragments = listOf(
                "watch_player",
                "player_fragment_container",
                "watch_while_player"
            )
        ),
        ContentTarget(
            id = "youtube_home_feed",
            selectedTabLabels = listOf("home"),
            packageName = YOUTUBE,
            label = "Home feed",
            description = "The recommended videos grid",
            viewIdFragments = listOf("browse_results", "results_list")
        ),

        // -------------------------------------------------------------- Instagram
        ContentTarget(
            id = "instagram_reels",
            selectedTabLabels = listOf("reels"),
            packageName = INSTAGRAM,
            label = "Reels",
            description = "Full-screen short video feed",
            viewIdFragments = listOf(
                "clips_viewer",
                "clips_video_container",
                "clips_swipe_refresh",
                "clips_item"
            ),
            isShortForm = true
        ),
        ContentTarget(
            id = "instagram_stories",
            packageName = INSTAGRAM,
            label = "Stories",
            description = "Full-screen story viewer",
            viewIdFragments = listOf("reel_viewer_texture_view", "reel_viewer_media")
        ),
        ContentTarget(
            id = "instagram_explore",
            selectedTabLabels = listOf("search and explore", "explore"),
            packageName = INSTAGRAM,
            label = "Explore",
            description = "Discovery grid",
            viewIdFragments = listOf("explore_grid", "discovery_recycler")
        ),
        ContentTarget(
            id = "instagram_feed",
            selectedTabLabels = listOf("home"),
            packageName = INSTAGRAM,
            label = "Home feed",
            description = "The main scrolling timeline",
            viewIdFragments = listOf("feed_recycler_view", "main_feed_recycler")
        ),

        // ----------------------------------------------------------------- TikTok
        ContentTarget(
            id = "tiktok_for_you",
            selectedTabLabels = listOf("for you"),
            packageName = TIKTOK,
            label = "For You feed",
            description = "The main recommendation feed",
            viewIdFragments = listOf(
                "vertical_view_pager",
                "video_feed_recycler",
                "feed_video_container"
            ),
            isShortForm = true
        ),
        ContentTarget(
            id = "tiktok_following",
            selectedTabLabels = listOf("following"),
            packageName = TIKTOK,
            label = "Following feed",
            description = "Videos from accounts you follow",
            viewIdFragments = listOf("following_feed", "follow_feed_container")
        ),
        ContentTarget(
            id = "tiktok_live",
            packageName = TIKTOK,
            label = "LIVE",
            description = "Live streams",
            viewIdFragments = listOf("live_room_container", "live_player")
        ),

        // --------------------------------------------------------------- Facebook
        ContentTarget(
            id = "facebook_reels",
            selectedTabLabels = listOf("reels"),
            packageName = FACEBOOK,
            label = "Reels",
            description = "Short-form video feed",
            viewIdFragments = listOf("video_reels", "reels_viewer", "reels_player"),
            isShortForm = true
        ),
        ContentTarget(
            id = "facebook_feed",
            selectedTabLabels = listOf("home", "news feed"),
            packageName = FACEBOOK,
            label = "News feed",
            description = "The main scrolling timeline",
            viewIdFragments = listOf("news_feed_recycler", "feed_story_container")
        ),
        ContentTarget(
            id = "facebook_stories",
            packageName = FACEBOOK,
            label = "Stories",
            description = "Full-screen story viewer",
            viewIdFragments = listOf("story_viewer", "stories_tray_container")
        ),
        ContentTarget(
            id = "facebook_watch",
            selectedTabLabels = listOf("watch", "video"),
            packageName = FACEBOOK,
            label = "Watch",
            description = "Long-form video tab",
            viewIdFragments = listOf("watch_feed", "video_home_container")
        ),

        // --------------------------------------------------------------- Snapchat
        ContentTarget(
            id = "snapchat_spotlight",
            selectedTabLabels = listOf("spotlight"),
            packageName = SNAPCHAT,
            label = "Spotlight",
            description = "Short-form video feed",
            viewIdFragments = listOf("spotlight", "ngs_spotlight"),
            isShortForm = true
        ),
        ContentTarget(
            id = "snapchat_stories",
            selectedTabLabels = listOf("stories"),
            packageName = SNAPCHAT,
            label = "Stories",
            description = "Friend and creator stories",
            viewIdFragments = listOf("story_player", "stories_feed", "ngs_stories")
        ),
        ContentTarget(
            id = "snapchat_discover",
            selectedTabLabels = listOf("discover"),
            packageName = SNAPCHAT,
            label = "Discover",
            description = "Publisher content feed",
            viewIdFragments = listOf("discover_feed", "publisher_player", "ngs_discover")
        ),

        // -------------------------------------------------------------- Twitter/X
        ContentTarget(
            id = "x_video_feed",
            packageName = TWITTER,
            label = "Video feed",
            description = "Full-screen immersive video",
            viewIdFragments = listOf("immersive_player", "video_player_view"),
            isShortForm = true
        ),
        ContentTarget(
            id = "x_for_you",
            selectedTabLabels = listOf("for you", "home"),
            packageName = TWITTER,
            label = "For You timeline",
            description = "Algorithmic timeline",
            viewIdFragments = listOf("timeline_recycler", "home_timeline")
        ),

        // ----------------------------------------------------------------- Reddit
        ContentTarget(
            id = "reddit_video_feed",
            packageName = REDDIT,
            label = "Video feed",
            description = "Full-screen short video feed",
            viewIdFragments = listOf("video_player_container", "media_player_view"),
            isShortForm = true
        ),
        ContentTarget(
            id = "reddit_home_feed",
            selectedTabLabels = listOf("home"),
            packageName = REDDIT,
            label = "Home feed",
            description = "The main post list",
            viewIdFragments = listOf("link_list_recycler", "feed_recycler_view")
        )
    )

    private val byPackage: Map<String, List<ContentTarget>> = targets.groupBy { it.packageName }
    private val byId: Map<String, ContentTarget> = targets.associateBy { it.id }

    fun targetsFor(packageName: String): List<ContentTarget> = byPackage[packageName].orEmpty()

    fun byId(id: String): ContentTarget? = byId[id]

    fun hasTargets(packageName: String): Boolean = byPackage.containsKey(packageName)

    /** Pre-selection for a new content rule: the short-form feeds. */
    fun defaultTargetIdsFor(packageName: String): List<String> =
        targetsFor(packageName).filter { it.isShortForm }.map { it.id }

    /** Composite key used to accumulate per-content usage time. */
    fun usageKey(packageName: String, contentId: String): String = packageName + "::" + contentId
}
