package com.coreclean.app.data.datasource.contact

import android.content.ContentResolver
import android.database.MatrixCursor
import android.provider.ContactsContract
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class ContactDataSourceTest {

    private lateinit var contentResolver: ContentResolver
    private lateinit var dataSource: ContactDataSource

    @Before
    fun setUp() {
        contentResolver = mockk(relaxed = true)
        dataSource      = ContactDataSource(contentResolver)
    }

    @Test
    fun `getAllContacts returns contacts with phones`() = runTest {
        // Phone cursor
        val phoneCursor = MatrixCursor(
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
        )
        phoneCursor.addRow(arrayOf(1L, "Alice", "+84901234567"))
        phoneCursor.addRow(arrayOf(2L, "Bob",   "+84912345678"))

        // Email cursor (empty)
        val emailCursor = MatrixCursor(
            arrayOf(
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.ADDRESS
            )
        )

        // Contacts cursor
        val contactsCursor = MatrixCursor(
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.PHOTO_ID
            )
        )
        contactsCursor.addRow(arrayOf(1L, "Alice", null))
        contactsCursor.addRow(arrayOf(2L, "Bob", null))

        every { contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, any(), any(), any(), any()) } returns phoneCursor
        every { contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, any(), any(), any(), any()) } returns emailCursor
        every { contentResolver.query(ContactsContract.Contacts.CONTENT_URI, any(), any(), any(), any()) } returns contactsCursor

        val contacts = dataSource.getAllContacts()

        assertEquals(2, contacts.size)
        val alice = contacts.first { it.displayName == "Alice" }
        assertTrue("+84901234567" in alice.phones)
    }

    @Test
    fun `contact with name but no phone or email is incomplete`() = runTest {
        val phoneCursor = MatrixCursor(
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
        )
        val emailCursor = MatrixCursor(
            arrayOf(
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.ADDRESS
            )
        )
        val contactsCursor = MatrixCursor(
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.PHOTO_ID
            )
        )
        contactsCursor.addRow(arrayOf(10L, "Incomplete Person", null))

        every { contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, any(), any(), any(), any()) } returns phoneCursor
        every { contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, any(), any(), any(), any()) } returns emailCursor
        every { contentResolver.query(ContactsContract.Contacts.CONTENT_URI, any(), any(), any(), any()) } returns contactsCursor

        val contacts = dataSource.getAllContacts()

        assertEquals(1, contacts.size)
        val contact = contacts.first()
        assertTrue(contact.phones.isEmpty())
        assertTrue(contact.emails.isEmpty())
    }
}
