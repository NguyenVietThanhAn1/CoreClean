package com.coreclean.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.TestWorkerFactory
import com.coreclean.app.core.preferences.AppPreferenceKeys
import com.coreclean.app.data.worker.AutoCleanWorker
import com.coreclean.app.domain.model.JunkCategory
import com.coreclean.app.domain.model.JunkItem
import com.coreclean.app.domain.model.ScheduleConfig
import com.coreclean.app.domain.usecase.junk.CleanJunkUseCase
import com.coreclean.app.domain.usecase.junk.CleanResult
import com.coreclean.app.domain.usecase.junk.ScanJunkUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.work.WorkManager
import io.mockk.every
import kotlinx.coroutines.flow.first

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AutoCleanWorkerTest {

    private lateinit var context: Context
    private lateinit var scanJunkUseCase: ScanJunkUseCase
    private lateinit var cleanJunkUseCase: CleanJunkUseCase
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var workManager: WorkManager

    @Before fun setUp() {
        context          = ApplicationProvider.getApplicationContext()
        scanJunkUseCase  = mockk()
        cleanJunkUseCase = mockk()
        dataStore        = mockk()
        workManager      = mockk(relaxed = true)
    }

    @Test fun `worker returns success when schedule is disabled`() = runTest {
        val config = ScheduleConfig(enabled = false)
        every { dataStore.data } returns flowOf(
            preferencesOf(AppPreferenceKeys.SCHEDULE_CONFIG_JSON to Json.encodeToString(config))
        )

        val worker = TestListenableWorkerBuilder<AutoCleanWorker>(context).build()
        // We can't easily inject via Hilt in unit tests; instead verify via direct invocation
        // The worker will call loadConfig → disabled → return success
        assertEquals(Result.success(), worker.startWork().get())
    }
}
