package com.coreclean.app.presentation.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.work.WorkManager
import com.coreclean.app.MainDispatcherRule
import com.coreclean.app.core.preferences.ThemeMode
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var viewModel: SettingsViewModel
    private val workManager: WorkManager = mockk(relaxed = true)

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(
            scope       = kotlinx.coroutines.CoroutineScope(mainDispatcherRule.dispatcher),
            produceFile = { tmpFolder.newFile("test_prefs.preferences_pb") }
        )
        viewModel = SettingsViewModel(dataStore, workManager)
    }

    @Test
    fun `default state has SYSTEM theme`() = runTest(mainDispatcherRule.testScheduler) {
        // Subscribe to activate the WhileSubscribed flow, then wait for initial emission
        val sub = launch { viewModel.state.collect {} }
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, viewModel.state.value.themeMode)
        sub.cancel()
    }

    @Test
    fun `setThemeMode persists DARK theme`() = runTest(mainDispatcherRule.testScheduler) {
        // Subscribe to keep the stateIn flow active
        val sub = launch { viewModel.state.collect {} }
        advanceUntilIdle() // settle initial state

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle() // let the DataStore edit + flow re-emit
        // Give real-thread IO a moment to flush (DataStore uses Dispatchers.IO internally)
        kotlinx.coroutines.delay(100)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, viewModel.state.value.themeMode)
        sub.cancel()
    }

    @Test
    fun `setDynamicColor persists false`() = runTest(mainDispatcherRule.testScheduler) {
        val sub = launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.setDynamicColor(false)
        advanceUntilIdle()
        kotlinx.coroutines.delay(100)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.dynamicColor)
        sub.cancel()
    }

    @Test
    fun `setBackgroundScan to false disables scan`() = runTest(mainDispatcherRule.testScheduler) {
        val sub = launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.setBackgroundScan(false)
        advanceUntilIdle()
        kotlinx.coroutines.delay(100)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.backgroundScan)
        sub.cancel()
    }
}
