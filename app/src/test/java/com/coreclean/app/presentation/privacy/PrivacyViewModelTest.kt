package com.coreclean.app.presentation.privacy

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.coreclean.app.MainDispatcherRule
import com.coreclean.app.R
import com.coreclean.app.domain.repository.AppUsageRepository
import com.coreclean.app.domain.repository.BatteryRepository
import com.coreclean.app.domain.repository.MediaRepository
import com.coreclean.app.domain.repository.ScanResultRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [33])
class PrivacyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>

    private val scanResultRepository = mockk<ScanResultRepository>()
    private val mediaRepository      = mockk<MediaRepository>()
    private val batteryRepository    = mockk<BatteryRepository>()
    private val appUsageRepository   = mockk<AppUsageRepository>()

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(
            scope       = kotlinx.coroutines.CoroutineScope(mainDispatcherRule.dispatcher),
            produceFile = { tmpFolder.newFile("privacy_test_prefs.preferences_pb") }
        )
        coEvery { scanResultRepository.getCount() }         returns 3
        coEvery { scanResultRepository.clearAll() }         returns Unit
        coEvery { mediaRepository.getPendingReviewCount() } returns 2
        coEvery { mediaRepository.clearPendingReviewIds() } returns Unit
        coEvery { batteryRepository.getHistoryCount() }     returns 40
        coEvery { batteryRepository.clearHistory() }        returns Unit
        every  { appUsageRepository.hasUsageAccessPermission() } returns false
    }

    private fun createViewModel() = PrivacyViewModel(
        context                = RuntimeEnvironment.getApplication(),
        dataStore               = dataStore,
        scanResultRepository     = scanResultRepository,
        mediaRepository          = mediaRepository,
        batteryRepository        = batteryRepository,
        appUsageRepository       = appUsageRepository
    )

    @Test
    fun `load populates counts from all three repositories`() = runTest(mainDispatcherRule.testScheduler) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(false, state.isLoading)
        assertEquals(3, state.scanResultCount)
        assertEquals(2, state.pendingReviewCount)
        assertEquals(40, state.batteryHistoryCount)
    }

    @Test
    fun `clearHistory clears scan and pending review via repositories and resets counts`() =
        runTest(mainDispatcherRule.testScheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.clearHistory()
            advanceUntilIdle()

            coVerify(exactly = 1) { scanResultRepository.clearAll() }
            coVerify(exactly = 1) { mediaRepository.clearPendingReviewIds() }
            val state = viewModel.state.value
            assertEquals(0, state.scanResultCount)
            assertEquals(0, state.pendingReviewCount)
            assertEquals(R.string.privacy_cleared_scan_history, state.messageRes)
        }

    @Test
    fun `clearBatteryHistory clears battery history via repository and resets count`() =
        runTest(mainDispatcherRule.testScheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.clearBatteryHistory()
            advanceUntilIdle()

            coVerify(exactly = 1) { batteryRepository.clearHistory() }
            val state = viewModel.state.value
            assertEquals(0, state.batteryHistoryCount)
            assertEquals(R.string.privacy_cleared_battery_history, state.messageRes)
        }

    @Test
    fun `dismissMessage clears the message`() = runTest(mainDispatcherRule.testScheduler) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.clearBatteryHistory()
        advanceUntilIdle()
        assertEquals(R.string.privacy_cleared_battery_history, viewModel.state.value.messageRes)

        viewModel.dismissMessage()
        assertEquals(null, viewModel.state.value.messageRes)
    }
}
