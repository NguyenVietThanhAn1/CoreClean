package com.coreclean.app.domain.model

data class Contact(
    val id: Long,
    val displayName: String,
    val phones: List<String>,
    val emails: List<String>,
    val hasPhoto: Boolean
)

data class ContactDuplicateGroup(
    val contacts: List<Contact>
)
