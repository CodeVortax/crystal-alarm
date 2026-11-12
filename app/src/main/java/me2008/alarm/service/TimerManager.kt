// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me2008.alarm.data.TimerSession
import me2008.alarm.data.TimerPreset

object TimerManager {
    private val _state = MutableStateFlow(TimerSession())
    val state: StateFlow<TimerSession> = _state.asStateFlow()
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun start(preset: TimerPreset, customSeconds: Long = 0L) {
        val total = if (preset == TimerPreset.CUSTOM) customSeconds else preset.seconds
        _state.value = TimerSession(totalSeconds = total, remainingSeconds = total, isRunning = true)
        job?.cancel()
        job = scope.launch {
            while (_state.value.remainingSeconds > 0) {
                delay(1000L)
                _state.value = _state.value.copy(remainingSeconds = _state.value.remainingSeconds - 1)
            }
            _state.value = _state.value.copy(isRunning = false, isFinished = true)
        }
    }

    fun pause() { job?.cancel(); _state.value = _state.value.copy(isRunning = false) }

    fun resume() {
        if (_state.value.totalSeconds > 0 && !_state.value.isFinished) {
            _state.value = _state.value.copy(isRunning = true)
            job?.cancel()
            job = scope.launch {
                while (_state.value.remainingSeconds > 0) {
                    delay(1000L)
                    _state.value = _state.value.copy(remainingSeconds = _state.value.remainingSeconds - 1)
                }
                _state.value = _state.value.copy(isRunning = false, isFinished = true)
            }
        }
    }

    fun reset() { job?.cancel(); _state.value = TimerSession() }
    fun addSeconds(s: Long) { _state.value = _state.value.copy(
        totalSeconds = _state.value.totalSeconds + s,
        remainingSeconds = _state.value.remainingSeconds + s) }
}
