package com.coreclean.app.domain.usecase.contact

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.provider.ContactsContract
import com.coreclean.app.domain.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MergeContactsUseCase @Inject constructor(
    private val contentResolver: ContentResolver
) {
    /**
     * Merges [contacts] by setting AggregationException.TYPE_KEEP_TOGETHER on all pairs.
     * This forces the Android contacts aggregator to treat them as one contact.
     */
    suspend fun invoke(contacts: List<Contact>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (contacts.size < 2) return@runCatching

            // Get raw contact IDs for each contact
            val rawIds = contacts.flatMap { contact ->
                getRawContactIds(contact.id)
            }

            if (rawIds.size < 2) return@runCatching

            val ops = ArrayList<ContentProviderOperation>()
            // Set KEEP_TOGETHER for all pairs
            for (i in rawIds.indices) {
                for (j in i + 1 until rawIds.size) {
                    ops.add(
                        ContentProviderOperation.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI)
                            .withValue(ContactsContract.AggregationExceptions.TYPE, ContactsContract.AggregationExceptions.TYPE_KEEP_TOGETHER)
                            .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID1, rawIds[i])
                            .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID2, rawIds[j])
                            .build()
                    )
                }
            }
            if (ops.isNotEmpty()) {
                contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            }
        }
    }

    private fun getRawContactIds(contactId: Long): List<Long> {
        val ids = mutableListOf<Long>()
        contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts._ID)
            while (cursor.moveToNext()) ids.add(cursor.getLong(idCol))
        }
        return ids
    }
}
