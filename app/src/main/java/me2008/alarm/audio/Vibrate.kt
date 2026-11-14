// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import me2008.alarm.data.VibrationPattern

/** Starts a repeating vibration for a pattern id, with strength 10..100 percent. */
object Vibrate {
    fun get(context: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    /** Parses a custom rhythm saved as comma-separated milliseconds ("on,off,on,..."). */
    fun parseCustom(text: String): LongArray =
        text.split(',').mapNotNull { it.trim().toLongOrNull()?.coerceIn(20L, 3000L) }.toLongArray()

    fun timings(pattern: String, custom: String): LongArray {
        val p = VibrationPattern.fromId(pattern)
        if (p == VibrationPattern.CUSTOM) {
            val c = parseCustom(custom)
            if (c.isNotEmpty()) {
                val base = longArrayOf(0L) + c
                return if (c.size % 2 == 1) base + longArrayOf(800L) else base
            }
        }
        return (if (p == VibrationPattern.CUSTOM) VibrationPattern.PULSE else p).timings
    }

    fun start(vibrator: Vibrator?, pattern: String, custom: String, strengthPercent: Int, repeat: Boolean = true) {
        val v = vibrator ?: return
        val t = timings(pattern, custom)
        val repeatIndex = if (repeat) 0 else -1
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && v.hasAmplitudeControl()) {
            val amp = (strengthPercent.coerceIn(10, 100) * 255 / 100).coerceIn(1, 255)
            // Amplitude array parallels timings: 0 for pauses, [amp] for buzzes (odd indexes are buzzes).
            val amps = IntArray(t.size) { if (it % 2 == 1) amp else 0 }
            VibrationEffect.createWaveform(t, amps, repeatIndex)
        } else VibrationEffect.createWaveform(t, repeatIndex)
        v.vibrate(effect)
    }
}
