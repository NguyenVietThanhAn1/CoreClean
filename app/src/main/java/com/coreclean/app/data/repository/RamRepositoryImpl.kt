package com.coreclean.app.data.repository

import com.coreclean.app.data.datasource.ram.RamDataSource
import com.coreclean.app.domain.model.RamInfo
import com.coreclean.app.domain.repository.RamRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RamRepositoryImpl @Inject constructor(
    private val dataSource: RamDataSource
) : RamRepository {
    override fun observe(): Flow<RamInfo> = dataSource.observe()
}
