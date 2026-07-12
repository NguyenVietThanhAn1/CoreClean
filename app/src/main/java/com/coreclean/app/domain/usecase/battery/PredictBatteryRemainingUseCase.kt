package com.coreclean.app.domain.usecase.battery

import com.coreclean.app.domain.model.BatteryHistoryEntry
import com.coreclean.app.domain.repository.BatteryRepository
import java.time.Duration
import javax.inject.Inject

private const val MIN_SAMPLES = 4
private const val WINDOW_HOURS = 24L

data class BatteryPrediction(
    val estimated: Duration?,    // null when data is insufficient
    val sampleCount: Int,
    val isCharging: Boolean
)

class PredictBatteryRemainingUseCase @Inject constructor(
    private val batteryRepository: BatteryRepository
) {
    /**
     * Estimates remaining battery time using simple linear regression on the last 24 h of
     * discharge samples. Returns null duration when fewer than [MIN_SAMPLES] points are
     * available — callers should show "collecting data" rather than a wrong estimate.
     *
     * Estimate is based on usage patterns and may differ from actual drain.
     */
    suspend operator fun invoke(currentLevelPercent: Int, isCharging: Boolean): BatteryPrediction {
        if (isCharging) return BatteryPrediction(null, 0, isCharging = true)

        val sinceMs  = System.currentTimeMillis() - WINDOW_HOURS * 60 * 60 * 1000
        val samples  = batteryRepository.getDischargingHistorySince(sinceMs)

        if (samples.size < MIN_SAMPLES) {
            return BatteryPrediction(null, samples.size, isCharging = false)
        }

        val drainRatePerMs = linearRegressionDrainRate(samples)
        if (drainRatePerMs <= 0.0) return BatteryPrediction(null, samples.size, isCharging = false)

        val msRemaining = (currentLevelPercent / drainRatePerMs).toLong().coerceAtMost(48 * 60 * 60 * 1000L)
        return BatteryPrediction(
            estimated    = Duration.ofMillis(msRemaining),
            sampleCount  = samples.size,
            isCharging   = false
        )
    }

    /**
     * Simple linear regression on (timestamp, level) pairs returns drain rate in %/ms.
     * Slope is negated so a positive value means battery is draining.
     */
    private fun linearRegressionDrainRate(samples: List<BatteryHistoryEntry>): Double {
        val n    = samples.size.toDouble()
        val xMid = samples.map { it.timestamp.toDouble() }.average()
        val yMid = samples.map { it.levelPercent.toDouble() }.average()

        var numerator   = 0.0
        var denominator = 0.0
        for (s in samples) {
            val dx = s.timestamp - xMid
            val dy = s.levelPercent - yMid
            numerator   += dx * dy
            denominator += dx * dx
        }
        if (denominator == 0.0) return 0.0
        val slope = numerator / denominator  // %/ms — negative when discharging
        return -slope                        // return positive drain rate
    }
}
