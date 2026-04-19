package com.coreclean.app.domain.usecase.media

import com.coreclean.app.domain.model.DuplicateGroup
import com.coreclean.app.domain.model.MediaImage
import com.coreclean.app.domain.repository.MediaRepository
import javax.inject.Inject

class FindDuplicateImagesUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(images: List<MediaImage>): List<DuplicateGroup> =
        mediaRepository.findDuplicates(images)
}