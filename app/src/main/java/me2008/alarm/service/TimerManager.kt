// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.AlarmManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me2008.alarm.data.TimerPreset
import me2008.alarm.data.TimerSession

/**
 * Countdown timer.
 *
 * Time is measured against the monotonic clock (not by counting ticks), so it cannot drift or fall behind
 * while the app is in the background. An exact alarm is also set for the end time; it starts
 * [TimerRingService] so the timer rings even if the app is not on screen.
 */
object TimerManager {
    private val _state = MutableStateFlow(TimerSession())
    val state: StateFlow<TimerSession> = _state.asStateFlow()
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private var appContext: Context? = null

    /** SystemClock.elapsedRealtime() at which the timer reaches zero, while running. */
    private var endAt = 0L
    /** Milliseconds left while paused. */
    private var leftMs = 0L

    fun start(preset: TimerPreset, customSeconds: Long = 0L, context: Context? = null) {
        if (context != null) appContext = context.applicationContext
        val total = if (preset == TimerPreset.CUSTOM) customSeconds else preset.seconds
        leftMs = total * 1000L
        _state.value = TimerSession(totalSeconds = total, remainingSeconds = total, isRunning = true)
        run()
    }

    fun pause() {
        if (!_state.value.isRunning) return
        leftMs = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        job?.cancel()
        cancelRing()
        _state.value = _state.value.copy(isRunning = false, remainingSeconds = (leftMs + 999L) / 1000L)
    }

    fun resume() {
        val s = _state.value
        if (s.totalSeconds > 0L && !s.isFinished && !s.isRunning) {
            _state.value = s.copy(isRunning = true)
            run()
        }
    }

    fun reset() {
        job?.cancel()
        cancelRing()
        appContext?.let { it.stopService(Intent(it, TimerRingService::class.java)) }
        leftMs = 0L
        _state.value = TimerSession()
    }

    fun addSeconds(s: Long) {
        val cur = _state.value
        if (cur.isFinished || cur.totalSeconds == 0L) return
        if (cur.isRunning) {
            endAt += s * 1000L
            scheduleRing(endAt - SystemClock.elapsedRealtime())
            _state.value = cur.copy(totalSeconds = cur.totalSeconds + s)
            job?.cancel()
            launchTicker()
        } else {
            leftMs += s * 1000L
            _state.value = cur.copy(totalSeconds = cur.totalSeconds + s, remainingSeconds = (leftMs + 999L) / 1000L)
        }
    }

    private fun run() {
        endAt = SystemClock.elapsedRealtime() + leftMs
        scheduleRing(leftMs)
        job?.cancel()
        launchTicker()
    }

    private fun launchTicker() {
        job = scope.launch {
            while (true) {
                val ms = endAt - SystemClock.elapsedRealtime()
                if (ms <= 0L) {
                    _state.value = _state.value.copy(remainingSeconds = 0L, isRunning = false, isFinished = true)
                    break
                }
                _state.value = _state.value.copy(remainingSeconds = (ms + 999L) / 1000L)
                // Sleep until the displayed second changes.
                delay((ms - 1L) % 1000L + 1L)
            }
        }
    }

    private fun ringIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 7001, Intent(context, TimerReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun scheduleRing(inMs: Long) {
        val ctx = appContext ?: return
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = SystemClock.elapsedRealtime() + inMs.coerceAtLeast(0L)
        try {
            AlarmManagerCompat.setExactAndAllowWhileIdle(am, AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, ringIntent(ctx))
        } catch (_: SecurityException) {
            // Exact alarms not allowed: fall back to the closest the system allows.
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, ringIntent(ctx))
        }
    }

    private fun cancelRing() {
        val ctx = appContext ?: return
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(ringIntent(ctx))
    }
}
