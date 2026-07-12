package com.coreclean.app.domain.model

enum class BatteryHealth { GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, COLD, UNKNOWN }
enum class ChargePlug    { AC, USB, WIRELESS, OTHER, NONE }

data class BatteryInfo(
    val levelPercent:     Int,
    val status:           Int,
    val healthCode:       BatteryHealth,
    val temperatureC:     Float,
    val voltageMv:        Int,
    val technology:       String,
    val chargeCounterMah: Int,
    val isCharging:       Boolean,
    val chargePlugCode:   ChargePlug
)
