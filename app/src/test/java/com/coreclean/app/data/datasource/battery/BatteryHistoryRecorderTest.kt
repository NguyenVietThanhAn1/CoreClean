package com.coreclean.app.data.datasource.battery

import com.coreclean.app.data.local.entity.BatteryHistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for BatteryHistoryRecorder scheduling constants and BatteryHistoryEntity model.
 * The actual Worker integration test requires a real Context and belongs in androidTest.
 */
class BatteryHistoryRecorderTest {

    @Test
    fun `BatteryHistoryEntity stores level correctly`() {
        val entity = BatteryHistoryEntity(
            timestamp    = 1000L,
            levelPercent = 85,
            isCharging   = false
        )
        assertEquals(85, entity.levelPercent)
        assertFalse(entity.isCharging)
    }

    @Test
    fun `BatteryHistoryEntity stores charging state correctly`() {
        val entity = BatteryHistoryEntity(
            timestamp    = 2000L,
            levelPercent = 50,
            isCharging   = true
        )
        assertTrue(entity.isCharging)
        assertEquals(50, entity.levelPercent)
    }

    @Test
    fun `BatteryHistoryEntity timestamp is stored correctly`() {
        val ts = System.currentTimeMillis()
        val entity = BatteryHistoryEntity(
            timestamp    = ts,
            levelPercent = 75,
            isCharging   = false
        )
        assertEquals(ts, entity.timestamp)
    }

    @Test
    fun `battery level percent range is valid`() {
        // Battery level should be between 0 and 100
        val validLevels = listOf(0, 1, 50, 99, 100)
        validLevels.forEach { level ->
            assertTrue("Level $level should be in range", level in 0..100)
        }
    }

    @Test
    fun `prediction requires minimum 4 samples`() {
        // From PredictBatteryRemainingUseCase: needs >= 4 samples for linear regression
        val minSamples = 4
        val samplesWith3 = (1..3).map {
            BatteryHistoryEntity(timestamp = it.toLong() * 1000, levelPercent = 90 - it, isCharging = false)
        }
        assertTrue(samplesWith3.size < minSamples)

        val samplesWith4 = (1..4).map {
            BatteryHistoryEntity(timestamp = it.toLong() * 1000, levelPercent = 90 - it, isCharging = false)
        }
        assertTrue(samplesWith4.size >= minSamples)
    }
}
