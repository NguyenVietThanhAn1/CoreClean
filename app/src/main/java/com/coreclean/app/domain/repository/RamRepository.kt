package com.coreclean.app.domain.repository

import com.coreclean.app.domain.model.RamInfo
import kotlinx.coroutines.flow.Flow

interface RamRepository {
    fun observe(): Flow<RamInfo>
}
