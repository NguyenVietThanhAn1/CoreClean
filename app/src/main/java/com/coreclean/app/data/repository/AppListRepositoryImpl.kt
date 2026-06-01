package com.coreclean.app.data.repository

import com.coreclean.app.data.datasource.applist.AppListDataSource
import com.coreclean.app.domain.model.InstalledApp
import com.coreclean.app.domain.repository.AppListRepository
import javax.inject.Inject

class AppListRepositoryImpl @Inject constructor(
    private val dataSource: AppListDataSource
) : AppListRepository {
    override suspend fun getInstalledApps(): List<InstalledApp> = dataSource.getInstalledApps()
}
