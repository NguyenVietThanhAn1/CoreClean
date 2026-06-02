package com.coreclean.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class JunkItem(
    val path: String,
    val sizeBytes: Long,
    val category: JunkCategory,
    val packageName: String? = null
)
