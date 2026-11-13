// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.screens.alarm

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me2008.alarm.data.Alarm
import me2008.alarm.data.AlarmRepository
import me2008.alarm.service.AlarmScheduler
import me2008.alarm.ui.clockParts
import me2008.alarm.ui.components.ScreenHeader
import me2008.alarm.ui.formatDuration
import me2008.alarm.ui.is24Hour
import me2008.alarm.ui.nextAlarmMillis
import me2008.alarm.ui.repeatSummary

@Composable
fun AlarmListScreen(
    repository: AlarmRepository,
    onAddAlarm: () -> Unit,
    onEditAlarm: (String) -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val is24 = is24Hour(context)
    var alarms by remember { mutableStateOf(repository.getAll()) }
    var pendingDelete by remember { mutableStateOf<Alarm?>(null) }
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    fun refresh() { alarms = repository.getAll() }

    // Refresh the list when coming back, and keep the "Alarm in …" headline current.
    LaunchedEffect(Unit) {
        refresh()
        while (true) { tick = System.currentTimeMillis(); delay(15_000L) }
    }

    val next = remember(alarms, tick) { nextAlarmMillis(alarms) }
    val headline = if (next == null) "No upcoming alarm"
    else "Alarm in ${formatDuration(next - System.currentTimeMillis(), withAnd = false)}"

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 112.dp)) {
            item { ScreenHeader("Alarm", onSettings) }
            item {
                Text(headline, fontSize = 28.sp, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center,
                    lineHeight = 38.sp, color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 36.dp))
            }
            if (alarms.isEmpty()) {
                item {
                    Text("Tap + to add your first alarm", textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(24.dp))
                }
            }
            items(alarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm, is24 = is24,
                    onToggle = {
                        val updated = repository.toggle(alarm.id)
                        if (updated != null) {
                            if (updated.enabled) {
                                AlarmScheduler.schedule(context, updated, rollOver = true)
                                AlarmScheduler.nextTrigger(updated, rollOver = true)?.let {
                                    Toast.makeText(context,
                                        "Alarm will go off in ${formatDuration(it - System.currentTimeMillis(), withAnd = true)}.",
                                        Toast.LENGTH_SHORT).show()
                                }
                            } else AlarmScheduler.cancel(context, alarm.id)
                        }
                        refresh()
                    },
                    onClick = { onEditAlarm(alarm.id) },
                    onLongClick = { pendingDelete = alarm },
                )
            }
        }

        FloatingActionButton(
            onClick = onAddAlarm, shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
        ) { Icon(Icons.Default.Add, contentDescription = "Add alarm") }
    }

    pendingDelete?.let { alarm ->
        AlertDialog(onDismissRequest = { pendingDelete = null },
            title = { Text("Delete alarm?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    AlarmScheduler.cancel(context, alarm.id)
                    repository.delete(alarm.id)
                    pendingDelete = null
                    refresh()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmCard(alarm: Alarm, is24: Boolean, onToggle: () -> Unit, onClick: () -> Unit, onLongClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val fade = if (alarm.enabled) 1f else 0.4f
    val (time, suffix) = clockParts(alarm.hour, alarm.minute, is24)
    val subtitle = if (alarm.label.isNotBlank()) "${alarm.label}, ${repeatSummary(alarm.repeatDays)}"
    else repeatSummary(alarm.repeatDays)

    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)).background(scheme.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(time, fontSize = 40.sp, fontWeight = FontWeight.Normal, color = scheme.onSurface.copy(alpha = fade))
                if (suffix != null) Text(suffix, fontSize = 15.sp, color = scheme.onSurface.copy(alpha = fade),
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
            }
            Text(subtitle, fontSize = 14.sp, color = scheme.onSurfaceVariant.copy(alpha = if (alarm.enabled) 1f else 0.7f))
        }
        Switch(
            checked = alarm.enabled, onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = scheme.primary,
                uncheckedThumbColor = Color.White, uncheckedTrackColor = scheme.outlineVariant,
                uncheckedBorderColor = Color.Transparent, checkedBorderColor = Color.Transparent),
        )
    }
}
