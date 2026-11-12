// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.screens.alarm

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me2008.alarm.data.Alarm
import me2008.alarm.data.AlarmRepository
import me2008.alarm.service.AlarmScheduler

@Composable
fun AlarmListScreen(
    repository: AlarmRepository,
    onAddAlarm: () -> Unit,
    onEditAlarm: (String) -> Unit,
) {
    val context = LocalContext.current
    var alarms by remember { mutableStateOf(repository.getAll()) }
    fun refresh() { alarms = repository.getAll() }
    LaunchedEffect(Unit) { refresh() }

    if (alarms.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No alarms yet", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(onClick = onAddAlarm) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Add alarm")
                }
            }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(alarms, key = { it.id }) { alarm ->
                AlarmCard(alarm = alarm,
                    onToggle = {
                        val updated = repository.toggle(alarm.id)
                        if (updated != null) {
                            if (updated.enabled) AlarmScheduler.schedule(context, updated)
                            else AlarmScheduler.cancel(context, alarm.id)
                        }
                        refresh()
                    },
                    onClick = { onEditAlarm(alarm.id) },
                    onDelete = {
                        AlarmScheduler.cancel(context, alarm.id)
                        repository.delete(alarm.id)
                        refresh()
                    }
                )
            }
        }
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, onToggle: () -> Unit, onClick: () -> Unit, onDelete: () -> Unit) {
    val hour12 = if (alarm.hour % 12 == 0) 12 else alarm.hour % 12
    val amPm = if (alarm.hour < 12) "AM" else "PM"
    val displayTime = "$hour12:${String.format("%02d", alarm.minute)} $amPm"
    val daysText = if (alarm.repeatDays.isEmpty()) "Once"
    else alarm.repeatDays.sortedBy { it.value }.joinToString(" ") { it.shortName }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(displayTime, fontSize = 28.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp,
                    color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                if (alarm.label.isNotBlank()) Text(alarm.label, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(daysText, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (alarm.vibrate) Text("📳", fontSize = 12.sp)
                    if (alarm.puzzleDismiss) Text("🧩", fontSize = 12.sp)
                    if (alarm.volumeRamp) Text("📈", fontSize = 12.sp)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(checked = alarm.enabled, onCheckedChange = { onToggle() })
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Delete", Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }
        }
    }
}
