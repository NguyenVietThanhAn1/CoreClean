package com.coreclean.app.domain.model

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val apkSizeBytes: Long,
    val cacheBytes: Long,
    val dataBytes: Long,
    val installTime: Long,
    val updateTime: Long,
    val isSystem: Boolean
)
