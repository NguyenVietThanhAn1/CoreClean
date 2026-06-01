package com.coreclean.app.domain.repository

import com.coreclean.app.domain.model.BatteryInfo
import kotlinx.coroutines.flow.Flow

interface BatteryRepository {
    fun observe(): Flow<BatteryInfo>
}
