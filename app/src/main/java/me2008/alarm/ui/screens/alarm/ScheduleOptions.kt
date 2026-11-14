// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.screens.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me2008.alarm.data.Alarm
import me2008.alarm.data.isShift
import me2008.alarm.ui.components.PillChip
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DateFmt = DateTimeFormatter.ofPattern("EEE d MMM yyyy")
private fun dayText(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(DateFmt)

/** Shift pattern (N days on, M days off) and skipped dates (holidays) for an alarm. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleOptions(alarm: Alarm, onChange: (Alarm) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var pickStart by remember { mutableStateOf(false) }
    var pickSkip by remember { mutableStateOf(false) }
    val today = LocalDate.now().toEpochDay()
    val shiftEnabled = alarm.shiftOn > 0 || alarm.shiftOff > 0

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(scheme.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Shift schedule", fontSize = 16.sp, color = scheme.onSurface)
                Text("Ring some days, then rest some days, in a repeating cycle. Replaces the weekday choice.",
                    fontSize = 12.sp, color = scheme.onSurfaceVariant)
            }
            Switch(checked = shiftEnabled, onCheckedChange = {
                onChange(if (it) alarm.copy(shiftOn = 5, shiftOff = 2, shiftStartEpochDay = today)
                else alarm.copy(shiftOn = 0, shiftOff = 0))
            })
        }
        if (shiftEnabled) {
            Text("Ring for ${alarm.shiftOn} day${if (alarm.shiftOn == 1) "" else "s"}", fontSize = 14.sp, color = scheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..7).forEach { n -> PillChip("$n", alarm.shiftOn == n, { onChange(alarm.copy(shiftOn = n)) }) }
            }
            Text("Then rest for ${alarm.shiftOff} day${if (alarm.shiftOff == 1) "" else "s"}", fontSize = 14.sp, color = scheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..7).forEach { n -> PillChip("$n", alarm.shiftOff == n, { onChange(alarm.copy(shiftOff = n)) }) }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { pickStart = true }.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("First ringing day", fontSize = 15.sp, color = scheme.onSurface)
                Text(dayText(alarm.shiftStartEpochDay), fontSize = 14.sp, color = scheme.primary)
            }
        }

        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
        Text("Skip dates", fontSize = 16.sp, color = scheme.onSurface)
        Text("The alarm stays quiet on these days, such as holidays.", fontSize = 12.sp, color = scheme.onSurfaceVariant)
        val upcoming = alarm.skipDates.filter { it >= today }.sorted()
        if (upcoming.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            upcoming.forEach { d ->
                InputChip(selected = true, onClick = { onChange(alarm.copy(skipDates = alarm.skipDates - d)) },
                    label = { Text(dayText(d), fontSize = 13.sp) },
                    trailingIcon = { Text("✕", fontSize = 12.sp) }, shape = RoundedCornerShape(50))
            }
        }
        TextButton(onClick = { pickSkip = true }) { Text("Add a date to skip") }
    }

    if (pickStart) DatePickerSheet(alarm.shiftStartEpochDay, onDismiss = { pickStart = false }) {
        onChange(alarm.copy(shiftStartEpochDay = it)); pickStart = false
    }
    if (pickSkip) DatePickerSheet(today, onDismiss = { pickSkip = false }) {
        onChange(alarm.copy(skipDates = alarm.skipDates.filter { d -> d >= today }.toSet() + it)); pickSkip = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(initialEpochDay: Long, onDismiss: () -> Unit, onPicked: (Long) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialEpochDay * 86_400_000L)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onPicked(Math.floorDiv(it, 86_400_000L)) } }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state = state) }
}
