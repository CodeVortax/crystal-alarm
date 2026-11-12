// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/** Stores alarms in SharedPreferences as a JSON array. */
class AlarmRepository(context: Context) {
    private val prefs = context.getSharedPreferences("crystal_alarms", Context.MODE_PRIVATE)

    fun getAll(): List<Alarm> {
        val raw = prefs.getString("alarms", null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { alarmFromJson(arr.getJSONObject(it)) }
            .sortedWith(compareBy({ it.hour }, { it.minute }))
    }

    fun getEnabled(): List<Alarm> = getAll().filter { it.enabled }
    fun get(id: String): Alarm? = getAll().find { it.id == id }

    fun save(alarm: Alarm) {
        val all = getAll().toMutableList()
        val idx = all.indexOfFirst { it.id == alarm.id }
        if (idx >= 0) all[idx] = alarm else all.add(alarm)
        persist(all)
    }

    fun delete(id: String) { persist(getAll().filter { it.id != id }) }

    fun toggle(id: String): Alarm? {
        val all = getAll().toMutableList()
        val idx = all.indexOfFirst { it.id == id }
        if (idx < 0) return null
        all[idx] = all[idx].copy(enabled = !all[idx].enabled)
        persist(all)
        return all[idx]
    }

    private fun persist(list: List<Alarm>) {
        val sorted = list.sortedWith(compareBy({ it.hour }, { it.minute }))
        val arr = JSONArray(sorted.map { it.toJson() })
        prefs.edit().putString("alarms", arr.toString()).apply()
    }
}
