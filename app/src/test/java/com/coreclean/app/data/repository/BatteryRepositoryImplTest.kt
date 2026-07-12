package com.coreclean.app.data.repository

import android.os.BatteryManager
import app.cash.turbine.test
import com.coreclean.app.MainDispatcherRule
import com.coreclean.app.data.datasource.battery.BatteryDataSource
import com.coreclean.app.data.local.dao.BatteryHistoryDao
import com.coreclean.app.data.local.entity.BatteryHistoryEntity
import com.coreclean.app.domain.model.BatteryHealth
import com.coreclean.app.domain.model.BatteryHistoryEntry
import com.coreclean.app.domain.model.BatteryInfo
import com.coreclean.app.domain.model.ChargePlug
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BatteryRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dataSource: BatteryDataSource
    private lateinit var batteryHistoryDao: BatteryHistoryDao
    private lateinit var repository: BatteryRepositoryImpl

    @Before
    fun setUp() {
        dataSource = mockk()
        batteryHistoryDao = mockk()
        repository = BatteryRepositoryImpl(dataSource, batteryHistoryDao)
    }

    @Test
    fun `observe delegates to dataSource`() = runTest(mainDispatcherRule.testScheduler) {
        val fakeBattery = BatteryInfo(
            levelPercent = 75, status = BatteryManager.BATTERY_STATUS_DISCHARGING,
            healthCode = BatteryHealth.GOOD, temperatureC = 28.5f, voltageMv = 3800,
            technology = "Li-ion", chargeCounterMah = 2000,
            isCharging = false, chargePlugCode = ChargePlug.NONE
        )
        every { dataSource.observe() } returns flowOf(fakeBattery)

        repository.observe().test {
            val item = awaitItem()
            assertEquals(75, item.levelPercent)
            assertFalse(item.isCharging)
            assertEquals("Li-ion", item.technology)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `charging status is true when status is CHARGING`() = runTest(mainDispatcherRule.testScheduler) {
        val charging = BatteryInfo(
            levelPercent = 50, status = BatteryManager.BATTERY_STATUS_CHARGING,
            healthCode = BatteryHealth.GOOD, temperatureC = 30f, voltageMv = 4000,
            technology = "Li-poly", chargeCounterMah = 1500,
            isCharging = true, chargePlugCode = ChargePlug.USB
        )
        every { dataSource.observe() } returns flowOf(charging)

        repository.observe().test {
            val item = awaitItem()
            assertTrue(item.isCharging)
            assertEquals(ChargePlug.USB, item.chargePlugCode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recordHistory maps domain entry to entity and inserts`() = runTest {
        coEvery { batteryHistoryDao.insert(any()) } returns Unit

        repository.recordHistory(BatteryHistoryEntry(timestamp = 1_000L, levelPercent = 42, isCharging = true))

        coVerify(exactly = 1) {
            batteryHistoryDao.insert(
                match { it.timestamp == 1_000L && it.levelPercent == 42 && it.isCharging }
            )
        }
    }

    @Test
    fun `getDischargingHistorySince maps entities to domain`() = runTest {
        coEvery { batteryHistoryDao.getDischargingSince(500L) } returns listOf(
            BatteryHistoryEntity(timestamp = 600L, levelPercent = 80, isCharging = false),
            BatteryHistoryEntity(timestamp = 700L, levelPercent = 78, isCharging = false)
        )

        val result = repository.getDischargingHistorySince(500L)

        assertEquals(2, result.size)
        assertEquals(BatteryHistoryEntry(600L, 80, false), result[0])
        assertEquals(BatteryHistoryEntry(700L, 78, false), result[1])
    }

    @Test
    fun `getAllHistorySince maps entities to domain`() = runTest {
        coEvery { batteryHistoryDao.getAllSince(0L) } returns listOf(
            BatteryHistoryEntity(timestamp = 100L, levelPercent = 90, isCharging = true)
        )

        val result = repository.getAllHistorySince(0L)

        assertEquals(1, result.size)
        assertEquals(BatteryHistoryEntry(100L, 90, true), result[0])
    }

    @Test
    fun `getHistoryCount delegates to dao`() = runTest {
        coEvery { batteryHistoryDao.count() } returns 7

        assertEquals(7, repository.getHistoryCount())
    }

    @Test
    fun `clearHistory delegates to dao`() = runTest {
        coEvery { batteryHistoryDao.clearAll() } returns Unit

        repository.clearHistory()

        coVerify(exactly = 1) { batteryHistoryDao.clearAll() }
    }
}
