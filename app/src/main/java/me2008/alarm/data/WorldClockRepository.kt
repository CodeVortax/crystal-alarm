// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.data

import android.content.Context
import java.time.ZoneId

/** Remembers which cities (time zone ids such as "Europe/Paris") the user added to World clock. */
class WorldClockRepository(context: Context) {
    private val prefs = context.getSharedPreferences("crystal_world_clock", Context.MODE_PRIVATE)

    fun getAll(): List<String> =
        (prefs.getString("zones", "") ?: "").split("|")
            .filter { it.isNotBlank() && runCatching { ZoneId.of(it) }.isSuccess }

    fun add(zoneId: String) {
        val list = getAll()
        if (zoneId !in list) persist(list + zoneId)
    }

    fun remove(zoneId: String) = persist(getAll() - zoneId)

    private fun persist(list: List<String>) {
        prefs.edit().putString("zones", list.joinToString("|")).apply()
    }

    companion object {
        /** "America/Los_Angeles" -> "Los Angeles" */
        fun cityName(zoneId: String): String = zoneId.substringAfterLast('/').replace('_', ' ')
        fun regionName(zoneId: String): String = zoneId.substringBefore('/').replace('_', ' ')

        /** Every selectable city zone, sorted by city name. Skips legacy ids like "EST" and "Etc/GMT+3". */
        fun allCities(): List<String> = ZoneId.getAvailableZoneIds()
            .filter { '/' in it && !it.startsWith("Etc/") && !it.startsWith("SystemV/") && !it.startsWith("US/") }
            .sortedBy { cityName(it) }
    }
}
