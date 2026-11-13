// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.screens.alarm

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import me2008.alarm.data.Alarm
import me2008.alarm.data.AlarmRepository
import me2008.alarm.data.DayOfWeek
import me2008.alarm.service.AlarmScheduler
import me2008.alarm.ui.clockParts
import me2008.alarm.ui.components.CircularTimePicker
import me2008.alarm.ui.components.PillChip
import me2008.alarm.ui.components.SoftTextField
import me2008.alarm.ui.components.SoundPicker
import me2008.alarm.ui.components.TimePickerDialogNew
import me2008.alarm.ui.formatDuration
import me2008.alarm.ui.is24Hour
import me2008.alarm.ui.repeatSummary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditScreen(alarmId: String?, repository: AlarmRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    val is24 = is24Hour(context)
    val existing = alarmId?.let { repository.get(it) }
    var alarm by remember { mutableStateOf(existing ?: Alarm()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showSoundPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }
    val isNew = existing == null

    fun save() {
        // Saving always turns the alarm on, like the stock clock apps.
        val toSave = alarm.copy(enabled = true)
        repository.save(toSave)
        AlarmScheduler.schedule(context, toSave, rollOver = true)
        AlarmScheduler.nextTrigger(toSave, rollOver = true)?.let {
            Toast.makeText(context, "Alarm will go off in ${formatDuration(it - System.currentTimeMillis(), withAnd = true)}.",
                Toast.LENGTH_SHORT).show()
        }
        onBack()
    }
    LaunchedEffect(deleted) { if (deleted) onBack() }

    val untilMs = AlarmScheduler.nextTrigger(alarm.copy(enabled = true), rollOver = true)
    val (time, suffix) = clockParts(alarm.hour, alarm.minute, is24)
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = scheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isNew) "New alarm" else "Edit alarm", fontWeight = FontWeight.Normal) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.Close, "Cancel") } },
                actions = { TextButton(onClick = { save() }) { Text("Save", fontWeight = FontWeight.SemiBold) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Big time (tap for exact entry) + countdown
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.clip(RoundedCornerShape(16.dp)).clickable { showTimePicker = true }.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.Bottom) {
                    Text(time, fontSize = 64.sp, fontWeight = FontWeight.Light, color = scheme.onBackground)
                    if (suffix != null) Text(suffix, fontSize = 20.sp, color = scheme.onBackground,
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
                }
                Text(if (untilMs != null) "Rings in ${formatDuration(untilMs - System.currentTimeMillis(), withAnd = false)}"
                else "Pick the days to repeat", fontSize = 14.sp, color = scheme.onSurfaceVariant)
            }

            CircularTimePicker(hour = alarm.hour, minute = alarm.minute,
                onTimeChange = { h, m -> alarm = alarm.copy(hour = h, minute = m) },
                modifier = Modifier.padding(horizontal = 8.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Drag the bell, or fine-tune by the minute. Tap the time to type it.", fontSize = 12.sp,
                    textAlign = TextAlign.Center, color = scheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(-5, -1, 1, 5).forEach { delta ->
                        PillChip(text = if (delta > 0) "+$delta" else "\u2212${-delta}", selected = false, onClick = {
                            val t = ((alarm.hour * 60 + alarm.minute + delta) % 1440 + 1440) % 1440
                            alarm = alarm.copy(hour = t / 60, minute = t % 60)
                        })
                    }
                }
            }

            // Label + repeat
            OptionGroup {
                SoftTextField(value = alarm.label, onValueChange = { alarm = alarm.copy(label = it) },
                    placeholder = "Label", modifier = Modifier.fillMaxWidth().padding(16.dp))
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Repeat", fontSize = 16.sp, color = scheme.onSurface)
                        Text(repeatSummary(alarm.repeatDays), fontSize = 14.sp, color = scheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DayOfWeek.entries.forEach { day ->
                            val sel = day in alarm.repeatDays
                            Box(
                                Modifier.size(38.dp).clip(CircleShape)
                                    .background(if (sel) scheme.primary else scheme.surfaceVariant)
                                    .clickable {
                                        alarm = alarm.copy(repeatDays = if (sel) alarm.repeatDays - day else alarm.repeatDays + day)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(day.shortName.take(1), fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                    color = if (sel) scheme.onPrimary else scheme.onSurface)
                            }
                        }
                    }
                }
            }

            // Sound + toggles
            OptionGroup {
                Row(Modifier.fillMaxWidth().clickable { showSoundPicker = true }.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Sound", fontSize = 16.sp, color = scheme.onSurface)
                    Text(alarm.soundName, fontSize = 14.sp, color = scheme.onSurfaceVariant)
                }
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                SettingsToggle("Vibrate", null, alarm.vibrate) { alarm = alarm.copy(vibrate = it) }
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                SettingsToggle("Gradually louder", "Volume ramps up while it rings", alarm.volumeRamp) { alarm = alarm.copy(volumeRamp = it) }
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                SettingsToggle("Puzzle to dismiss", "Solve a math problem to stop it", alarm.puzzleDismiss) { alarm = alarm.copy(puzzleDismiss = it) }
            }

            // Snooze
            OptionGroup {
                Column(Modifier.padding(16.dp)) {
                    Text("Snooze", fontSize = 16.sp, color = scheme.onSurface)
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        intArrayOf(1, 3, 5, 10, 15, 20, 30).forEach { min ->
                            PillChip(text = "$min min", selected = alarm.snoozeMinutes == min,
                                onClick = { alarm = alarm.copy(snoozeMinutes = min) })
                        }
                    }
                }
            }

            if (!isNew) {
                TextButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Delete alarm", color = scheme.error)
                }
            }
            Spacer(Modifier.height(24.dp))
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

/** A rounded card that groups related rows, like the "Alarm Options" block in the sleep-schedule screen. */
@Composable
internal fun OptionGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface), content = content)
}

@Composable
internal fun SettingsToggle(label: String, description: String?, checked: Boolean, onToggle: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, fontSize = 16.sp, color = scheme.onSurface)
            if (description != null) Text(description, fontSize = 12.sp, color = scheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onToggle, colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = scheme.primary,
            uncheckedThumbColor = Color.White, uncheckedTrackColor = scheme.outlineVariant,
            uncheckedBorderColor = Color.Transparent, checkedBorderColor = Color.Transparent))
    }
}
