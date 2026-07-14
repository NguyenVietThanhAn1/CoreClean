package com.coreclean.app.presentation.usage

import com.coreclean.app.MainDispatcherRule
import com.coreclean.app.domain.CrashReporter
import com.coreclean.app.domain.model.UsageRange
import com.coreclean.app.domain.repository.AppUsageRepository
import com.coreclean.app.domain.usecase.usage.GetAppUsageUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppUsageViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getAppUsage = mockk<GetAppUsageUseCase>()
    private val repository  = mockk<AppUsageRepository>()
    private val crashReporter = mockk<CrashReporter>(relaxed = true)

    private fun createViewModel() = AppUsageViewModel(
        getAppUsage   = getAppUsage,
        repository    = repository,
        crashReporter = crashReporter
    )

    @Test
    fun `load failure reports the exception to crashReporter and sets Error state`() =
        runTest(mainDispatcherRule.testScheduler) {
            every { repository.hasUsageAccessPermission() } returns true
            val exception = RuntimeException("usage stats unavailable")
            coEvery { getAppUsage(UsageRange.LAST_7) } throws exception

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is AppUsageUiState.Error)
            assertEquals("usage stats unavailable", (state as AppUsageUiState.Error).message)
            verify(exactly = 1) { crashReporter.captureException(exception) }
        }

    @Test
    fun `load success does not report to crashReporter`() = runTest(mainDispatcherRule.testScheduler) {
        every { repository.hasUsageAccessPermission() } returns true
        coEvery { getAppUsage(UsageRange.LAST_7) } returns emptyList()

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AppUsageUiState.Success)
        verify(exactly = 0) { crashReporter.captureException(any()) }
    }
}
