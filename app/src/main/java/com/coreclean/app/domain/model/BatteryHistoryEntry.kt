package com.coreclean.app.domain.model

data class BatteryHistoryEntry(
    val timestamp: Long,
    val levelPercent: Int,
    val isCharging: Boolean
)
