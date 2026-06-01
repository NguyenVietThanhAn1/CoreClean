package com.coreclean.app.data.repository

import com.coreclean.app.MainDispatcherRule
import com.coreclean.app.data.datasource.contact.ContactDataSource
import com.coreclean.app.domain.model.Contact
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class ContactRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dataSource: ContactDataSource
    private lateinit var repository: ContactRepositoryImpl

    @Before
    fun setUp() {
        dataSource = mockk()
        repository = ContactRepositoryImpl(dataSource)
    }

    @Test
    fun `findDuplicates groups contacts with same normalized phone`() = runTest {
        val contacts = listOf(
            Contact(1L, "Alice",  listOf("0901234567"),  emptyList(), false),
            Contact(2L, "Alice2", listOf("+84901234567"), emptyList(), false), // same number E.164
            Contact(3L, "Bob",    listOf("0912345678"),  emptyList(), false)
        )
        coEvery { dataSource.getAllContacts() } returns contacts

        val duplicates = repository.findDuplicates()

        // Alice and Alice2 share the same last-10 digits → one duplicate group
        assertTrue("Expected at least 1 duplicate group", duplicates.isNotEmpty())
        val group = duplicates.first()
        assertEquals(2, group.contacts.size)
    }

    @Test
    fun `findIncomplete returns contacts missing phone`() = runTest {
        val contacts = listOf(
            Contact(1L, "Alice",  listOf("0901234567"), emptyList(), false),
            Contact(2L, "NoPhone", emptyList(), emptyList(), false)
        )
        coEvery { dataSource.getAllContacts() } returns contacts

        val incomplete = repository.findIncomplete()

        assertEquals(1, incomplete.size)
        assertEquals("NoPhone", incomplete.first().displayName)
    }

    @Test
    fun `findDuplicates groups contacts with same display name`() = runTest {
        val contacts = listOf(
            Contact(1L, "Duplicate", listOf("0901111111"), emptyList(), false),
            Contact(2L, "Duplicate", listOf("0902222222"), emptyList(), false),
            Contact(3L, "Unique",    listOf("0903333333"), emptyList(), false)
        )
        coEvery { dataSource.getAllContacts() } returns contacts

        val duplicates = repository.findDuplicates()

        assertTrue(duplicates.any { g -> g.contacts.size == 2 && g.contacts.all { it.displayName == "Duplicate" } })
    }
}
