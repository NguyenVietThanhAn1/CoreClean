package com.coreclean.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class CleaningSuggestion {
    abstract val estimatedWastedBytes: Long

    @Serializable @SerialName("large_duplicate_group")
    data class LargeDuplicateGroup(
        val groupCount: Int,
        val wastedBytes: Long
    ) : CleaningSuggestion() {
        override val estimatedWastedBytes: Long get() = wastedBytes
    }

    @Serializable @SerialName("unused_app")
    data class UnusedApp(
        val packageName: String,
        val appName: String,
        val daysUnused: Int,
        val sizeBytes: Long
    ) : CleaningSuggestion() {
        override val estimatedWastedBytes: Long get() = sizeBytes
    }

    @Serializable @SerialName("oversized_download")
    data class OversizedDownload(
        val items: List<JunkItem>
    ) : CleaningSuggestion() {
        override val estimatedWastedBytes: Long get() = items.sumOf { it.sizeBytes }
    }

    @Serializable @SerialName("stale_screenshot")
    data class StaleScreenshot(
        val olderThanDays: Int,
        val count: Int,
        val totalBytes: Long
    ) : CleaningSuggestion() {
        override val estimatedWastedBytes: Long get() = totalBytes
    }

    @Serializable @SerialName("storage_full")
    data class StorageFull(
        val freePercent: Float
    ) : CleaningSuggestion() {
        override val estimatedWastedBytes: Long get() = 0L
    }
}
