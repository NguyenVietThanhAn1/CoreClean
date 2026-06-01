package com.coreclean.app.domain.model

sealed class CleaningSuggestion {
    abstract val estimatedWastedBytes: Long

    data class LargeDuplicateGroup(
        val groupCount: Int,
        val wastedBytes: Long
    ) : CleaningSuggestion() {
        override val estimatedWastedBytes: Long get() = wastedBytes
    }

    data class UnusedApp(
        val packageName: String,
        val appName: String,
        val daysUnused: Int,
        val sizeBytes: Long
    ) : CleaningSuggestion() {
        override val estimatedWastedBytes: Long get() = sizeBytes
    }

    data class OversizedDownload(
        val items: List<JunkItem>
    ) : CleaningSuggestion() {
        override val estimatedWastedBytes: Long get() = items.sumOf { it.sizeBytes }
    }

    data class StaleScreenshot(
        val olderThanDays: Int,
        val count: Int,
        val totalBytes: Long
    ) : CleaningSuggestion() {
        override val estimatedWastedBytes: Long get() = totalBytes
    }

    data class StorageFull(
        val freePercent: Float
    ) : CleaningSuggestion() {
        override val estimatedWastedBytes: Long get() = 0L
    }
}
