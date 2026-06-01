package com.coreclean.app.presentation.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.work.WorkManager
import app.cash.turbine.test
import com.coreclean.app.MainDispatcherRule
import com.coreclean.app.core.preferences.ThemeMode
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        viewModel.state.test {
            val initial = awaitItem()
            assertEquals(ThemeMode.SYSTEM, initial.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setThemeMode persists DARK theme`() = runTest(mainDispatcherRule.testScheduler) {
        viewModel.state.test {
            awaitItem() // initial
            viewModel.setThemeMode(ThemeMode.DARK)
            val updated = awaitItem()
            assertEquals(ThemeMode.DARK, updated.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setDynamicColor persists false`() = runTest(mainDispatcherRule.testScheduler) {
        viewModel.state.test {
            awaitItem() // initial
            viewModel.setDynamicColor(false)
            val updated = awaitItem()
            assertFalse(updated.dynamicColor)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setBackgroundScan to false disables scan`() = runTest(mainDispatcherRule.testScheduler) {
        viewModel.state.test {
            awaitItem() // initial
            viewModel.setBackgroundScan(false)
            val updated = awaitItem()
            assertFalse(updated.backgroundScan)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
