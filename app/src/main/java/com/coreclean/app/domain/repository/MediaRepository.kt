package com.coreclean.app.domain.repository

import android.content.IntentSender
import com.coreclean.app.domain.model.DuplicateGroup
import com.coreclean.app.domain.model.MediaImage
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllImages(): Flow<List<MediaImage>>
    suspend fun findDuplicates(images: List<MediaImage>): List<DuplicateGroup>
    suspend fun deleteImages(images: List<MediaImage>): Result<Int>
    suspend fun createDeleteRequest(images: List<MediaImage>): IntentSender

    suspend fun savePendingReviewIds(ids: List<Long>)
    suspend fun getPendingReviewIds(): List<Long>
    suspend fun clearPendingReviewIds()
    suspend fun getPendingReviewCount(): Int
}
