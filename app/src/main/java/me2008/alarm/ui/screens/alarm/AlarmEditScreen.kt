// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.screens.alarm

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import me2008.alarm.data.DayOfWeek
import me2008.alarm.service.AlarmScheduler
import me2008.alarm.ui.components.SoundPicker
import me2008.alarm.ui.components.TimePickerDialogNew

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(alarmId: String?, repository: AlarmRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    val existing = alarmId?.let { repository.get(it) }
    var alarm by remember { mutableStateOf(existing ?: Alarm()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showSoundPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }
    val isNew = existing == null
    fun save() { repository.save(alarm); AlarmScheduler.schedule(context, alarm); onBack() }
    LaunchedEffect(deleted) { if (deleted) onBack() }

    Scaffold(topBar = {
        TopAppBar(title = { Text(if (isNew) "New alarm" else "Edit alarm", fontWeight = FontWeight.Light) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            actions = {
                if (!isNew) IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                TextButton(onClick = { save() }) { Text("Done", fontWeight = FontWeight.Bold) }
            })
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(String.format("%02d : %02d", alarm.hour, alarm.minute), fontSize = 48.sp,
                    fontWeight = FontWeight.Light, letterSpacing = 8.sp, color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider()
            OutlinedTextField(value = alarm.label, onValueChange = { alarm = alarm.copy(label = it) },
                label = { Text("Label") }, placeholder = { Text("Wake up") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Repeat", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DayOfWeek.entries.forEach { day ->
                    val sel = day in alarm.repeatDays
                    FilterChip(selected = sel, onClick = {
                        alarm = alarm.copy(repeatDays = if (sel) alarm.repeatDays - day else alarm.repeatDays + day)
                    }, label = { Text(day.shortName, fontSize = 12.sp) })
                }
            }
            Text("Sound", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedCard(onClick = { showSoundPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(alarm.soundName, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurface)
            }
            SettingsToggle("Vibrate", alarm.vibrate) { alarm = alarm.copy(vibrate = it) }
            SettingsToggle("Volume ramp (gradually louder)", alarm.volumeRamp) { alarm = alarm.copy(volumeRamp = it) }
            SettingsToggle("Puzzle dismiss (solve a math problem)", alarm.puzzleDismiss) { alarm = alarm.copy(puzzleDismiss = it) }
            Text("Snooze duration", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                intArrayOf(1, 3, 5, 10, 15, 30).forEach { min ->
                    FilterChip(selected = alarm.snoozeMinutes == min,
                        onClick = { alarm = alarm.copy(snoozeMinutes = min) }, label = { Text("${min}m", fontSize = 12.sp) })
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
    if (showTimePicker) TimePickerDialogNew(initialHour = alarm.hour, initialMinute = alarm.minute,
        onDismiss = { showTimePicker = false }, onConfirm = { h, m -> alarm = alarm.copy(hour = h, minute = m); showTimePicker = false })
    if (showSoundPicker) SoundPicker(currentUri = alarm.soundUri, currentName = alarm.soundName,
        onDismiss = { showSoundPicker = false }, onSelected = { uri, name ->
            alarm = alarm.copy(soundUri = uri.toString(), soundName = name); showSoundPicker = false })
    if (showDeleteConfirm) AlertDialog(onDismissRequest = { showDeleteConfirm = false },
        title = { Text("Delete alarm?") }, text = { Text("This cannot be undone.") },
        confirmButton = { TextButton(onClick = {
            AlarmScheduler.cancel(context, alarm.id); repository.delete(alarm.id); deleted = true; showDeleteConfirm = false
        }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } })
}

@Composable private fun SettingsToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
