// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui

import android.content.Context
import me2008.alarm.data.Alarm
import me2008.alarm.data.DayOfWeek
import me2008.alarm.service.AlarmScheduler

/** "17 hours 5 minutes" (header) or "17 hours and 5 minutes" (toast). Rounds up to the next minute. */
fun formatDuration(ms: Long, withAnd: Boolean): String {
    val totalMin = (ms.coerceAtLeast(0L) + 59_999L) / 60_000L
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
