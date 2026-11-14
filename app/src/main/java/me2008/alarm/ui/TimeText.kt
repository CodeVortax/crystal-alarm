// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui

import android.content.Context
import me2008.alarm.data.Alarm
import me2008.alarm.data.isShift
import me2008.alarm.data.DayOfWeek
import me2008.alarm.service.AlarmScheduler

/** "17 hours 5 minutes" (header) or "17 hours and 5 minutes" (toast). Whole minutes, rounded down like the stock clock. */
fun formatDuration(ms: Long, withAnd: Boolean): String {
    val totalMin = ms.coerceAtLeast(0L) / 60_000L
    val d = totalMin / 1440
    val h = (totalMin % 1440) / 60
    val m = totalMin % 60
    val parts = buildList {
        if (d > 0) add(if (d == 1L) "1 day" else "$d days")
        if (h > 0) add(if (h == 1L) "1 hour" else "$h hours")
        if (m > 0) add(if (m == 1L) "1 minute" else "$m minutes")
    }
    if (parts.isEmpty()) return "less than a minute"
    return if (withAnd && parts.size > 1) parts.dropLast(1).joinToString(" ") + " and " + parts.last()
    else parts.joinToString(" ")
}

/** The soonest upcoming ring time among enabled alarms, or null if there is none. */
fun nextAlarmMillis(alarms: List<Alarm>): Long? =
    alarms.filter { it.enabled }.mapNotNull { AlarmScheduler.nextTrigger(it) }.minOrNull()

fun repeatSummary(days: Set<DayOfWeek>): String = when {
    days.isEmpty() -> "Once"
    days.size == 7 -> "Daily"
    days == setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY) ->
        "Monday to Friday"
    days == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Sat Sun"
    else -> days.sortedBy { it.value }.joinToString(" ") { it.shortName }
}

fun is24Hour(context: Context): Boolean = android.text.format.DateFormat.is24HourFormat(context)

/** Hour/minute split for big clock text: "06:00" in 24h mode, "6:00" + "AM" in 12h mode. */
fun clockParts(hour: Int, minute: Int, is24: Boolean): Pair<String, String?> =
    if (is24) String.format("%02d:%02d", hour, minute) to null
    else String.format("%d:%02d", if (hour % 12 == 0) 12 else hour % 12, minute) to (if (hour < 12) "AM" else "PM")

/** Stopwatch text split for display: ("01:23", ".45") or ("1:02:03", ".45") once past an hour. */
fun stopwatchParts(ms: Long): Pair<String, String> {
    val total = ms.coerceAtLeast(0L)
    val cs = (total % 1000L) / 10L
    val s = (total / 1000L) % 60L
    val m = (total / 60_000L) % 60L
    val h = total / 3_600_000L
    val main = if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    return main to String.format(".%02d", cs)
}

/** Countdown text: "09:59" or "1:00:00". */
fun formatHms(totalSeconds: Long): String {
    val t = totalSeconds.coerceAtLeast(0L)
    val h = t / 3600L
    val m = (t % 3600L) / 60L
    val s = t % 60L
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}

/** "22:30" in 24h mode, "10:30 PM" in 12h mode. */
fun formatMinuteOfDay(minuteOfDay: Int, is24: Boolean): String {
    val (t, suffix) = clockParts(minuteOfDay / 60, minuteOfDay % 60, is24)
    return if (suffix == null) t else "$t $suffix"
}

/** Minutes between bedtime and wake-up going forward around the clock (0 if the same time). */
fun sleepMinutes(bedMinutes: Int, wakeMinutes: Int): Int = ((wakeMinutes - bedMinutes) % 1440 + 1440) % 1440

/** "8 hr 30 min", "8 hr" or "45 min". */
fun formatSleepDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h hr"
        else -> "$h hr $m min"
    }
}

/** One-line description of when an alarm repeats, including shift patterns and skipped dates. */
fun alarmSummary(alarm: Alarm): String {
    val base = if (alarm.isShift) "Shift: ${alarm.shiftOn} on, ${alarm.shiftOff} off" else repeatSummary(alarm.repeatDays)
    val today = java.time.LocalDate.now().toEpochDay()
    val skips = alarm.skipDates.count { it >= today }
    return if (skips > 0) "$base · skips $skips" else base
}
