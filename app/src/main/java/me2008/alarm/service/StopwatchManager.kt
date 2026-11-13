// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.service

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Stopwatch state that lives outside the screen, so switching tabs does not reset it.
 * Uses the monotonic clock, so it is not affected by changing the wall-clock time.
 */
object StopwatchManager {
    var running by mutableStateOf(false)
        private set
    var baseMs by mutableLongStateOf(0L)
        private set
    /** Cumulative elapsed time at each lap press, oldest first. */
    var laps by mutableStateOf<List<Long>>(emptyList())
        private set
    private var startedAt = 0L

    fun elapsed(now: Long = SystemClock.elapsedRealtime()): Long =
        if (running) baseMs + (now - startedAt) else baseMs

    fun start() {
        if (!running) { startedAt = SystemClock.elapsedRealtime(); running = true }
    }

    fun pause() {
        if (running) { baseMs = elapsed(); running = false }
    }

    fun lap() {
        if (running) laps = laps + elapsed()
    }

    fun reset() {
        running = false; baseMs = 0L; laps = emptyList()
    }
}
