package com.coreclean.app.domain.model

data class StorageInfo(
    val totalBytes: Long,
    val freeBytes:  Long,
    val usedBytes:  Long,
    val breakdown:  List<StorageCategory>
)

/** One segment in the storage breakdown donut chart. [colorArgb] is an ARGB int. */
data class StorageCategory(
    val name:      String,
    val sizeBytes: Long,
    val colorArgb: Int
)
