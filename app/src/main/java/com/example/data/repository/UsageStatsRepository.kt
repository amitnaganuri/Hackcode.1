package com.example.data.repository

data class AppUsageInfo(
    val appName: String,
    val packageName: String,
    val usageMinutesToday: Int,
    val launchesToday: Int
)

interface UsageStatsRepository {
    fun getTodayUsageList(): List<AppUsageInfo>
    fun hasUsagePermission(): Boolean
}

class SampleUsageStatsRepository : UsageStatsRepository {
    override fun getTodayUsageList(): List<AppUsageInfo> {
        return listOf(
            AppUsageInfo("Instagram", "com.instagram.android", 42, 28),
            AppUsageInfo("YouTube", "com.google.android.youtube", 77, 19),
            AppUsageInfo("TikTok", "com.zhiliaoapp.musically", 36, 12),
            AppUsageInfo("Twitter / X", "com.twitter.android", 14, 8)
        )
    }

    override fun hasUsagePermission(): Boolean = true
}
