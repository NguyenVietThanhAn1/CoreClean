package com.coreclean.app.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.coreclean.app.core.di.IoDispatcher
import com.coreclean.app.data.datasource.media.DuplicateDetector
import com.coreclean.app.data.datasource.media.MediaDataSource
import com.coreclean.app.domain.model.DuplicateGroup
import com.coreclean.app.domain.model.MediaImage
import com.coreclean.app.domain.repository.MediaRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val mediaDataSource: MediaDataSource,
    private val duplicateDetector: DuplicateDetector,
    private val contentResolver: ContentResolver,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MediaRepository {

    override fun getAllImages(): Flow<List<MediaImage>> =
        mediaDataSource.queryAllImages().flowOn(ioDispatcher)

    override suspend fun findDuplicates(
        images: List<MediaImage>
    ): List<DuplicateGroup> = withContext(ioDispatcher) {
        duplicateDetector.detect(images)
    }

    override suspend fun deleteImages(
        images: List<MediaImage>
    ): Result<Int> = withContext(ioDispatcher) {
        runCatching {
            var deleted = 0
            images.forEach { image ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+ — dùng createDeleteRequest, cần user confirm
                    // Xử lý ở ViewModel với ActivityResultLauncher
                } else {
                    val rows = contentResolver.delete(image.uri, null, null)
                    if (rows > 0) deleted++
                }
            }
            deleted
        }
    }
}
