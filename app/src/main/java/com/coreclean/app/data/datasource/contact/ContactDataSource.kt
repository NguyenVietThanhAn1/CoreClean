package com.coreclean.app.data.datasource.contact

import android.content.ContentResolver
import android.provider.ContactsContract
import com.coreclean.app.domain.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ContactDataSource @Inject constructor(
    private val contentResolver: ContentResolver
) {
    suspend fun getAllContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contacts = mutableMapOf<Long, ContactBuilder>()

        // Query phones
        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )?.use { cursor ->
            val idCol    = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameCol  = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numCol   = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val id     = cursor.getLong(idCol)
                val name   = cursor.getString(nameCol) ?: ""
                val number = cursor.getString(numCol) ?: ""
                contacts.getOrPut(id) { ContactBuilder(id, name) }.phones.add(number)
            }
        }

        // Query emails
        contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.ADDRESS
            ),
            null, null, null
        )?.use { cursor ->
            val idCol      = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
            val emailCol   = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (cursor.moveToNext()) {
                val id    = cursor.getLong(idCol)
                val email = cursor.getString(emailCol) ?: ""
                contacts[id]?.emails?.add(email)
            }
        }

        // Query contacts with no phone (display name only via RawContacts)
        contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.PHOTO_ID
            ),
            null, null, null
        )?.use { cursor ->
            val idCol      = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameCol    = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val photoCol   = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_ID)
            while (cursor.moveToNext()) {
                val id       = cursor.getLong(idCol)
                val name     = cursor.getString(nameCol) ?: ""
                val hasPhoto = !cursor.isNull(photoCol)
                contacts.getOrPut(id) { ContactBuilder(id, name) }.also {
                    if (it.displayName.isEmpty()) it.displayName = name
                    it.hasPhoto = hasPhoto
                }
            }
        }

        contacts.values.map { it.build() }
    }

    private class ContactBuilder(val id: Long, var displayName: String) {
        val phones = mutableListOf<String>()
        val emails = mutableListOf<String>()
        var hasPhoto = false
        fun build() = Contact(id, displayName, phones.distinct(), emails.distinct(), hasPhoto)
    }
}
