package com.coreclean.app.core.di

import com.coreclean.app.data.repository.BatteryRepositoryImpl
import com.coreclean.app.domain.repository.BatteryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BatteryModule {
    @Binds @Singleton
    abstract fun bindBatteryRepository(impl: BatteryRepositoryImpl): BatteryRepository
}
