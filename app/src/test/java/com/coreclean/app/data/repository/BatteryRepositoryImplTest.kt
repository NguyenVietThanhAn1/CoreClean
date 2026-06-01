package com.coreclean.app.data.repository

import android.content.Intent
import android.os.BatteryManager
import app.cash.turbine.test
import com.coreclean.app.MainDispatcherRule
import com.coreclean.app.data.datasource.battery.BatteryDataSource
import com.coreclean.app.data.datasource.battery.toBatteryInfo
import com.coreclean.app.domain.model.BatteryInfo
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
    private lateinit var repository: BatteryRepositoryImpl

    @Before
    fun setUp() {
        dataSource = mockk()
        repository = BatteryRepositoryImpl(dataSource)
    }

    @Test
    fun `observe delegates to dataSource`() = runTest(mainDispatcherRule.testScheduler) {
        val fakeBattery = BatteryInfo(
            levelPercent = 75, status = BatteryManager.BATTERY_STATUS_DISCHARGING,
            healthLabel = "Tot", temperatureC = 28.5f, voltageMv = 3800,
            technology = "Li-ion", chargeCounterMah = 2000,
            isCharging = false, chargePlug = "Khong sac"
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
            healthLabel = "Tot", temperatureC = 30f, voltageMv = 4000,
            technology = "Li-poly", chargeCounterMah = 1500,
            isCharging = true, chargePlug = "USB"
        )
        every { dataSource.observe() } returns flowOf(charging)

        repository.observe().test {
            val item = awaitItem()
            assertTrue(item.isCharging)
            assertEquals("USB", item.chargePlug)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
