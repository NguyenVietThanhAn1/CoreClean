package com.coreclean.app

import com.coreclean.app.data.local.dao.BatteryHistoryDao
import com.coreclean.app.data.local.entity.BatteryHistoryEntity
import com.coreclean.app.domain.usecase.battery.PredictBatteryRemainingUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PredictBatteryRemainingUseCaseTest {

    private lateinit var dao: BatteryHistoryDao
    private lateinit var useCase: PredictBatteryRemainingUseCase

    @Before fun setUp() {
        dao     = mockk()
        useCase = PredictBatteryRemainingUseCase(dao)
    }

    @Test fun `returns null duration when charging`() = runTest {
        val result = useCase(80, isCharging = true)
        assertTrue(result.isCharging)
        assertNull(result.estimated)
    }

    @Test fun `returns null duration when fewer than 4 samples`() = runTest {
        coEvery { dao.getDischargingSince(any()) } returns listOf(
            makeSample(100, System.currentTimeMillis() - 3_600_000L, false),
            makeSample(95,  System.currentTimeMillis() - 1_800_000L, false),
        )
        val result = useCase(90, isCharging = false)
        assertNull(result.estimated)
        assertEquals(2, result.sampleCount)
    }

    @Test fun `returns estimate when enough samples with steady drain`() = runTest {
        val now = System.currentTimeMillis()
        val intervalMs = 15 * 60 * 1000L  // 15 min
        // Simulate draining 1% per 15 min → 100% takes 100 * 15 min = 25 hours
        val samples = (0 until 8).map { i ->
            makeSample(100 - i, now - (7 - i) * intervalMs, false)
        }
        coEvery { dao.getDischargingSince(any()) } returns samples

        val result = useCase(92, isCharging = false)
        assertNotNull(result.estimated)
        assertTrue(result.estimated!!.toHours() > 0)
    }

    @Test fun `returns null duration when drain rate is zero`() = runTest {
        val now = System.currentTimeMillis()
        // All samples at same level — no drain
        val samples = (0 until 5).map { i ->
            makeSample(80, now - i * 60_000L, false)
        }
        coEvery { dao.getDischargingSince(any()) } returns samples
        val result = useCase(80, isCharging = false)
        assertNull(result.estimated)
    }

    private fun makeSample(level: Int, ts: Long, charging: Boolean) =
        BatteryHistoryEntity(timestamp = ts, levelPercent = level, isCharging = charging)
}
