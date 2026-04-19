package com.coreclean.app.domain.usecase.media

import com.coreclean.app.domain.model.MediaImage
import com.coreclean.app.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllImagesUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    operator fun invoke(): Flow<List<MediaImage>> =
        mediaRepository.getAllImages()
}