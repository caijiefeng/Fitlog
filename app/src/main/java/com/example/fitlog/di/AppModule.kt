package com.example.fitlog.di

import com.example.fitlog.data.nutrition.LocalFoodDataProvider
import com.example.fitlog.domain.example.GetExampleUseCase
import com.example.fitlog.domain.nutrition.FoodDataProvider
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

    @Provides
    @Singleton
    fun provideFoodDataProvider(local: LocalFoodDataProvider): FoodDataProvider {
        return local
    }
}
