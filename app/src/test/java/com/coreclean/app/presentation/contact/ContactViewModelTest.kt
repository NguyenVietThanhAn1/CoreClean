package com.coreclean.app.presentation.contact

import com.coreclean.app.MainDispatcherRule
import com.coreclean.app.R
import com.coreclean.app.domain.CrashReporter
import com.coreclean.app.domain.model.Contact
import com.coreclean.app.domain.repository.ContactRepository
import com.coreclean.app.domain.usecase.contact.MergeContactsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [33])
class ContactViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository     = mockk<ContactRepository>()
    private val mergeContacts  = mockk<MergeContactsUseCase>()
    private val crashReporter  = mockk<CrashReporter>(relaxed = true)

    private val contact1 = Contact(id = 1, displayName = "A", phones = listOf("111"), emails = emptyList(), hasPhoto = false)
    private val contact2 = Contact(id = 2, displayName = "A", phones = listOf("111"), emails = emptyList(), hasPhoto = false)

    @Before
    fun setUp() {
        shadowOf(RuntimeEnvironment.getApplication())
            .grantPermissions(android.Manifest.permission.READ_CONTACTS)

        coEvery { repository.getAllContacts() } returns listOf(contact1, contact2)
        coEvery { repository.findDuplicates() } returns emptyList()
        coEvery { repository.findIncomplete() } returns emptyList()
    }

    private fun createViewModel() = ContactViewModel(
        context         = RuntimeEnvironment.getApplication(),
        repository      = repository,
        mergeContacts   = mergeContacts,
        crashReporter   = crashReporter
    )

    @Test
    fun `confirmMerge success sets contact_merge_success messageRes with merged count`() =
        runTest(mainDispatcherRule.testScheduler) {
            coEvery { mergeContacts.invoke(any()) } returns Result.success(Unit)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.confirmMerge(listOf(contact1, contact2))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(R.string.contact_merge_success, state.messageRes)
            assertEquals(2, state.mergedCount)
        }

    @Test
    fun `confirmMerge failure sets contact_merge_error messageRes and reports the exception`() =
        runTest(mainDispatcherRule.testScheduler) {
            val exception = RuntimeException("boom")
            coEvery { mergeContacts.invoke(any()) } returns Result.failure(exception)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.confirmMerge(listOf(contact1, contact2))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(R.string.contact_merge_error, state.messageRes)
            verify(exactly = 1) { crashReporter.captureException(exception) }
        }

    @Test
    fun `load without READ_CONTACTS permission sets hasPermission false and stops loading`() =
        runTest(mainDispatcherRule.testScheduler) {
            shadowOf(RuntimeEnvironment.getApplication())
                .denyPermissions(android.Manifest.permission.READ_CONTACTS)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(false, state.isLoading)
            assertEquals(false, state.hasPermission)
        }

    @Test
    fun `dismissMergeMessage clears messageRes`() = runTest(mainDispatcherRule.testScheduler) {
        coEvery { mergeContacts.invoke(any()) } returns Result.success(Unit)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.confirmMerge(listOf(contact1, contact2))
        advanceUntilIdle()
        assertEquals(R.string.contact_merge_success, viewModel.uiState.value.messageRes)

        viewModel.dismissMergeMessage()
        assertEquals(null, viewModel.uiState.value.messageRes)
    }
}
