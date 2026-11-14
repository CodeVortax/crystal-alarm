// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

/** One night. [estimated] is true when no "I'm going to bed" tap was recorded and the planned bedtime was used. */
data class SleepEntry(val bedMs: Long, val wakeMs: Long, val estimated: Boolean) {
    val minutes: Int get() = ((wakeMs - bedMs) / 60_000L).toInt()
}

/** Remembers recent nights: bedtime from the bedtime notification, wake-up from dismissing the wake-up alarm. */
class SleepHistory(private val context: Context) {
    private val prefs = context.getSharedPreferences("crystal_sleep", Context.MODE_PRIVATE)

    /** Newest night first. */
    fun entries(): List<SleepEntry> {
        val arr = try { JSONArray(prefs.getString("entries", "[]")) } catch (_: Exception) { JSONArray() }
        return (0 until arr.length()).map { arr.getJSONObject(it) }
            .map { SleepEntry(it.getLong("bed"), it.getLong("wake"), it.optBoolean("estimated")) }
            .sortedByDescending { it.wakeMs }
    }

    fun markBedNow() { prefs.edit().putLong("pendingBed", System.currentTimeMillis()).apply() }

    fun recordWake(schedule: SleepSchedule) {
        val now = System.currentTimeMillis()
        val pending = prefs.getLong("pendingBed", 0L)
        val tapped = pending in (now - 16 * 3_600_000L)..(now - 2 * 3_600_000L)
        val bed = if (tapped) pending else plannedBedBefore(now, schedule.bedMinutes)
        prefs.edit().remove("pendingBed").apply()
        val minutes = (now - bed) / 60_000L
        val list = entries()
        if (minutes < 120 || minutes > 16 * 60) return
        if (list.firstOrNull()?.let { now - it.wakeMs < 4 * 3_600_000L } == true) return
        val kept = (listOf(SleepEntry(bed, now, !tapped)) + list).take(60)
        val json = JSONArray(kept.map { JSONObject().put("bed", it.bedMs).put("wake", it.wakeMs).put("estimated", it.estimated) })
        prefs.edit().putString("entries", json.toString()).apply()
    }

    private fun plannedBedBefore(now: Long, bedMinutes: Int): Long {
        val zone = ZoneId.systemDefault()
        var t = LocalDate.now(zone).atTime(bedMinutes / 60, bedMinutes % 60).atZone(zone).toInstant().toEpochMilli()
        if (t > now) t -= 24 * 3_600_000L
        return t
    }
}
