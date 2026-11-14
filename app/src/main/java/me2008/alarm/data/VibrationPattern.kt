// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.data

/**
 * Built-in vibration rhythms. [timings] use Android's waveform layout: a leading delay of 0, then
 * alternating "on" and "off" lengths in milliseconds. The whole pattern repeats while the alarm rings.
 */
enum class VibrationPattern(val id: String, val label: String, val timings: LongArray) {
    PULSE("pulse", "Pulse", longArrayOf(0, 500, 500)),
    HEARTBEAT("heartbeat", "Heartbeat", longArrayOf(0, 120, 120, 120, 900)),
    RAPID("rapid", "Rapid", longArrayOf(0, 150, 150)),
    LONG("long", "Long buzz", longArrayOf(0, 1500, 700)),
    ESCALATING("escalating", "Escalating", longArrayOf(0, 150, 350, 300, 350, 600, 350, 1000, 800)),
    SOS("sos", "SOS", longArrayOf(0, 200, 200, 200, 200, 200, 500, 600, 200, 600, 200, 600, 500, 200, 200, 200, 200, 200, 1200)),
    CUSTOM("custom", "Your own rhythm", longArrayOf());

    companion object {
        fun fromId(id: String): VibrationPattern = entries.firstOrNull { it.id == id } ?: PULSE
    }
}
