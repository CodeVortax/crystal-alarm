// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.screens.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me2008.alarm.data.TimerPreset
import me2008.alarm.service.TimerManager

@Composable
fun TimerScreen() {
    val state by TimerManager.state.collectAsState()
    var customSeconds by remember { mutableStateOf("600") }

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {

        val progress = animateFloatAsState(
            targetValue = if (state.totalSeconds > 0) state.remainingSeconds.toFloat() / state.totalSeconds.toFloat() else 1f,
            animationSpec = tween(500), label = "progress")

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
            Canvas(Modifier.size(250.dp)) {
                val sw = 16f; val r = (size.minDimension - sw) / 2f
                val tl = Offset((size.width - r * 2f) / 2f, (size.height - r * 2f) / 2f)
                drawCircle(color = Color.Gray.copy(alpha = 0.15f), radius = r + sw / 2f, style = Stroke(width = sw, cap = StrokeCap.Round))
                drawArc(color = MaterialTheme.colorScheme.primary, startAngle = -90f,
                    sweepAngle = 360f * (1f - progress.value), useCenter = false,
                    topLeft = tl, size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                    style = Stroke(width = sw, cap = StrokeCap.Round))
            }
            val m = state.remainingSeconds / 60; val s = state.remainingSeconds % 60
            Text(String.format("%02d:%02d", m, s), fontSize = 48.sp, fontWeight = FontWeight.Light,
                letterSpacing = 4.sp, color = if (state.isFinished) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground)
        }

        Spacer(Modifier.height(16.dp))
        if (state.label.isNotBlank()) Text(state.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (!state.isRunning && !state.isFinished) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimerPreset.entries.dropLast(1).forEach { preset ->
                    FilterChip(selected = state.totalSeconds == preset.seconds,
                        onClick = { TimerManager.start(preset) }, label = { Text(preset.label, fontSize = 12.sp) })
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = customSeconds, onValueChange = { customSeconds = it.filter { c -> c.isDigit() } },
                    label = { Text("Seconds") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(120.dp), singleLine = true)
                Button(onClick = { val secs = customSeconds.toLongOrNull() ?: return@Button; if (secs > 0) TimerManager.start(TimerPreset.CUSTOM, secs) }) { Text("Go") }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.isFinished) { Button(onClick = { TimerManager.reset() }) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Reset") } }
            else if (state.isRunning) { FilledTonalButton(onClick = { TimerManager.pause() }) { Icon(Icons.Default.Pause, null); Spacer(Modifier.width(6.dp)); Text("Pause") }
                OutlinedButton(onClick = { TimerManager.addSeconds(60) }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("+1 min") } }
            else if (state.totalSeconds > 0) { Button(onClick = { TimerManager.resume() }) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Resume") }
                OutlinedButton(onClick = { TimerManager.reset() }) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Reset") } }
        }
        if (state.isFinished) { Spacer(Modifier.height(16.dp)); Text("⏰ Time's up!", fontSize = 20.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
    }
}
