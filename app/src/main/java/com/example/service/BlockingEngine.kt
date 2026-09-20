package com.example.service

import com.example.data.model.BlockedAppRule
import com.example.data.repository.FocusGuardRepository

data class ContentDetectionResult(
    val packageName: String,
    val isShortsOrReels: Boolean,
    val matchedRule: BlockedAppRule? = null
)

interface BlockingEngine {
    fun evaluatePackage(packageName: String, currentWindowClassName: String? = null): ContentDetectionResult
}

class DefaultBlockingEngine(
    private val repository: FocusGuardRepository
) : BlockingEngine {
    override fun evaluatePackage(packageName: String, currentWindowClassName: String?): ContentDetectionResult {
        val rules = repository.rules.value
        val matched = rules.firstOrNull { it.packageName.equals(packageName, ignoreCase = true) && it.isEnabled }
        val isShorts = currentWindowClassName?.contains("reel", ignoreCase = true) == true ||
                currentWindowClassName?.contains("short", ignoreCase = true) == true
        return ContentDetectionResult(
            packageName = packageName,
            isShortsOrReels = isShorts,
            matchedRule = matched
        )
    }
}
