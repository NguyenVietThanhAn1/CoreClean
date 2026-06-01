package com.coreclean.app.core.di

import com.coreclean.app.data.repository.RamRepositoryImpl
import com.coreclean.app.domain.repository.RamRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RamModule {
    @Binds @Singleton
    abstract fun bindRamRepository(impl: RamRepositoryImpl): RamRepository
}
