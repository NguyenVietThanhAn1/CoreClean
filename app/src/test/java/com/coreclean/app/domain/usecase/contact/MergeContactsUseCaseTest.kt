package com.coreclean.app.domain.usecase.contact

import android.content.ContentResolver
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.coreclean.app.domain.model.Contact
import android.database.MatrixCursor
import android.provider.ContactsContract

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class MergeContactsUseCaseTest {

    private lateinit var contentResolver: ContentResolver
    private lateinit var useCase: MergeContactsUseCase

    @Before
    fun setUp() {
        contentResolver = mockk(relaxed = true)
        useCase         = MergeContactsUseCase(contentResolver)
    }

    @Test
    fun `merge with fewer than 2 contacts does nothing`() = runTest {
        val result = useCase.invoke(listOf(
            Contact(1L, "Alice", listOf("0901234567"), emptyList(), false)
        ))
        assertTrue(result.isSuccess)
        verify(exactly = 0) { contentResolver.applyBatch(any(), any()) }
    }

    @Test
    fun `merge with 2 contacts with no raw contacts succeeds silently`() = runTest {
        // Raw contacts cursor returns empty — no AggregationException ops needed
        val rawCursor = MatrixCursor(arrayOf(ContactsContract.RawContacts._ID))
        every { contentResolver.query(ContactsContract.RawContacts.CONTENT_URI, any(), any(), any(), any()) } returns rawCursor

        val contacts = listOf(
            Contact(1L, "Alice", emptyList(), emptyList(), false),
            Contact(2L, "Bob",   emptyList(), emptyList(), false)
        )
        val result = useCase.invoke(contacts)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `merge calls applyBatch when raw contacts exist`() = runTest {
        // selectionArgs is position 3 — match on array content
        every {
            contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI, any(), any(),
                match { args -> args?.firstOrNull() == "1" }, any()
            )
        } returns buildRawCursor(101L)
        every {
            contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI, any(), any(),
                match { args -> args?.firstOrNull() == "2" }, any()
            )
        } returns buildRawCursor(102L)

        val contacts = listOf(
            Contact(1L, "Alice", emptyList(), emptyList(), false),
            Contact(2L, "Bob",   emptyList(), emptyList(), false)
        )
        val result = useCase.invoke(contacts)
        assertTrue(result.isSuccess)
        verify(exactly = 1) { contentResolver.applyBatch(ContactsContract.AUTHORITY, any()) }
    }

    private fun buildRawCursor(rawId: Long): android.database.Cursor {
        val cursor = MatrixCursor(arrayOf(ContactsContract.RawContacts._ID))
        cursor.addRow(arrayOf(rawId))
        return cursor
    }
}
