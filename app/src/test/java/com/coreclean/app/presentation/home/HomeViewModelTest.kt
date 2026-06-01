package com.coreclean.app.presentation.home

import com.coreclean.app.MainDispatcherRule
import com.coreclean.app.domain.model.CleaningSuggestion
import com.coreclean.app.domain.model.StorageInfo
import com.coreclean.app.domain.repository.MediaRepository
import com.coreclean.app.domain.repository.StorageRepository
import com.coreclean.app.domain.usecase.suggest.GenerateSuggestionsUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mediaRepository = mockk<MediaRepository>()
    private val storageRepository = mockk<StorageRepository>()
    private val generateSuggestions = mockk<GenerateSuggestionsUseCase>()

    private fun createViewModel() = HomeViewModel(
        generateSuggestions = generateSuggestions,
        mediaRepository     = mediaRepository,
        storageRepository   = storageRepository
    )

    @Test
    fun `suggestions are initially empty`() = runTest(mainDispatcherRule.testScheduler) {
        every { mediaRepository.getAllImages() } returns flowOf(emptyList())
        coEvery { storageRepository.getStorageInfo() } returns StorageInfo(
            totalBytes = 0L, freeBytes = 0L, usedBytes = 0L, breakdown = emptyList()
        )
        coEvery { mediaRepository.findDuplicates(any()) } returns emptyList()
        // GenerateSuggestionsUseCase.invoke has 6 parameters (all have defaults)
        every { generateSuggestions.invoke(any(), any(), any(), any(), any(), any()) } returns emptyList()

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(emptyList<CleaningSuggestion>(), viewModel.suggestions.value)
    }

    @Test
    fun `suggestions are loaded on init`() = runTest(mainDispatcherRule.testScheduler) {
        val totalBytes = 32L * 1024 * 1024 * 1024
        val usedBytes  = 31L * 1024 * 1024 * 1024
        val expectedSuggestions = listOf(
            CleaningSuggestion.StorageFull(freePercent = 0.03f)
        )

        every { mediaRepository.getAllImages() } returns flowOf(emptyList())
        coEvery { storageRepository.getStorageInfo() } returns StorageInfo(
            totalBytes = totalBytes,
            freeBytes  = totalBytes - usedBytes,
            usedBytes  = usedBytes,
            breakdown  = emptyList()
        )
        coEvery { mediaRepository.findDuplicates(any()) } returns emptyList()
        every { generateSuggestions.invoke(any(), any(), any(), any(), any(), any()) } returns expectedSuggestions

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(expectedSuggestions, viewModel.suggestions.value)
    }

    @Test
    fun `loadSuggestions does not crash on repository error`() = runTest(mainDispatcherRule.testScheduler) {
        every { mediaRepository.getAllImages() } returns flowOf(emptyList())
        coEvery { storageRepository.getStorageInfo() } throws RuntimeException("Simulated error")

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Should not crash, suggestions stay empty
        assertEquals(emptyList<CleaningSuggestion>(), viewModel.suggestions.value)
    }
}
