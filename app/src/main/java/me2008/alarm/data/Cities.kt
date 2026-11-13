// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.data

import java.time.ZoneId

/** A city shown in World clock. [zoneId] is a java.time zone id such as "America/Los_Angeles". */
data class City(val name: String, val country: String, val zoneId: String) {
    /** Stable identity used for storage and list keys. */
    val key: String get() = "$name|$zoneId"
}

/** A hand-written list of well-known cities, so e.g. "Las Vegas" can be found and not only zone ids. */
object Cities {
    private fun c(name: String, country: String, zone: String) = City(name, country, zone)

    private val curated: List<City> = listOf(
        // Middle East & Africa
        c("Cairo", "Egypt", "Africa/Cairo"), c("Alexandria", "Egypt", "Africa/Cairo"),
        c("Giza", "Egypt", "Africa/Cairo"), c("Sharm El Sheikh", "Egypt", "Africa/Cairo"),
        c("Riyadh", "Saudi Arabia", "Asia/Riyadh"), c("Jeddah", "Saudi Arabia", "Asia/Riyadh"),
        c("Mecca", "Saudi Arabia", "Asia/Riyadh"), c("Medina", "Saudi Arabia", "Asia/Riyadh"),
        c("Dubai", "United Arab Emirates", "Asia/Dubai"), c("Abu Dhabi", "United Arab Emirates", "Asia/Dubai"),
        c("Doha", "Qatar", "Asia/Qatar"), c("Kuwait City", "Kuwait", "Asia/Kuwait"),
        c("Manama", "Bahrain", "Asia/Bahrain"), c("Muscat", "Oman", "Asia/Muscat"),
        c("Baghdad", "Iraq", "Asia/Baghdad"), c("Amman", "Jordan", "Asia/Amman"),
        c("Beirut", "Lebanon", "Asia/Beirut"), c("Damascus", "Syria", "Asia/Damascus"),
        c("Jerusalem", "Israel", "Asia/Jerusalem"), c("Tel Aviv", "Israel", "Asia/Jerusalem"),
        c("Tehran", "Iran", "Asia/Tehran"), c("Sanaa", "Yemen", "Asia/Aden"),
        c("Istanbul", "Turkey", "Europe/Istanbul"), c("Ankara", "Turkey", "Europe/Istanbul"),
        c("Casablanca", "Morocco", "Africa/Casablanca"), c("Rabat", "Morocco", "Africa/Casablanca"),
        c("Algiers", "Algeria", "Africa/Algiers"), c("Tunis", "Tunisia", "Africa/Tunis"),
        c("Tripoli", "Libya", "Africa/Tripoli"), c("Khartoum", "Sudan", "Africa/Khartoum"),
        c("Addis Ababa", "Ethiopia", "Africa/Addis_Ababa"), c("Nairobi", "Kenya", "Africa/Nairobi"),
        c("Lagos", "Nigeria", "Africa/Lagos"), c("Accra", "Ghana", "Africa/Accra"),
        c("Dakar", "Senegal", "Africa/Dakar"), c("Johannesburg", "South Africa", "Africa/Johannesburg"),
        c("Cape Town", "South Africa", "Africa/Johannesburg"), c("Kinshasa", "DR Congo", "Africa/Kinshasa"),
        // Europe
        c("London", "United Kingdom", "Europe/London"), c("Manchester", "United Kingdom", "Europe/London"),
        c("Edinburgh", "United Kingdom", "Europe/London"), c("Dublin", "Ireland", "Europe/Dublin"),
        c("Paris", "France", "Europe/Paris"), c("Marseille", "France", "Europe/Paris"),
        c("Berlin", "Germany", "Europe/Berlin"), c("Munich", "Germany", "Europe/Berlin"),
        c("Frankfurt", "Germany", "Europe/Berlin"), c("Hamburg", "Germany", "Europe/Berlin"),
        c("Madrid", "Spain", "Europe/Madrid"), c("Barcelona", "Spain", "Europe/Madrid"),
        c("Lisbon", "Portugal", "Europe/Lisbon"), c("Rome", "Italy", "Europe/Rome"),
        c("Milan", "Italy", "Europe/Rome"), c("Venice", "Italy", "Europe/Rome"),
        c("Amsterdam", "Netherlands", "Europe/Amsterdam"), c("Brussels", "Belgium", "Europe/Brussels"),
        c("Zurich", "Switzerland", "Europe/Zurich"), c("Geneva", "Switzerland", "Europe/Zurich"),
        c("Vienna", "Austria", "Europe/Vienna"), c("Prague", "Czechia", "Europe/Prague"),
        c("Warsaw", "Poland", "Europe/Warsaw"), c("Budapest", "Hungary", "Europe/Budapest"),
        c("Stockholm", "Sweden", "Europe/Stockholm"), c("Oslo", "Norway", "Europe/Oslo"),
        c("Copenhagen", "Denmark", "Europe/Copenhagen"), c("Helsinki", "Finland", "Europe/Helsinki"),
        c("Athens", "Greece", "Europe/Athens"), c("Bucharest", "Romania", "Europe/Bucharest"),
        c("Kyiv", "Ukraine", "Europe/Kiev"), c("Moscow", "Russia", "Europe/Moscow"),
        c("Saint Petersburg", "Russia", "Europe/Moscow"), c("Reykjavik", "Iceland", "Atlantic/Reykjavik"),
        // North America
        c("New York", "United States", "America/New_York"), c("Washington, D.C.", "United States", "America/New_York"),
        c("Boston", "United States", "America/New_York"), c("Miami", "United States", "America/New_York"),
        c("Atlanta", "United States", "America/New_York"), c("Philadelphia", "United States", "America/New_York"),
        c("Chicago", "United States", "America/Chicago"), c("Houston", "United States", "America/Chicago"),
        c("Dallas", "United States", "America/Chicago"), c("New Orleans", "United States", "America/Chicago"),
        c("Denver", "United States", "America/Denver"), c("Phoenix", "United States", "America/Phoenix"),
        c("Las Vegas", "United States", "America/Los_Angeles"), c("Los Angeles", "United States", "America/Los_Angeles"),
        c("San Francisco", "United States", "America/Los_Angeles"), c("San Diego", "United States", "America/Los_Angeles"),
        c("Seattle", "United States", "America/Los_Angeles"), c("Portland", "United States", "America/Los_Angeles"),
        c("Anchorage", "United States", "America/Anchorage"), c("Honolulu", "United States", "Pacific/Honolulu"),
        c("Toronto", "Canada", "America/Toronto"), c("Montreal", "Canada", "America/Toronto"),
        c("Ottawa", "Canada", "America/Toronto"), c("Vancouver", "Canada", "America/Vancouver"),
        c("Calgary", "Canada", "America/Edmonton"), c("Winnipeg", "Canada", "America/Winnipeg"),
        c("Mexico City", "Mexico", "America/Mexico_City"), c("Havana", "Cuba", "America/Havana"),
        c("Panama City", "Panama", "America/Panama"),
        // South America
        c("São Paulo", "Brazil", "America/Sao_Paulo"), c("Rio de Janeiro", "Brazil", "America/Sao_Paulo"),
        c("Brasília", "Brazil", "America/Sao_Paulo"), c("Buenos Aires", "Argentina", "America/Argentina/Buenos_Aires"),
        c("Santiago", "Chile", "America/Santiago"), c("Lima", "Peru", "America/Lima"),
        c("Bogotá", "Colombia", "America/Bogota"), c("Caracas", "Venezuela", "America/Caracas"),
        c("Quito", "Ecuador", "America/Guayaquil"), c("Montevideo", "Uruguay", "America/Montevideo"),
        // Asia
        c("Mumbai", "India", "Asia/Kolkata"), c("Delhi", "India", "Asia/Kolkata"),
        c("Bangalore", "India", "Asia/Kolkata"), c("Kolkata", "India", "Asia/Kolkata"),
        c("Chennai", "India", "Asia/Kolkata"), c("Hyderabad", "India", "Asia/Kolkata"),
        c("Karachi", "Pakistan", "Asia/Karachi"), c("Lahore", "Pakistan", "Asia/Karachi"),
        c("Islamabad", "Pakistan", "Asia/Karachi"), c("Dhaka", "Bangladesh", "Asia/Dhaka"),
        c("Kabul", "Afghanistan", "Asia/Kabul"), c("Kathmandu", "Nepal", "Asia/Kathmandu"),
        c("Colombo", "Sri Lanka", "Asia/Colombo"), c("Tashkent", "Uzbekistan", "Asia/Tashkent"),
        c("Almaty", "Kazakhstan", "Asia/Almaty"), c("Baku", "Azerbaijan", "Asia/Baku"),
        c("Tbilisi", "Georgia", "Asia/Tbilisi"), c("Yerevan", "Armenia", "Asia/Yerevan"),
        c("Bangkok", "Thailand", "Asia/Bangkok"), c("Hanoi", "Vietnam", "Asia/Ho_Chi_Minh"),
        c("Ho Chi Minh City", "Vietnam", "Asia/Ho_Chi_Minh"), c("Jakarta", "Indonesia", "Asia/Jakarta"),
        c("Kuala Lumpur", "Malaysia", "Asia/Kuala_Lumpur"), c("Singapore", "Singapore", "Asia/Singapore"),
        c("Manila", "Philippines", "Asia/Manila"), c("Hong Kong", "China", "Asia/Hong_Kong"),
        c("Beijing", "China", "Asia/Shanghai"), c("Shanghai", "China", "Asia/Shanghai"),
        c("Shenzhen", "China", "Asia/Shanghai"), c("Guangzhou", "China", "Asia/Shanghai"),
        c("Taipei", "Taiwan", "Asia/Taipei"), c("Seoul", "South Korea", "Asia/Seoul"),
        c("Tokyo", "Japan", "Asia/Tokyo"), c("Osaka", "Japan", "Asia/Tokyo"),
        c("Kyoto", "Japan", "Asia/Tokyo"), c("Ulaanbaatar", "Mongolia", "Asia/Ulaanbaatar"),
        // Oceania
        c("Sydney", "Australia", "Australia/Sydney"), c("Melbourne", "Australia", "Australia/Melbourne"),
        c("Brisbane", "Australia", "Australia/Brisbane"), c("Perth", "Australia", "Australia/Perth"),
        c("Adelaide", "Australia", "Australia/Adelaide"), c("Auckland", "New Zealand", "Pacific/Auckland"),
        c("Wellington", "New Zealand", "Pacific/Auckland"), c("Fiji", "Fiji", "Pacific/Fiji"),
    )

    /** Curated cities (dropping any whose zone id this device does not know), then every other zone by its own name. */
    val all: List<City> by lazy {
        val valid = curated.filter { runCatching { ZoneId.of(it.zoneId) }.isSuccess }
        val names = valid.map { it.name.lowercase() }.toSet()
        val extra = ZoneId.getAvailableZoneIds()
            .filter { '/' in it && !it.startsWith("Etc/") && !it.startsWith("SystemV/") && !it.startsWith("US/") }
            .map { City(zoneName(it), zoneRegion(it), it) }
            .filter { it.name.lowercase() !in names }
            .sortedBy { it.name }
        valid.sortedBy { it.name } + extra
    }

    fun search(query: String, exclude: Collection<String>): List<City> {
        val q = query.trim()
        return all.filter { it.key !in exclude && (q.isEmpty() ||
            it.name.contains(q, ignoreCase = true) || it.country.contains(q, ignoreCase = true)) }
    }

    fun zoneName(zoneId: String): String = zoneId.substringAfterLast('/').replace('_', ' ')
    fun zoneRegion(zoneId: String): String = zoneId.substringBefore('/').replace('_', ' ')
}
