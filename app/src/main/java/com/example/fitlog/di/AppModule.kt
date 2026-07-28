package com.example.fitlog.di

import com.example.fitlog.domain.example.GetExampleUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGetExampleUseCase(): GetExampleUseCase {
        return GetExampleUseCase()
    }
}
