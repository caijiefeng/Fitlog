package com.example.fitlog.core.di

import com.example.fitlog.core.time.AppClock
import com.example.fitlog.core.time.CurrentDateProvider
import com.example.fitlog.core.time.SystemAppClock
import com.example.fitlog.core.time.SystemCurrentDateProvider
import com.example.fitlog.core.time.SystemZoneIdProvider
import com.example.fitlog.core.time.ZoneIdProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    @Provides
    @Singleton
    fun provideAppClock(): AppClock = SystemAppClock()

    @Provides
    @Singleton
    fun provideZoneIdProvider(): ZoneIdProvider = SystemZoneIdProvider()

    @Provides
    @Singleton
    fun provideCurrentDateProvider(): CurrentDateProvider = SystemCurrentDateProvider()
}
