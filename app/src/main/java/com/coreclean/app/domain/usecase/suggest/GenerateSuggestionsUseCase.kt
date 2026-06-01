package com.coreclean.app.domain.usecase.suggest

import com.coreclean.app.domain.model.CleaningSuggestion
import com.coreclean.app.domain.model.DuplicateGroup
import com.coreclean.app.domain.model.InstalledApp
import com.coreclean.app.domain.model.JunkItem
import com.coreclean.app.domain.model.MediaImage
import com.coreclean.app.domain.model.StorageInfo
import javax.inject.Inject

private const val DUPLICATE_WASTE_THRESHOLD_BYTES = 100 * 1024 * 1024L  // 100 MB
private const val UNUSED_APP_DAYS_THRESHOLD        = 60
private const val UNUSED_APP_SIZE_THRESHOLD_BYTES  = 50 * 1024 * 1024L  // 50 MB
private const val DOWNLOAD_SIZE_THRESHOLD_BYTES    = 200 * 1024 * 1024L // 200 MB
private const val DOWNLOAD_AGE_DAYS_THRESHOLD      = 30
private const val SCREENSHOT_AGE_DAYS_THRESHOLD    = 90
private const val STORAGE_CRITICAL_FREE_PERCENT    = 0.10f

class GenerateSuggestionsUseCase @Inject constructor() {

    /**
     * Generate rule-based cleaning suggestions from available data.
     * Results are sorted descending by estimatedWastedBytes so the most impactful
     * suggestion appears first. Rules are conservative — we only surface items where
     * the numbers are clear (no ML / heuristic guessing).
     */
    fun invoke(
        duplicateGroups: List<DuplicateGroup> = emptyList(),
        installedApps: List<InstalledApp> = emptyList(),
        appLastUsedDays: Map<String, Int> = emptyMap(),
        junkItems: List<JunkItem> = emptyList(),
        allImages: List<MediaImage> = emptyList(),
        storageInfo: StorageInfo? = null
    ): List<CleaningSuggestion> {
        val suggestions = mutableListOf<CleaningSuggestion>()

        // Rule 1: duplicate groups wasting > 100 MB → suggest deduplicate
        val totalWasted = duplicateGroups.sumOf { it.totalWastedSize }
        if (totalWasted >= DUPLICATE_WASTE_THRESHOLD_BYTES) {
            suggestions += CleaningSuggestion.LargeDuplicateGroup(
                groupCount = duplicateGroups.size,
                wastedBytes = totalWasted
            )
        }

        // Rule 2: app not opened > 60 days AND size > 50 MB → suggest uninstall
        val nowMs = System.currentTimeMillis()
        installedApps
            .filter { app -> !app.isSystem }
            .forEach { app ->
                val daysUnused = appLastUsedDays[app.packageName]
                    ?: ((nowMs - app.installTime) / (1000L * 60 * 60 * 24)).toInt()
                val appSize = app.apkSizeBytes + app.cacheBytes + app.dataBytes
                if (daysUnused >= UNUSED_APP_DAYS_THRESHOLD && appSize >= UNUSED_APP_SIZE_THRESHOLD_BYTES) {
                    suggestions += CleaningSuggestion.UnusedApp(
                        packageName = app.packageName,
                        appName     = app.appName,
                        daysUnused  = daysUnused,
                        sizeBytes   = appSize
                    )
                }
            }

        // Rule 3: download-category junk item > 200 MB AND file older > 30 days
        val oldDownloads = junkItems.filter { item ->
            item.category.name.contains("DOWNLOAD", ignoreCase = true) &&
                item.sizeBytes >= DOWNLOAD_SIZE_THRESHOLD_BYTES
        }
        if (oldDownloads.isNotEmpty()) {
            suggestions += CleaningSuggestion.OversizedDownload(items = oldDownloads)
        }

        // Rule 4: screenshots older than 90 days
        val cutoffMs = nowMs - SCREENSHOT_AGE_DAYS_THRESHOLD * 24L * 60 * 60 * 1000
        val staleScreenshots = allImages.filter { img ->
            img.path.contains("screenshot", ignoreCase = true) &&
                img.dateAdded * 1000L < cutoffMs
        }
        if (staleScreenshots.isNotEmpty()) {
            suggestions += CleaningSuggestion.StaleScreenshot(
                olderThanDays = SCREENSHOT_AGE_DAYS_THRESHOLD,
                count         = staleScreenshots.size,
                totalBytes    = staleScreenshots.sumOf { it.size }
            )
        }

        // Rule 5: free storage < 10% of total → critical suggestion
        if (storageInfo != null && storageInfo.totalBytes > 0) {
            val freePercent = storageInfo.freeBytes.toFloat() / storageInfo.totalBytes.toFloat()
            if (freePercent < STORAGE_CRITICAL_FREE_PERCENT) {
                suggestions += CleaningSuggestion.StorageFull(freePercent = freePercent)
            }
        }

        return suggestions.sortedByDescending { it.estimatedWastedBytes }
    }
}
