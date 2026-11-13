// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.data

import android.content.Context
import java.time.ZoneId

/** Remembers which cities the user added to World clock. */
class WorldClockRepository(context: Context) {
    private val prefs = context.getSharedPreferences("crystal_world_clock", Context.MODE_PRIVATE)

    fun getAll(): List<City> {
        val raw = prefs.getString("cities", null)
        val cities = if (raw != null) {
            raw.split(";").mapNotNull { entry ->
                val name = entry.substringBefore('|', "")
                val zone = entry.substringAfter('|', "")
                // Country is looked up again so the stored text stays tiny.
                if (name.isBlank() || zone.isBlank()) null
                else City(name, Cities.all.firstOrNull { it.name == name && it.zoneId == zone }?.country
                    ?: Cities.zoneRegion(zone), zone)
            }
        } else {
            // Older builds stored plain zone ids separated by "|".
            (prefs.getString("zones", "") ?: "").split("|").filter { it.isNotBlank() }
                .map { City(Cities.zoneName(it), Cities.zoneRegion(it), it) }
        }
        return cities.filter { runCatching { ZoneId.of(it.zoneId) }.isSuccess }
    }

    fun add(city: City) {
        val list = getAll()
        if (list.none { it.key == city.key }) persist(list + city)
    }

    fun remove(city: City) = persist(getAll().filter { it.key != city.key })

    private fun persist(list: List<City>) {
        prefs.edit().putString("cities", list.joinToString(";") { "${it.name}|${it.zoneId}" }).apply()
    }
}
