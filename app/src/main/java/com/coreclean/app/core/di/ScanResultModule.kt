package com.coreclean.app.core.di

import com.coreclean.app.data.repository.ScanResultRepositoryImpl
import com.coreclean.app.domain.repository.ScanResultRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ScanResultModule {
    @Binds @Singleton
    abstract fun bindScanResultRepository(impl: ScanResultRepositoryImpl): ScanResultRepository
}
