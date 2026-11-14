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
    // Sound mixer: the main sound (soundUri/soundName) plus optional extra layers, all played together.
    val soundVolume: Int = 100,
    val soundStartMs: Long = 0L,
    val soundEndMs: Long = 0L,
    val layers: List<AlarmSound> = emptyList(),
    val masterVolume: Int = 100,
    // Vibration: a named pattern (or "custom" with a recorded rhythm) and its strength in percent.
    val vibrationPattern: String = "pulse",
    val vibrationCustom: String = "",
    val vibrationStrength: Int = 100,
    // Shift pattern: ring [shiftOn] days, then stay silent [shiftOff] days, starting from [shiftStartEpochDay]. 0 = off.
    val shiftOn: Int = 0,
    val shiftOff: Int = 0,
    val shiftStartEpochDay: Long = 0L,
    // Days (as epoch days) on which the alarm stays quiet, e.g. holidays.
    val skipDates: Set<Long> = emptySet(),
)

val Alarm.isShift: Boolean get() = shiftOn > 0 && shiftOff > 0

/** One sound in an alarm's mix. [endMs] of 0 means "play to the end"; [startMs] trims the beginning. */
data class AlarmSound(
    val uri: String,
    val name: String,
    val volume: Int = 100,
    val startMs: Long = 0L,
    val endMs: Long = 0L,
)

/** The main sound followed by any extra layers. */
fun Alarm.allSounds(): List<AlarmSound> =
    listOf(AlarmSound(soundUri, soundName, soundVolume, soundStartMs, soundEndMs)) + layers

/** Replaces sound [index] (0 = main). An index past the end adds a new layer. */
fun Alarm.withSound(index: Int, s: AlarmSound): Alarm = when {
    index <= 0 -> copy(soundUri = s.uri, soundName = s.name, soundVolume = s.volume, soundStartMs = s.startMs, soundEndMs = s.endMs)
    index - 1 < layers.size -> copy(layers = layers.mapIndexed { i, old -> if (i == index - 1) s else old })
    else -> copy(layers = layers + s)
}

fun Alarm.withoutLayer(index: Int): Alarm =
    if (index >= 1 && index - 1 < layers.size) copy(layers = layers.filterIndexed { i, _ -> i != index - 1 }) else this

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
    put("soundVolume", soundVolume); put("soundStartMs", soundStartMs); put("soundEndMs", soundEndMs)
    put("layers", JSONArray(layers.map {
        JSONObject().put("uri", it.uri).put("name", it.name).put("volume", it.volume)
            .put("startMs", it.startMs).put("endMs", it.endMs)
    }))
    put("masterVolume", masterVolume)
    put("vibrationPattern", vibrationPattern); put("vibrationCustom", vibrationCustom)
    put("vibrationStrength", vibrationStrength)
    put("shiftOn", shiftOn); put("shiftOff", shiftOff); put("shiftStart", shiftStartEpochDay)
    put("skipDates", JSONArray(skipDates.toList()))
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
    soundVolume = obj.optInt("soundVolume", 100),
    soundStartMs = obj.optLong("soundStartMs", 0L),
    soundEndMs = obj.optLong("soundEndMs", 0L),
    layers = run {
        val arr = obj.optJSONArray("layers") ?: JSONArray()
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            AlarmSound(o.optString("uri"), o.optString("name"), o.optInt("volume", 100),
                o.optLong("startMs", 0L), o.optLong("endMs", 0L))
        }
    },
    masterVolume = obj.optInt("masterVolume", 100),
    vibrationPattern = obj.optString("vibrationPattern", "pulse"),
    vibrationCustom = obj.optString("vibrationCustom", ""),
    vibrationStrength = obj.optInt("vibrationStrength", 100),
    shiftOn = obj.optInt("shiftOn", 0),
    shiftOff = obj.optInt("shiftOff", 0),
    shiftStartEpochDay = obj.optLong("shiftStart", 0L),
    skipDates = run {
        val arr = obj.optJSONArray("skipDates") ?: JSONArray()
        (0 until arr.length()).map { arr.getLong(it) }.toSet()
    },
)
