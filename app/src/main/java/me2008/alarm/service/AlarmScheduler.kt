// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import me2008.alarm.data.Alarm
import me2008.alarm.data.AlarmRepository
import java.util.Calendar

/**
 * Schedules alarms with Android's AlarmManager.
 * Each alarm fires into [AlarmReceiver], which starts the foreground service.
 */
object AlarmScheduler {
    /**
     * @param rollOver when true, a one-time alarm whose time already passed today is set for
     * tomorrow (used when the user creates or turns on an alarm). When false (re-scheduling after
     * it rang, or after boot) a passed one-time alarm is left unscheduled.
     */
    fun schedule(context: Context, alarm: Alarm, rollOver: Boolean = false) {
        cancel(context, alarm.id)
        if (!alarm.enabled) return

        val triggerAt = nextTrigger(alarm, rollOver) ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
        val pi = PendingIntent.getBroadcast(context, alarm.id.hashCode() and 0xFFFF, intent, flags)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        AlarmManagerCompat.setExactAndAllowWhileIdle(am, AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    fun cancel(context: Context, alarmId: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val immutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
        val pi = PendingIntent.getBroadcast(context, alarmId.hashCode() and 0xFFFF, intent,
            immutable or PendingIntent.FLAG_NO_CREATE)
        pi?.cancel()
    }

    fun rescheduleAll(context: Context) {
        AlarmRepository(context).getEnabled().forEach { schedule(context, it) }
    }

    fun reschedule(context: Context, alarmId: String) {
        AlarmRepository(context).get(alarmId)?.let { schedule(context, it) }
    }

    /** Next time [alarm] should ring, or null if it has no upcoming occurrence. */
    fun nextTrigger(alarm: Alarm, rollOver: Boolean = false): Long? {
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (alarm.repeatDays.isEmpty()) {
            if (cal.timeInMillis > now.timeInMillis) return cal.timeInMillis
            if (!rollOver) return null
            cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }

        val targetDays = alarm.repeatDays.map { it.value }
        for (offset in 0..7) {
            val test = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                add(Calendar.DAY_OF_YEAR, offset)
            }
            // Calendar: Sunday = 1 … Saturday = 7. The app's DayOfWeek: Monday = 1 … Sunday = 7.
            val calDow = test.get(Calendar.DAY_OF_WEEK)
            val dow = if (calDow == Calendar.SUNDAY) 7 else calDow - 1
            if (dow in targetDays && test.timeInMillis > now.timeInMillis) return test.timeInMillis
        }
        return null
    }

    const val EXTRA_ALARM_ID = "crystal_alarm_id"
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
        AlarmForegroundService.start(context, id)
    }
}
