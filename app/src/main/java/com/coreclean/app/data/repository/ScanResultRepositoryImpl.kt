package com.coreclean.app.data.repository

import com.coreclean.app.data.local.dao.ScanResultDao
import com.coreclean.app.data.local.entity.ScanResultEntity
import com.coreclean.app.domain.model.ScanResult
import com.coreclean.app.domain.repository.ScanResultRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanResultRepositoryImpl @Inject constructor(
    private val dao: ScanResultDao
) : ScanResultRepository {

    override suspend fun replaceAll(results: List<ScanResult>) {
        dao.clearAll()
        dao.insertAll(
            results.map {
                ScanResultEntity(
                    filePath     = it.filePath,
                    fileSize     = it.fileSize,
                    fileType     = it.fileType,
                    lastModified = it.lastModified
                )
            }
        )
    }

    override suspend fun getCount(): Int = dao.count()

    override suspend fun clearAll() = dao.clearAll()
}
