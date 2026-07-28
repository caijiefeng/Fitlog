package com.example.fitlog.domain.example

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Example UseCase demonstrating the domain layer pattern.
 *
 * Architecture: ViewModel → UseCase → Repository → DataSource
 * This will be replaced with real UseCases in future versions.
 */
class GetExampleUseCase @Inject constructor() {

    operator fun invoke(): Flow<String> {
        return flowOf("FitLog is ready.")
    }
}
