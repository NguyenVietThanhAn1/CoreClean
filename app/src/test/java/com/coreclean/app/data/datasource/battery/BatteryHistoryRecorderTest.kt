package com.coreclean.app.data.datasource.battery

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.coreclean.app.domain.model.BatteryHealth
import com.coreclean.app.domain.model.BatteryInfo
import com.coreclean.app.domain.model.BatteryHistoryEntry
import com.coreclean.app.domain.model.ChargePlug
import com.coreclean.app.domain.repository.BatteryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class BatteryHistoryRecorderTest {

    private lateinit var context: Context
    private lateinit var batteryDataSource: BatteryDataSource
    private lateinit var batteryRepository: BatteryRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        batteryDataSource = mockk()
        batteryRepository = mockk()
    }

    @Test
    fun `doWork returns success and records history with current battery level`() = runBlocking {
        val fakeBattery = BatteryInfo(
            levelPercent = 63, status = 0,
            healthCode = BatteryHealth.GOOD, temperatureC = 27f, voltageMv = 3900,
            technology = "Li-ion", chargeCounterMah = 1800,
            isCharging = false, chargePlugCode = ChargePlug.NONE
        )
        every { batteryDataSource.getBatteryInfo() } returns fakeBattery
        coEvery { batteryRepository.recordHistory(any()) } returns Unit

        val worker = TestListenableWorkerBuilder<BatteryHistoryRecorder>(context)
            .setWorkerFactory(FakeWorkerFactory(batteryDataSource, batteryRepository))
            .build()

        val result = worker.startWork().get()
        assertEquals(Result.success(), result)

        coVerify(exactly = 1) {
            batteryRepository.recordHistory(
                match { it.levelPercent == 63 && !it.isCharging }
            )
        }
    }

    @Test
    fun `doWork returns failure when repository throws`() = runBlocking {
        val fakeBattery = BatteryInfo(
            levelPercent = 10, status = 0,
            healthCode = BatteryHealth.GOOD, temperatureC = 27f, voltageMv = 3900,
            technology = "Li-ion", chargeCounterMah = 1800,
            isCharging = false, chargePlugCode = ChargePlug.NONE
        )
        every { batteryDataSource.getBatteryInfo() } returns fakeBattery
        coEvery { batteryRepository.recordHistory(any<BatteryHistoryEntry>()) } throws RuntimeException("db error")

        val worker = TestListenableWorkerBuilder<BatteryHistoryRecorder>(context)
            .setWorkerFactory(FakeWorkerFactory(batteryDataSource, batteryRepository))
            .build()

        val result = worker.startWork().get()
        assertEquals(Result.failure(), result)
    }

    @Test
    fun `doWork returns failure when reading battery info throws`() = runBlocking {
        every { batteryDataSource.getBatteryInfo() } throws IllegalStateException("no receiver")

        val worker = TestListenableWorkerBuilder<BatteryHistoryRecorder>(context)
            .setWorkerFactory(FakeWorkerFactory(batteryDataSource, batteryRepository))
            .build()

        val result = worker.startWork().get()
        assertEquals(Result.failure(), result)
        coVerify(exactly = 0) { batteryRepository.recordHistory(any()) }
    }
}

/** Minimal WorkerFactory that injects test doubles into [BatteryHistoryRecorder]. */
private class FakeWorkerFactory(
    private val dataSource: BatteryDataSource,
    private val repository: BatteryRepository
) : androidx.work.WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: androidx.work.WorkerParameters
    ): androidx.work.ListenableWorker = BatteryHistoryRecorder(appContext, workerParameters, dataSource, repository)
}
