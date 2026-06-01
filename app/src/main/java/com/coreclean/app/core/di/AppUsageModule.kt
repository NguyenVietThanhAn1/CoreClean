package com.coreclean.app.core.di

import com.coreclean.app.data.repository.AppUsageRepositoryImpl
import com.coreclean.app.domain.repository.AppUsageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppUsageModule {
    @Binds @Singleton
    abstract fun bindAppUsageRepository(impl: AppUsageRepositoryImpl): AppUsageRepository
}
