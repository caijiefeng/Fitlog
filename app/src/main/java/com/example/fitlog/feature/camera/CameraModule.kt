package com.example.fitlog.feature.camera

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @Singleton
    fun provideCameraEngine(): CameraEngine {
        // UNVERIFIED_DEVICE: Swap to CameraXEngine when CameraX API issues resolved.
        // CameraXEngine requires manual verification of Preview, ImageCapture,
        // VideoCapture API compatibility with camera-*:1.3.4.
        return FakeCameraEngine()
    }
}
