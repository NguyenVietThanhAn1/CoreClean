package com.coreclean.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.coreclean.app.data.local.dao.ScanResultDao
import com.coreclean.app.data.local.entity.ScanResultEntity
import com.coreclean.app.domain.repository.MediaRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class MediaScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val mediaRepository: MediaRepository,
    private val scanResultDao: ScanResultDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val images = mediaRepository.getAllImages().first()
            mediaRepository.findDuplicates(images) // side-effect: warms duplicate cache
            val entities = images.map { img ->
                ScanResultEntity(
                    filePath     = img.path,
                    fileSize     = img.size,
                    fileType     = img.mimeType,
                    lastModified = img.dateAdded
                )
            }
            scanResultDao.clearAll()
            scanResultDao.insertAll(entities)
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }
}
