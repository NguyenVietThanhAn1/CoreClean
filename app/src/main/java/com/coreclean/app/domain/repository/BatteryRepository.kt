package com.coreclean.app.domain.repository

import com.coreclean.app.domain.model.BatteryHistoryEntry
import com.coreclean.app.domain.model.BatteryInfo
import kotlinx.coroutines.flow.Flow

interface BatteryRepository {
    fun observe(): Flow<BatteryInfo>

    suspend fun recordHistory(entry: BatteryHistoryEntry)
    suspend fun getDischargingHistorySince(sinceMs: Long): List<BatteryHistoryEntry>
    suspend fun getAllHistorySince(sinceMs: Long): List<BatteryHistoryEntry>
    suspend fun getHistoryCount(): Int
    suspend fun clearHistory()
}
