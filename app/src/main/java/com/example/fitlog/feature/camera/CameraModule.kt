package com.example.fitlog.feature.camera

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides the production [CameraEngine] binding.
 *
 * In tests you can override this binding by providing a [FakeCameraEngine]
 * in a dedicated test module.
 */
@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @Singleton
    fun provideCameraEngine(
        @ApplicationContext context: Context,
    ): CameraEngine {
        return CameraXEngine(context)
    }
}
