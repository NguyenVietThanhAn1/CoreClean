package com.coreclean.app.domain.repository

import com.coreclean.app.domain.model.StorageInfo

interface StorageRepository {
    suspend fun getStorageInfo(): StorageInfo
}
