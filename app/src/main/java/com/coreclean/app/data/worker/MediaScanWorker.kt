package com.coreclean.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.coreclean.app.domain.model.ScanResult
import com.coreclean.app.domain.repository.MediaRepository
import com.coreclean.app.domain.repository.ScanResultRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class MediaScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val mediaRepository: MediaRepository,
    private val scanResultRepository: ScanResultRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val images = mediaRepository.getAllImages().first()
            mediaRepository.findDuplicates(images) // side-effect: warms duplicate cache
            val results = images.map { img ->
                ScanResult(
                    filePath     = img.path,
                    fileSize     = img.size,
                    fileType     = img.mimeType,
                    lastModified = img.dateAdded
                )
            }
            scanResultRepository.replaceAll(results)
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }
}
