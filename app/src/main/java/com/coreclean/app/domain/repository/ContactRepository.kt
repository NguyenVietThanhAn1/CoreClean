package com.coreclean.app.domain.repository

import com.coreclean.app.domain.model.Contact
import com.coreclean.app.domain.model.ContactDuplicateGroup

interface ContactRepository {
    suspend fun getAllContacts(): List<Contact>
    suspend fun findDuplicates(): List<ContactDuplicateGroup>
    suspend fun findIncomplete(): List<Contact>
}
