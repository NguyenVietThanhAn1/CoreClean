package com.coreclean.app.presentation.junk

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.coreclean.app.MainDispatcherRule
import com.coreclean.app.domain.CrashReporter
import com.coreclean.app.domain.usecase.junk.CleanJunkUseCase
import com.coreclean.app.domain.usecase.junk.ScanJunkUseCase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [33])
class JunkViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>

    private val scanJunk    = mockk<ScanJunkUseCase>()
    private val cleanJunk   = mockk<CleanJunkUseCase>()
    private val crashReporter = mockk<CrashReporter>(relaxed = true)

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(
            scope       = kotlinx.coroutines.CoroutineScope(mainDispatcherRule.dispatcher),
            produceFile = { tmpFolder.newFile("junk_test_prefs.preferences_pb") }
        )
    }

    private fun createViewModel() = JunkViewModel(
        scanJunk      = scanJunk,
        cleanJunk     = cleanJunk,
        dataStore     = dataStore,
        crashReporter = crashReporter
    )

    @Test
    fun `scan failure reports the exception to crashReporter and sets Error state`() =
        runTest(mainDispatcherRule.testScheduler) {
            val exception = RuntimeException("scan failed")
            coEvery { scanJunk(any()) } throws exception

            val viewModel = createViewModel()
            viewModel.scan()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is JunkUiState.Error)
            assertEquals("scan failed", (state as JunkUiState.Error).message)
            verify(exactly = 1) { crashReporter.captureException(exception) }
        }

    @Test
    fun `scan success does not report to crashReporter`() = runTest(mainDispatcherRule.testScheduler) {
        coEvery { scanJunk(any()) } returns emptyList()

        val viewModel = createViewModel()
        viewModel.scan()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is JunkUiState.Ready)
        verify(exactly = 0) { crashReporter.captureException(any()) }
    }
}
