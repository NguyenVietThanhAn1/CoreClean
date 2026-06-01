package com.coreclean.app.domain.model

data class RamInfo(
    val totalMb: Long,
    val availableMb: Long,
    val usedMb: Long,
    val threshold: Long,
    val isLowMemory: Boolean,
    val topApps: List<RunningAppInfo> = emptyList()
)

data class RunningAppInfo(
    val packageName: String,
    val appName: String,
    val memoryMb: Long
)
