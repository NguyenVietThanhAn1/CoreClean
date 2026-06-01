package com.coreclean.app.domain.usecase.storage

import com.coreclean.app.domain.model.StorageInfo
import com.coreclean.app.domain.repository.StorageRepository
import javax.inject.Inject

class GetStorageInfoUseCase @Inject constructor(
    private val repository: StorageRepository
) {
    suspend operator fun invoke(): StorageInfo = repository.getStorageInfo()
}
