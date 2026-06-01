package com.coreclean.app.data.datasource.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.coreclean.app.domain.model.BatteryInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class BatteryDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Returns a one-shot snapshot of current battery state via sticky broadcast. */
    fun getBatteryInfo(): BatteryInfo {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return BatteryInfo(0, 0, "Khong xac dinh", 0f, 0, "Unknown", 0, false, "Khong sac")
        return intent.toBatteryInfo(context)
    }

    /**
     * Returns a cold Flow that emits [BatteryInfo] on each battery change.
     * ACTION_BATTERY_CHANGED is a sticky broadcast, so the first emission
     * arrives immediately upon receiver registration.
     */
    fun observe(): Flow<BatteryInfo> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                trySend(intent.toBatteryInfo(context))
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        awaitClose { context.unregisterReceiver(receiver) }
    }
}

internal fun Intent.toBatteryInfo(context: Context): BatteryInfo {
    val level  = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale  = getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    val levelPercent = if (scale > 0) (level * 100f / scale).roundToInt() else level

    val status = getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

    val health = getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
    val healthLabel = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD          -> "Tot"
        BatteryManager.BATTERY_HEALTH_OVERHEAT      -> "Qua nong"
        BatteryManager.BATTERY_HEALTH_DEAD          -> "Hong"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE  -> "Dien ap cao"
        BatteryManager.BATTERY_HEALTH_COLD          -> "Qua lanh"
        else                                        -> "Khong xac dinh"
    }

    val temperatureC = getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
    val voltageMv    = getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
    val technology   = getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

    val plugged = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
    val chargePlug = when (plugged) {
        BatteryManager.BATTERY_PLUGGED_AC       -> "AC"
        BatteryManager.BATTERY_PLUGGED_USB      -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Khong day"
        else -> if (isCharging) "Khac" else "Khong sac"
    }

    val batteryManager = context.getSystemService(BatteryManager::class.java)
    val chargeCounterMah = batteryManager
        .getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        .let { if (it > 0) it / 1000 else 0 }

    return BatteryInfo(
        levelPercent     = levelPercent,
        status           = status,
        healthLabel      = healthLabel,
        temperatureC     = temperatureC,
        voltageMv        = voltageMv,
        technology       = technology,
        chargeCounterMah = chargeCounterMah,
        isCharging       = isCharging,
        chargePlug       = chargePlug
    )
}
