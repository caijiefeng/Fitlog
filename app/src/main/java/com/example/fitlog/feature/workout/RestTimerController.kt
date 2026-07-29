package com.example.fitlog.feature.workout

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RestTimerState(
    val startedAt: Long = 0L,
    val durationSeconds: Int = 0,
    val isRunning: Boolean = false,
    val remainingSeconds: Int = 0,
    val isFinished: Boolean = false,
)

interface TimeProvider {
    fun currentTimeMillis(): Long
}

class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis() = System.currentTimeMillis()
}

class RestTimerController(
    private val timeProvider: TimeProvider = SystemTimeProvider(),
) {
    private val _state = MutableStateFlow(RestTimerState())
    val state: StateFlow<RestTimerState> = _state.asStateFlow()

    fun start(durationSeconds: Int, startedAt: Long = timeProvider.currentTimeMillis()) {
        val clamped = durationSeconds.coerceAtLeast(0)
        _state.value = RestTimerState(
            startedAt = startedAt,
            durationSeconds = clamped,
            isRunning = clamped > 0,
            remainingSeconds = clamped,
            isFinished = clamped == 0,
        )
    }

    fun tick(currentTimeMillis: Long = timeProvider.currentTimeMillis()) {
        val s = _state.value
        if (!s.isRunning || s.isFinished) return
        val elapsed = ((currentTimeMillis - s.startedAt) / 1000).toInt()
        val remaining = (s.durationSeconds - elapsed).coerceAtLeast(0)
        _state.value = s.copy(
            remainingSeconds = remaining,
            isFinished = remaining <= 0,
            isRunning = remaining > 0,
        )
    }

    fun add15Seconds() {
        val s = _state.value
        if (!s.isRunning || s.isFinished) return
        val newDuration = s.durationSeconds + 15
        val elapsed = ((timeProvider.currentTimeMillis() - s.startedAt) / 1000).toInt()
        _state.value = s.copy(
            durationSeconds = newDuration,
            remainingSeconds = (newDuration - elapsed).coerceAtLeast(0),
        )
    }

    fun subtract15Seconds() {
        val s = _state.value
        if (!s.isRunning || s.isFinished) return
        val newDuration = (s.durationSeconds - 15).coerceAtLeast(0)
        val elapsed = ((timeProvider.currentTimeMillis() - s.startedAt) / 1000).toInt()
        val remaining = (newDuration - elapsed).coerceAtLeast(0)
        val finished = newDuration <= 0 || remaining <= 0
        _state.value = s.copy(
            durationSeconds = newDuration,
            remainingSeconds = remaining,
            isFinished = finished,
            isRunning = !finished,
        )
    }

    fun skip() {
        _state.value = _state.value.copy(isRunning = false, isFinished = true, remainingSeconds = 0)
    }

    fun stop() {
        _state.value = _state.value.copy(isRunning = false)
    }

    fun restore(startedAt: Long, durationSeconds: Int) {
        start(durationSeconds, startedAt)
        tick()
    }

    fun reset() {
        _state.value = RestTimerState()
    }
}
