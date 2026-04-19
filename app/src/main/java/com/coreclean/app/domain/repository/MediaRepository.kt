package com.coreclean.app.domain.repository

import com.coreclean.app.domain.model.DuplicateGroup
import com.coreclean.app.domain.model.MediaImage
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllImages(): Flow<List<MediaImage>>
    suspend fun findDuplicates(images: List<MediaImage>): List<DuplicateGroup>
    suspend fun deleteImages(images: List<MediaImage>): Result<Int>  // số file đã xóa
}