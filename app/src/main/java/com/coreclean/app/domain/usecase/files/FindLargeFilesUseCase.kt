package com.coreclean.app.domain.usecase.files

import android.content.ContentResolver
import android.database.Cursor
import android.provider.MediaStore
import com.coreclean.app.domain.model.MediaImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FindLargeFilesUseCase @Inject constructor(
    private val contentResolver: ContentResolver
) {
    suspend operator fun invoke(minSizeBytes: Long = 50L * 1024 * 1024): List<MediaImage> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<MediaImage>()
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATA
            )
            val selection = "${MediaStore.Files.FileColumns.SIZE} > ?"
            val selectionArgs = arrayOf(minSizeBytes.toString())
            val sortOrder = "${MediaStore.Files.FileColumns.SIZE} DESC"

            val cursor: Cursor? = contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection, selection, selectionArgs, sortOrder
            )
            cursor?.use { c ->
                val idCol      = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol    = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol    = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val modCol     = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mimeCol    = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val dataCol    = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                while (c.moveToNext()) {
                    val id       = c.getLong(idCol)
                    val name     = c.getString(nameCol) ?: ""
                    val size     = c.getLong(sizeCol)
                    val modified = c.getLong(modCol) * 1000L
                    val mime     = c.getString(mimeCol) ?: ""
                    val path     = c.getString(dataCol) ?: ""
                    val uri      = android.net.Uri.withAppendedPath(
                        MediaStore.Files.getContentUri("external"), id.toString()
                    )
                    results.add(MediaImage(id, uri, name, path, size, modified, mime))
                }
            }
            results
        }
}
