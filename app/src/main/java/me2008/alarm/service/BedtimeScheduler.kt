// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import me2008.alarm.MainActivity
import me2008.alarm.data.BedtimeRepository
import me2008.alarm.data.SleepHistory
import me2008.alarm.data.SleepSchedule
import java.time.LocalDate
import java.time.ZoneId

/** Schedules the bedtime reminder, the bedtime notification (with Do Not Disturb) and the end of Do Not Disturb. */
object BedtimeScheduler {
    const val ACTION_REMINDER = "me2008.alarm.BED_REMINDER"
    const val ACTION_BEDTIME = "me2008.alarm.BED_TIME"
    const val ACTION_WAKE_END = "me2008.alarm.WAKE_END"
    const val ACTION_BED_NOW = "me2008.alarm.BED_NOW"
    const val CHANNEL_ID = "crystal_bedtime"
    const val NOTIFICATION_ID = 3001

    private val kinds = listOf(ACTION_REMINDER to 8001, ACTION_BEDTIME to 8002, ACTION_WAKE_END to 8003)

    private fun pending(context: Context, action: String, code: Int, flags: Int): PendingIntent? {
        val intent = Intent(context, BedtimeReceiver::class.java).setAction(action)
        val immutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(context, code, intent, flags or immutable)
    }

    /** Cancels everything, then sets the next reminder / bedtime / wake-end for the saved schedule. */
    fun scheduleAll(context: Context) {
        val repo = BedtimeRepository(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        kinds.forEach { (action, code) -> pending(context, action, code, PendingIntent.FLAG_NO_CREATE)?.let { am.cancel(it); it.cancel() } }
        if (!repo.isConfigured()) return
        val s = repo.load()

        fun set(action: String, code: Int, at: Long?) {
            if (at == null) return
            val pi = pending(context, action, code, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
            AlarmManagerCompat.setExactAndAllowWhileIdle(am, AlarmManager.RTC_WAKEUP, at, pi)
        }
        if (s.reminderMin > 0) set(ACTION_REMINDER, 8001, nextBed(s, -s.reminderMin))
        set(ACTION_BEDTIME, 8002, nextBed(s, 0))
        if (s.dnd) set(ACTION_WAKE_END, 8003, nextWake(s))
    }

    /** Next bedtime (plus [shiftMin] minutes) on a night that is followed by a wake-up day you chose. */
    private fun nextBed(s: SleepSchedule, shiftMin: Int): Long? {
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val today = LocalDate.now(zone)
        for (offset in -1L..8L) {
            val d = today.plusDays(offset)
            val wakeDay = if (s.bedMinutes > s.wakeMinutes) d.plusDays(1) else d
            if (s.days.none { it.value == wakeDay.dayOfWeek.value }) continue
            val t = d.atTime(s.bedMinutes / 60, s.bedMinutes % 60).atZone(zone).toInstant().toEpochMilli() + shiftMin * 60_000L
            if (t > now) return t
        }
        return null
    }

    private fun nextWake(s: SleepSchedule): Long? {
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val today = LocalDate.now(zone)
        for (offset in 0L..2L) {
            val t = today.plusDays(offset).atTime(s.wakeMinutes / 60, s.wakeMinutes % 60).atZone(zone).toInstant().toEpochMilli()
            if (t > now) return t
        }
        return null
    }
}

class BedtimeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val s = BedtimeRepository(context).load()
        when (intent.action) {
            BedtimeScheduler.ACTION_REMINDER -> notify(context, nm, "Bedtime in ${s.reminderMin} min", "Start winding down.", withBedAction = false)
            BedtimeScheduler.ACTION_BEDTIME -> {
                notify(context, nm, "Time for bed", "Tap when you go to sleep so your sleep is tracked.", withBedAction = true)
                if (s.dnd) setDnd(context, nm, on = true)
            }
            BedtimeScheduler.ACTION_WAKE_END -> setDnd(context, nm, on = false)
            BedtimeScheduler.ACTION_BED_NOW -> {
                SleepHistory(context).markBedNow()
                nm.cancel(BedtimeScheduler.NOTIFICATION_ID)
                return
            }
            else -> return
        }
        BedtimeScheduler.scheduleAll(context)
    }

    private fun notify(context: Context, nm: NotificationManager, title: String, text: String, withBedAction: Boolean) {
        nm.createNotificationChannel(NotificationChannel(BedtimeScheduler.CHANNEL_ID, "Bedtime", NotificationManager.IMPORTANCE_DEFAULT))
        val immutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), immutable)
        val b = NotificationCompat.Builder(context, BedtimeScheduler.CHANNEL_ID)
            .setContentTitle(title).setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(open).setAutoCancel(true)
        if (withBedAction) {
            val bedNow = PendingIntent.getBroadcast(context, 8004,
                Intent(context, BedtimeReceiver::class.java).setAction(BedtimeScheduler.ACTION_BED_NOW), immutable)
            b.addAction(0, "I'm going to bed", bedNow)
        }
        try { nm.notify(BedtimeScheduler.NOTIFICATION_ID, b.build()) } catch (_: SecurityException) { /* notifications not allowed */ }
    }

    /** Alarms-only Do Not Disturb. Remembers whether we turned it on, so we never switch off the user's own setting. */
    private fun setDnd(context: Context, nm: NotificationManager, on: Boolean) {
        if (!nm.isNotificationPolicyAccessGranted) return
        val prefs = context.getSharedPreferences("crystal_bedtime", Context.MODE_PRIVATE)
        if (on) {
            if (!prefs.contains("dndPrev")) prefs.edit().putInt("dndPrev", nm.currentInterruptionFilter).apply()
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
        } else if (prefs.contains("dndPrev")) {
            nm.setInterruptionFilter(prefs.getInt("dndPrev", NotificationManager.INTERRUPTION_FILTER_ALL))
            prefs.edit().remove("dndPrev").apply()
        }
    }
}
