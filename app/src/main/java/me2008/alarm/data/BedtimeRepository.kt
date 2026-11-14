// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.data

import android.content.Context
import me2008.alarm.service.AlarmScheduler
import me2008.alarm.service.BedtimeScheduler

/** The user's sleep schedule: when they go to bed and when the wake-up alarm rings. */
data class SleepSchedule(
    val bedMinutes: Int = 22 * 60 + 30,
    val wakeMinutes: Int = 7 * 60,
    val wakeAlarm: Boolean = true,
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val vibrate: Boolean = true,
    val volumeRamp: Boolean = true,
    /** Minutes before bedtime to remind you; 0 turns the reminder off. */
    val reminderMin: Int = 30,
    /** Switch on Do Not Disturb (alarms still ring) from bedtime until wake-up. */
    val dnd: Boolean = false,
)

class BedtimeRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("crystal_bedtime", Context.MODE_PRIVATE)

    /** False until the user has saved a schedule once. */
    fun isConfigured(): Boolean = prefs.contains("bed")

    fun load(): SleepSchedule {
        val d = SleepSchedule()
        return SleepSchedule(
            bedMinutes = prefs.getInt("bed", d.bedMinutes),
            wakeMinutes = prefs.getInt("wake", d.wakeMinutes),
            wakeAlarm = prefs.getBoolean("wakeAlarm", d.wakeAlarm),
            days = (prefs.getString("days", null)?.split(",")?.mapNotNull { v ->
                DayOfWeek.entries.firstOrNull { it.value == v.toIntOrNull() }
            }?.toSet()) ?: d.days,
            vibrate = prefs.getBoolean("vibrate", d.vibrate),
            volumeRamp = prefs.getBoolean("volumeRamp", d.volumeRamp),
            reminderMin = prefs.getInt("reminderMin", d.reminderMin),
            dnd = prefs.getBoolean("dnd", d.dnd),
        )
    }

    /** Saves the schedule and creates, updates or removes the wake-up alarm to match. */
    fun save(s: SleepSchedule) {
        prefs.edit()
            .putInt("bed", s.bedMinutes).putInt("wake", s.wakeMinutes)
            .putBoolean("wakeAlarm", s.wakeAlarm)
            .putString("days", s.days.joinToString(",") { it.value.toString() })
            .putBoolean("vibrate", s.vibrate).putBoolean("volumeRamp", s.volumeRamp)
            .putInt("reminderMin", s.reminderMin).putBoolean("dnd", s.dnd)
            .apply()
        syncWakeAlarm(s)
        BedtimeScheduler.scheduleAll(context)
    }

    private fun syncWakeAlarm(s: SleepSchedule) {
        val alarms = AlarmRepository(context)
        if (s.wakeAlarm) {
            val alarm = (alarms.get(WAKE_ALARM_ID) ?: Alarm(id = WAKE_ALARM_ID)).copy(
                hour = s.wakeMinutes / 60, minute = s.wakeMinutes % 60, label = "Bedtime", enabled = true,
                repeatDays = s.days, vibrate = s.vibrate, volumeRamp = s.volumeRamp)
            alarms.save(alarm)
            AlarmScheduler.schedule(context, alarm, rollOver = true)
        } else {
            AlarmScheduler.cancel(context, WAKE_ALARM_ID)
            alarms.delete(WAKE_ALARM_ID)
        }
    }

    companion object { const val WAKE_ALARM_ID = "bedtime-wake" }
}
