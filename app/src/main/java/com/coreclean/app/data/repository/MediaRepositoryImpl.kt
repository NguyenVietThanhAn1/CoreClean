package com.coreclean.app.data.repository

import android.content.ContentResolver
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import com.coreclean.app.core.di.IoDispatcher
import com.coreclean.app.data.datasource.media.DuplicateDetector
import com.coreclean.app.data.datasource.media.MediaDataSource
import com.coreclean.app.data.local.dao.PendingReviewDao
import com.coreclean.app.data.local.entity.PendingReviewEntity
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
    private val pendingReviewDao: PendingReviewDao,
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return@withContext Result.failure(
                UnsupportedOperationException("Android 11+: use createDeleteRequest instead.")
            )
        }
        runCatching {
            var deleted = 0
            images.forEach { image ->
                if (contentResolver.delete(image.uri, null, null) > 0) deleted++
            }
            deleted
        }
    }

    override suspend fun createDeleteRequest(
        images: List<MediaImage>
    ): IntentSender = withContext(ioDispatcher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = images.map { it.uri }
            MediaStore.createDeleteRequest(contentResolver, uris).intentSender
        } else {
            throw UnsupportedOperationException("createDeleteRequest requires Android 11+")
        }
    }

    override suspend fun savePendingReviewIds(ids: List<Long>) = withContext(ioDispatcher) {
        pendingReviewDao.clearAll()
        pendingReviewDao.insertAll(ids.map { PendingReviewEntity(it) })
    }

    override suspend fun getPendingReviewIds(): List<Long> = withContext(ioDispatcher) {
        pendingReviewDao.getAllIds()
    }

    override suspend fun clearPendingReviewIds() = withContext(ioDispatcher) {
        pendingReviewDao.clearAll()
    }

    override suspend fun getPendingReviewCount(): Int = withContext(ioDispatcher) {
        pendingReviewDao.count()
    }
}
