package com.coreclean.app.data.repository

import com.coreclean.app.data.datasource.contact.ContactDataSource
import com.coreclean.app.domain.model.Contact
import com.coreclean.app.domain.model.ContactDuplicateGroup
import com.coreclean.app.domain.repository.ContactRepository
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    private val dataSource: ContactDataSource
) : ContactRepository {

    override suspend fun getAllContacts(): List<Contact> = dataSource.getAllContacts()

    override suspend fun findDuplicates(): List<ContactDuplicateGroup> {
        val contacts = getAllContacts()
        // Group by normalized display name (case-insensitive, trimmed)
        val groups = contacts
            .filter { it.displayName.isNotBlank() }
            .groupBy { it.displayName.trim().lowercase() }
            .values
            .filter { it.size > 1 }
            .map { ContactDuplicateGroup(it) }

        // Also group by shared phone number
        val phoneGroups = contacts
            .flatMap { c -> c.phones.map { phone -> normalizePhone(phone) to c } }
            .groupBy({ it.first }, { it.second })
            .values
            .filter { it.size > 1 }
            .map { ContactDuplicateGroup(it.distinctBy { c -> c.id }) }

        return (groups + phoneGroups).distinctBy { group ->
            group.contacts.map { it.id }.sorted()
        }
    }

    override suspend fun findIncomplete(): List<Contact> =
        getAllContacts().filter { it.phones.isEmpty() || it.displayName.isBlank() }

    private fun normalizePhone(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        // Drop country code prefix: if starts with "84" (Vietnam) or "1" (US) strip it
        return when {
            digits.startsWith("84") && digits.length > 10 -> digits.drop(2).takeLast(9)
            digits.startsWith("1")  && digits.length > 10 -> digits.drop(1).takeLast(10)
            else -> digits.takeLast(9)
        }
    }
}
