package com.coreclean.app.domain.model

data class ScanResult(
    val filePath: String,
    val fileSize: Long,
    val fileType: String,
    val lastModified: Long
)
