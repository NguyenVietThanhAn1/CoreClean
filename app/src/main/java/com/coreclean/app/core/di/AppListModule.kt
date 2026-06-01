package com.coreclean.app.core.di

import com.coreclean.app.data.repository.AppListRepositoryImpl
import com.coreclean.app.domain.repository.AppListRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppListModule {
    @Binds @Singleton
    abstract fun bindAppListRepository(impl: AppListRepositoryImpl): AppListRepository
}
