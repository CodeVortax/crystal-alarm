// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.screens.clock

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me2008.alarm.data.City
import me2008.alarm.data.Cities
import me2008.alarm.data.WorldClockRepository
import me2008.alarm.ui.components.AnalogClock
import me2008.alarm.ui.components.ScreenHeader
import me2008.alarm.ui.components.SoftTextField
import me2008.alarm.ui.is24Hour
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClockScreen(onSettings: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { WorldClockRepository(context) }
    var zones by remember { mutableStateOf(repo.getAll()) }
    var showAdd by remember { mutableStateOf(false) }
    var toRemove by remember { mutableStateOf<City?>(null) }
    val is24 = is24Hour(context)
    val localZone = remember { ZoneId.systemDefault() }

    // Frame-accurate time for the sweeping second hand; text only reads the whole-second value.
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { withFrameNanos { nowMs = System.currentTimeMillis() } } }
    val nowSecond by remember { derivedStateOf { nowMs / 1000L } }

    val localNow = ZonedDateTime.ofInstant(Instant.ofEpochSecond(nowSecond), localZone)
    val timeFmt = remember(is24) { DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a", Locale.getDefault()) }
    val timeSecFmt = remember(is24) { DateTimeFormatter.ofPattern(if (is24) "HH:mm:ss" else "h:mm:ss a", Locale.getDefault()) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            ScreenHeader("World clock", onSettings, actions = {
                IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "Add city") }
            })
        }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AnalogClock(nowMillis = { nowMs }, modifier = Modifier.fillMaxWidth(0.7f).widthIn(max = 300.dp))
                Spacer(Modifier.height(20.dp))
                Text(localNow.format(timeSecFmt), fontSize = 32.sp, fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground)
                Text("Local time  ${localNow.format(dateFmt)}", fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (zones.isEmpty()) {
            item {
                Text("Tap + to add a city", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(zones, key = { it.key }) { city ->
            val zone = ZoneId.of(city.zoneId)
            val there = ZonedDateTime.ofInstant(Instant.ofEpochSecond(nowSecond), zone)
            val diffMin = (there.offset.totalSeconds - localNow.offset.totalSeconds) / 60
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface)
                    .combinedClickable(onClick = {}, onLongClick = { toRemove = city })
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(city.name, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${there.format(dateFmt)}  |  ${offsetText(diffMin)}", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(there.format(timeFmt), fontSize = 34.sp, fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (zones.isNotEmpty()) {
            item {
                Text("Long-press a city to remove it", fontSize = 12.sp, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
    }

    if (showAdd) AddCityDialog(existing = zones.map { it.key }, onDismiss = { showAdd = false }, onPick = {
        repo.add(it); zones = repo.getAll(); showAdd = false
    })
    toRemove?.let { city ->
        AlertDialog(onDismissRequest = { toRemove = null },
            title = { Text("Remove ${city.name}?") },
            confirmButton = { TextButton(onClick = { repo.remove(city); zones = repo.getAll(); toRemove = null }) {
                Text("Remove", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { toRemove = null }) { Text("Cancel") } })
    }
}

/** "Same as local time", "+9 hrs", "-2 hrs", "+5 hrs 30 min" */
private fun offsetText(diffMin: Int): String {
    if (diffMin == 0) return "Same as local time"
    val sign = if (diffMin > 0) "+" else "-"
    val h = abs(diffMin) / 60
    val m = abs(diffMin) % 60
    val hPart = if (h == 1) "1 hr" else "$h hrs"
    return when {
        h == 0 -> "$sign$m min"
        m == 0 -> "$sign$hPart"
        else -> "$sign$hPart $m min"
    }
}

@Composable
private fun AddCityDialog(existing: List<String>, onDismiss: () -> Unit, onPick: (City) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, existing) { Cities.search(query, existing) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add city") },
        text = {
            Column {
                SoftTextField(value = query, onValueChange = { query = it },
                    placeholder = "Search city or country", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(results.take(100), key = { it.key }) { city ->
                        Column(Modifier.fillMaxWidth().clickable { onPick(city) }.padding(vertical = 10.dp, horizontal = 4.dp)) {
                            Text(city.name, fontSize = 16.sp)
                            Text(city.country, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (results.isEmpty()) item {
                        Text("No matching city", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp))
                    }
                }
            }
        },
        confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}
