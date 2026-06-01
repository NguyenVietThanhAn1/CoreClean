package com.coreclean.app.data.repository

import com.coreclean.app.data.datasource.battery.BatteryDataSource
import com.coreclean.app.domain.model.BatteryInfo
import com.coreclean.app.domain.repository.BatteryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryRepositoryImpl @Inject constructor(
    private val dataSource: BatteryDataSource
) : BatteryRepository {
    override fun observe(): Flow<BatteryInfo> = dataSource.observe()
}
