package com.coreclean.app.core.di

import com.coreclean.app.data.crash.SentryCrashReporter
import com.coreclean.app.domain.CrashReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CrashReporterModule {
    @Binds @Singleton
    abstract fun bindCrashReporter(impl: SentryCrashReporter): CrashReporter
}
