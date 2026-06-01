package com.coreclean.app.domain.model

data class BatteryInfo(
    val levelPercent:    Int,
    val status:          Int,
    val healthLabel:     String,
    val temperatureC:    Float,
    val voltageMv:       Int,
    val technology:      String,
    val chargeCounterMah: Int,
    val isCharging:      Boolean,
    val chargePlug:      String
)
