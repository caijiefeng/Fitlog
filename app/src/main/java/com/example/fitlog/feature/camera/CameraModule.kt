package com.example.fitlog.feature.camera

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Camera dependency injection module.
 *
 * CameraXEngine is not provided through Hilt because it requires
 * [android.content.Context] and [androidx.lifecycle.LifecycleOwner]
 * which are only available in the composable scope.
 *
 * The screen creates [CameraXEngine] directly and passes it to
 * [FitLogCameraViewModel.cameraEngine].
 */
@Module
@InstallIn(SingletonComponent::class)
object CameraModule
