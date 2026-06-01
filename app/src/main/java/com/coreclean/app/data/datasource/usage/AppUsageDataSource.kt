package com.coreclean.app.data.datasource.usage

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.coreclean.app.domain.model.AppUsageInfo
import com.coreclean.app.domain.model.UsageRange
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

class AppUsageDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }
    private val packageManager: PackageManager get() = context.packageManager

    suspend fun getUsageStats(range: UsageRange): List<AppUsageInfo> = withContext(Dispatchers.IO) {
        val endMs = System.currentTimeMillis()
        val startMs = endMs - range.days * 24L * 60 * 60 * 1000

        val stats: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startMs, endMs
        ) ?: emptyList()

        // Merge by package name
        val merged = mutableMapOf<String, UsageStats>()
        for (s in stats) {
            val existing = merged[s.packageName]
            if (existing == null) {
                merged[s.packageName] = s
            } else {
                // Keep the entry with higher total foreground time; update lastTimeUsed
                if (s.totalTimeInForeground > existing.totalTimeInForeground) {
                    merged[s.packageName] = s
                }
            }
        }

        merged.values
            .filter { it.totalTimeInForeground > 0 }
            .filter { !isSystemApp(it.packageName) }
            .sortedByDescending { it.totalTimeInForeground }
            .map { s ->
                AppUsageInfo(
                    packageName          = s.packageName,
                    appName              = resolveAppName(s.packageName),
                    totalTimeForegroundMs = s.totalTimeInForeground,
                    lastTimeUsed         = s.lastTimeUsed
                )
            }
    }

    private fun isSystemApp(packageName: String): Boolean {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdated = (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            isSystem && !isUpdated
        } catch (_: PackageManager.NameNotFoundException) { true }
    }

    private fun resolveAppName(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) { packageName }
    }
}
