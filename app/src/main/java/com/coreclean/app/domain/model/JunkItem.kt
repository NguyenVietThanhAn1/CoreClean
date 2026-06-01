package com.coreclean.app.domain.model

data class JunkItem(
    val path: String,
    val sizeBytes: Long,
    val category: JunkCategory,
    val packageName: String? = null
)
