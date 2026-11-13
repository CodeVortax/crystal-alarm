// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.screens.timer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me2008.alarm.data.TimerPreset
import me2008.alarm.service.TimerManager
import me2008.alarm.ui.components.CircleButton
import me2008.alarm.ui.components.NumberWheel
import me2008.alarm.ui.components.PillChip
import me2008.alarm.ui.formatHms
import me2008.alarm.ui.is24Hour
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TimerScreen() {
    val state by TimerManager.state.collectAsState()
    // Idle until a timer has been started; running, paused and finished all have a total.
    if (state.totalSeconds == 0L) TimerSetup() else TimerRunning(state.totalSeconds, state.remainingSeconds,
        state.isRunning, state.isFinished)
}

@Composable
private fun TimerSetup() {
    val context = LocalContext.current
    var hh by remember { mutableIntStateOf(0) }
    var mm by remember { mutableIntStateOf(10) }
    var ss by remember { mutableIntStateOf(0) }
    var wheelKey by remember { mutableIntStateOf(0) }
    val total = hh * 3600L + mm * 60L + ss

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        key(wheelKey) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                NumberWheel(0..23, hh, { hh = it }, label = "hr")
                NumberWheel(0..59, mm, { mm = it }, label = "min")
                NumberWheel(0..59, ss, { ss = it }, label = "sec")
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimerPreset.entries.filter { it != TimerPreset.CUSTOM }.forEach { preset ->
                PillChip(text = preset.label, selected = total == preset.seconds, onClick = {
                    hh = (preset.seconds / 3600L).toInt()
                    mm = ((preset.seconds % 3600L) / 60L).toInt()
                    ss = (preset.seconds % 60L).toInt()
                    wheelKey++
                })
            }
        }
        Spacer(Modifier.weight(1f))
        CircleButton(onClick = { TimerManager.start(TimerPreset.CUSTOM, total, context) }, size = 84.dp, primary = true, enabled = total > 0L) {
            Icon(Icons.Default.PlayArrow, "Start timer", modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun TimerRunning(total: Long, remaining: Long, running: Boolean, finished: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val progress by animateFloatAsState(
        targetValue = if (total > 0L) remaining.toFloat() / total.toFloat() else 0f,
        animationSpec = tween(900, easing = LinearEasing), label = "timerProgress")
    val trackColor = scheme.surfaceVariant
    val ringColor = if (finished) scheme.error else scheme.primary
    val endFmt = remember { DateTimeFormatter.ofPattern(if (is24Hour(context)) "HH:mm" else "h:mm a", Locale.getDefault()) }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(290.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                val sw = 14.dp.toPx()
                val d = size.minDimension - sw
                val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                drawArc(trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = Size(d, d), style = Stroke(width = sw))
                if (progress > 0f) drawArc(ringColor, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                    topLeft = topLeft, size = Size(d, d), style = Stroke(width = sw, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatHms(remaining), fontSize = 56.sp, fontWeight = FontWeight.Light,
                    color = if (finished) scheme.error else scheme.onBackground)
                if (finished) Text("Time's up", fontSize = 16.sp, color = scheme.error)
                else Text("Ends at ${LocalTime.now().plusSeconds(remaining).format(endFmt)}", fontSize = 13.sp,
                    color = scheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleButton(onClick = { TimerManager.reset() }, size = 68.dp) {
                Icon(if (finished) Icons.Default.Refresh else Icons.Default.Close, if (finished) "Reset" else "Cancel")
            }
            if (!finished) {
                CircleButton(onClick = { if (running) TimerManager.pause() else TimerManager.resume() }, size = 84.dp, primary = true) {
                    Icon(if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (running) "Pause" else "Resume", modifier = Modifier.size(40.dp))
                }
                CircleButton(onClick = { TimerManager.addSeconds(60L) }, size = 68.dp) {
                    Text("+1:00", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}
