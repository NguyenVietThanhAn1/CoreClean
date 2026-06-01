package com.coreclean.app.domain.repository

import com.coreclean.app.domain.model.InstalledApp

interface AppListRepository {
    suspend fun getInstalledApps(): List<InstalledApp>
}
