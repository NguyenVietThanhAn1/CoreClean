package com.coreclean.app.data.repository

import com.coreclean.app.data.datasource.battery.BatteryDataSource
import com.coreclean.app.data.local.dao.BatteryHistoryDao
import com.coreclean.app.data.local.entity.BatteryHistoryEntity
import com.coreclean.app.domain.model.BatteryHistoryEntry
import com.coreclean.app.domain.model.BatteryInfo
import com.coreclean.app.domain.repository.BatteryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryRepositoryImpl @Inject constructor(
    private val dataSource: BatteryDataSource,
    private val batteryHistoryDao: BatteryHistoryDao
) : BatteryRepository {
    override fun observe(): Flow<BatteryInfo> = dataSource.observe()

    override suspend fun recordHistory(entry: BatteryHistoryEntry) {
        batteryHistoryDao.insert(
            BatteryHistoryEntity(
                timestamp = entry.timestamp,
                levelPercent = entry.levelPercent,
                isCharging = entry.isCharging
            )
        )
    }

    override suspend fun getDischargingHistorySince(sinceMs: Long): List<BatteryHistoryEntry> =
        batteryHistoryDao.getDischargingSince(sinceMs).map { it.toDomain() }

    override suspend fun getAllHistorySince(sinceMs: Long): List<BatteryHistoryEntry> =
        batteryHistoryDao.getAllSince(sinceMs).map { it.toDomain() }

    override suspend fun getHistoryCount(): Int = batteryHistoryDao.count()

    override suspend fun clearHistory() = batteryHistoryDao.clearAll()

    private fun BatteryHistoryEntity.toDomain() =
        BatteryHistoryEntry(timestamp, levelPercent, isCharging)
}
