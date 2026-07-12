package com.coreclean.app.domain.repository

import com.coreclean.app.domain.model.ScanResult

interface ScanResultRepository {
    suspend fun replaceAll(results: List<ScanResult>)
    suspend fun getCount(): Int
    suspend fun clearAll()
}
