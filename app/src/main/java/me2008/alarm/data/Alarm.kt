// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A single alarm. Stored as JSON. */
data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int = 7,
    val minute: Int = 0,
    val label: String = "",
    val enabled: Boolean = true,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val soundUri: String = "default",
    val soundName: String = "Crystal chime",
    val vibrate: Boolean = true,
    val volumeRamp: Boolean = true,
    val puzzleDismiss: Boolean = false,
    val snoozeMinutes: Int = 5,
    val smartSnooze: Boolean = false,
)

enum class DayOfWeek(val value: Int, val shortName: String) {
    MONDAY(1, "Mon"),
    TUESDAY(2, "Tue"),
    WEDNESDAY(3, "Wed"),
    THURSDAY(4, "Thu"),
    FRIDAY(5, "Fri"),
    SATURDAY(6, "Sat"),
    SUNDAY(7, "Sun");
}

fun Alarm.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("hour", hour); put("minute", minute)
    put("label", label); put("enabled", enabled)
    put("repeatDays", JSONArray(repeatDays.map { it.value }))
    put("soundUri", soundUri); put("soundName", soundName)
    put("vibrate", vibrate); put("volumeRamp", volumeRamp)
    put("puzzleDismiss", puzzleDismiss); put("snoozeMinutes", snoozeMinutes)
    put("smartSnooze", smartSnooze)
}

fun alarmFromJson(obj: JSONObject): Alarm = Alarm(
    id = obj.optString("id", UUID.randomUUID().toString()),
    hour = obj.optInt("hour", 7),
    minute = obj.optInt("minute", 0),
    label = obj.optString("label", ""),
    enabled = obj.optBoolean("enabled", true),
    repeatDays = run {
        val arr = obj.optJSONArray("repeatDays") ?: JSONArray()
        (0 until arr.length()).map { DayOfWeek.entries.first { d -> d.value == arr.getInt(it) } }.toSet()
    },
    soundUri = obj.optString("soundUri", "default"),
    soundName = obj.optString("soundName", "Crystal chime"),
    vibrate = obj.optBoolean("vibrate", true),
    volumeRamp = obj.optBoolean("volumeRamp", true),
    puzzleDismiss = obj.optBoolean("puzzleDismiss", false),
    snoozeMinutes = obj.optInt("snoozeMinutes", 5),
    smartSnooze = obj.optBoolean("smartSnooze", false),
)
