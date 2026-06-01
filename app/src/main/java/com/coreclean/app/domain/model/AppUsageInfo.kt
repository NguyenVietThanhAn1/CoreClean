package com.coreclean.app.domain.model

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeForegroundMs: Long,
    val lastTimeUsed: Long,
    val iconResId: Int? = null
)

data class UsageRange(val days: Int) {
    companion object {
        val LAST_7  = UsageRange(7)
        val LAST_30 = UsageRange(30)
    }
}
