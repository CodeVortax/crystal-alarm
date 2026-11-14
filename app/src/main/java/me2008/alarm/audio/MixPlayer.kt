// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import me2008.alarm.data.AlarmSound

/**
 * Plays one or more sounds together (the "mixer"). Each sound has its own volume and an optional
 * trimmed part that loops; [masterVolume] scales everything. Used by the ringing service and by previews.
 */
class MixPlayer(private val context: Context) {
    private class Voice(val player: MediaPlayer, val sound: AlarmSound)

    private val handler = Handler(Looper.getMainLooper())
    private val voices = mutableListOf<Voice>()
    private var master = 1f
    private var ramp = 1f
    private var released = false

    private val loopCheck = object : Runnable {
        override fun run() {
            if (released) return
            voices.forEach { v ->
                val end = v.sound.endMs
                if (end > 0 && runCatching { v.player.currentPosition }.getOrDefault(0) >= end) {
                    runCatching { v.player.seekTo(v.sound.startMs.toInt()) }
                }
            }
            handler.postDelayed(this, 40L)
        }
    }

    val isPlaying: Boolean get() = voices.isNotEmpty() && !released

    fun play(sounds: List<AlarmSound>, masterVolume: Int = 100, rampSeconds: Int = 0, usage: Int = AudioAttributes.USAGE_ALARM) {
        stop()
        released = false
        master = masterVolume.coerceIn(0, 100) / 100f
        sounds.forEach { s ->
            try {
                val p = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setUsage(usage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    setDataSource(context, AudioInfo.resolve(s.uri))
                    prepare()
                    if (s.startMs > 0) seekTo(s.startMs.toInt())
                    // Plain looping when untrimmed; trimmed sounds are looped by loopCheck / completion.
                    isLooping = s.endMs <= 0 && s.startMs <= 0
                    setOnCompletionListener { mp ->
                        if (!released && !mp.isLooping) { mp.seekTo(s.startMs.toInt()); mp.start() }
                    }
                }
                voices += Voice(p, s)
            } catch (_: Exception) {
                // Skip a sound that can't be opened; the rest still play.
            }
        }
        if (voices.isEmpty() && sounds.isNotEmpty()) {
            // Last resort so an alarm is never silent.
            runCatching {
                val p = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setUsage(usage).build())
                    setDataSource(context, AudioInfo.resolve("default"))
                    isLooping = true; prepare()
                }
                voices += Voice(p, AlarmSound("default", "Default"))
            }
        }
        ramp = if (rampSeconds > 0) 0.05f else 1f
        applyVolumes()
        voices.forEach { runCatching { it.player.start() } }
        handler.post(loopCheck)
        if (rampSeconds > 0) startRamp(rampSeconds)
    }

    private fun startRamp(seconds: Int) {
        val stepMs = 300L
        val steps = (seconds * 1000L / stepMs).toInt().coerceAtLeast(1)
        var i = 0
        handler.post(object : Runnable {
            override fun run() {
                if (released) return
                i++
                ramp = minOf(1f, 0.05f + i.toFloat() / steps * 0.95f)
                applyVolumes()
                if (i < steps) handler.postDelayed(this, stepMs)
            }
        })
    }

    fun setMaster(percent: Int) { master = percent.coerceIn(0, 100) / 100f; applyVolumes() }

    /** Changes the volume of the sound at [index] while playing. */
    fun setVoiceVolume(index: Int, percent: Int) {
        val v = voices.getOrNull(index) ?: return
        voices[index] = Voice(v.player, v.sound.copy(volume = percent))
        applyVolumes()
    }

    private fun applyVolumes() {
        voices.forEach { v ->
            val vol = (v.sound.volume.coerceIn(0, 100) / 100f * master * ramp).coerceIn(0f, 1f)
            runCatching { v.player.setVolume(vol, vol) }
        }
    }

    /** Stops everything and frees the players. Safe to call more than once. */
    fun stop() {
        released = true
        handler.removeCallbacksAndMessages(null)
        voices.forEach { v ->
            runCatching { if (v.player.isPlaying) v.player.stop() }
            runCatching { v.player.release() }
        }
        voices.clear()
    }
}
