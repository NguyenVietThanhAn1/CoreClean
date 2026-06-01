package com.coreclean.app.data.repository

import com.coreclean.app.data.datasource.storage.StorageDataSource
import com.coreclean.app.domain.model.StorageInfo
import com.coreclean.app.domain.repository.StorageRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val dataSource: StorageDataSource
) : StorageRepository {
    override suspend fun getStorageInfo(): StorageInfo = dataSource.getStorageInfo()
}
