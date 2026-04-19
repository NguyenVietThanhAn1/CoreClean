package com.coreclean.app.data.datasource.media

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.coreclean.app.domain.model.MediaImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun queryAllImages(): Flow<List<MediaImage>> = flow {
        val images = mutableListOf<MediaImage>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection, projection, null, null, sortOrder
        )?.use { cursor ->
            val idCol       = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol     = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dataCol     = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val sizeCol     = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateCol     = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val mimeCol     = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val widthCol    = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol   = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id  = cursor.getLong(idCol)
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString()
                )
                images += MediaImage(
                    id        = id,
                    uri       = uri,
                    name      = cursor.getString(nameCol) ?: "unknown",
                    path      = cursor.getString(dataCol) ?: "",
                    size      = cursor.getLong(sizeCol),
                    dateAdded = cursor.getLong(dateCol),
                    mimeType  = cursor.getString(mimeCol) ?: "image/jpeg",
                    width     = cursor.getInt(widthCol),
                    height    = cursor.getInt(heightCol)
                )
            }
        }
        emit(images)
    }
}
