package com.coreclean.app.presentation.media

import app.cash.turbine.test
import com.coreclean.app.MainDispatcherRule
import com.coreclean.app.data.local.dao.PendingReviewDao
import com.coreclean.app.domain.model.DuplicateGroup
import com.coreclean.app.domain.model.MediaImage
import com.coreclean.app.domain.usecase.media.FindDuplicateImagesUseCase
import com.coreclean.app.domain.usecase.media.GetAllImagesUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getAllImagesUseCase: GetAllImagesUseCase
    private lateinit var findDuplicateImagesUseCase: FindDuplicateImagesUseCase
    private lateinit var pendingReviewDao: PendingReviewDao
    private lateinit var viewModel: MediaViewModel

    @Before
    fun setUp() {
        getAllImagesUseCase     = mockk()
        findDuplicateImagesUseCase = mockk()
        pendingReviewDao        = mockk(relaxed = true)
        viewModel = MediaViewModel(getAllImagesUseCase, findDuplicateImagesUseCase, pendingReviewDao)
    }

    /**
     * Tests that [MediaViewModel.loadImages] transitions through Loading and ends in Success.
     *
     * Uses a suspending flow (delay before emit) so that the Loading state is observable
     * before Success — avoiding StateFlow conflation of the two rapid state changes.
     */
    @Test
    fun `loadImages transitions Loading then Success`() = runTest(mainDispatcherRule.testScheduler) {
        val images = listOf(fakeImage(1L), fakeImage(2L))
        // Suspend once before emitting, so Loading state can be observed via Turbine
        every { getAllImagesUseCase() } returns flow {
            delay(1)
            emit(images)
        }

        viewModel.uiState.test {
            assertTrue(awaitItem() is MediaUiState.Idle)       // initial state

            viewModel.loadImages()
            advanceUntilIdle()   // runs until delay(1) suspends the collecting coroutine

            // Loading should be visible before the delay resolves
            // (advanceUntilIdle drains all currently-schedulable work, leaving 'delay(1)' pending)
            // Actually, advanceUntilIdle advances virtual time too. Let us just collect items:
            val item1 = awaitItem()
            val item2 = awaitItem()

            assertTrue(
                "Expected Loading then Success, got: $item1, $item2",
                item1 is MediaUiState.Loading && item2 is MediaUiState.Success
            )
            assertTrue((item2 as MediaUiState.Success).images.size == 2)

            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Tests that [MediaViewModel.findDuplicates] sets the scanning flag to true while running
     * and back to false when done.
     *
     * Uses a suspending mock (delay inside) so the `true` flag is observable before it flips back.
     */
    @Test
    fun `findDuplicates sets isScanningDuplicates true then false`() =
        runTest(mainDispatcherRule.testScheduler) {
            val images = listOf(fakeImage(1L))
            every { getAllImagesUseCase() } returns flowOf(images)
            // Suspend inside findDuplicates so isScanningDuplicates=true is observable
            coEvery { findDuplicateImagesUseCase(any()) } coAnswers {
                delay(1)
                emptyList<DuplicateGroup>()
            }

            // Bring the ViewModel to Success state
            viewModel.loadImages()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value is MediaUiState.Success)

            viewModel.isScanningDuplicates.test {
                assertFalse(awaitItem())   // initial: not scanning

                viewModel.findDuplicates()
                assertTrue(awaitItem())    // scanning = true (before delay(1) resolves)

                advanceUntilIdle()         // finish the delay, complete the scan
                assertFalse(awaitItem())   // scanning = false

                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun fakeImage(id: Long) = MediaImage(
        id        = id,
        uri       = mockk(),
        name      = "img_$id.jpg",
        path      = "/storage/img_$id.jpg",
        size      = 1024L,
        dateAdded = 0L,
        mimeType  = "image/jpeg"
    )
}
