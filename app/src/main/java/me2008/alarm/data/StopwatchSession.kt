// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.data

data class StopwatchSession(
    val elapsedMs: Long = 0L,
    val isRunning: Boolean = false,
    val laps: List<Long> = emptyList(),
)
