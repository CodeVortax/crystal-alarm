// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.data

/** Countdown timer state. */
data class TimerSession(
    val totalSeconds: Long = 0L,
    val remainingSeconds: Long = 0L,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val label: String = "",
) {
    val progress: Float
        get() = if (totalSeconds > 0L) remainingSeconds.toFloat() / totalSeconds.toFloat() else 1f
}

enum class TimerPreset(val seconds: Long, val label: String) {
    POMODORO(25 * 60, "Pomodoro"),
    SHORT_BREAK(5 * 60, "Short break"),
    LONG_BREAK(15 * 60, "Long break"),
    CUSTOM(0, "Custom"),
}
