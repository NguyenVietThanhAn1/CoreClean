package com.coreclean.app.core.di

import android.content.ContentResolver
import android.content.Context
import com.coreclean.app.data.repository.MediaRepositoryImpl
import com.coreclean.app.domain.repository.MediaRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {

    @Binds @Singleton
    abstract fun bindMediaRepository(
        impl: MediaRepositoryImpl
    ): MediaRepository

    companion object {
        @Provides @Singleton
        fun provideContentResolver(
            @ApplicationContext context: Context
        ): ContentResolver = context.contentResolver
    }
}
