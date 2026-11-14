// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.screens.alarm

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me2008.alarm.audio.Vibrate
import me2008.alarm.data.Alarm
import me2008.alarm.data.VibrationPattern

/** Full-screen vibration page: pick a rhythm (tap to feel it), set strength, or tap out your own rhythm. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibrationPage(alarm: Alarm, onChange: (Alarm) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val vibrator = remember { Vibrate.get(context) }
    val hasStrength = remember { android.os.Build.VERSION.SDK_INT >= 26 && vibrator.hasAmplitudeControl() }
    DisposableEffect(Unit) { onDispose { vibrator.cancel() } }
    BackHandler { vibrator.cancel(); onBack() }

    fun feel(a: Alarm) { vibrator.cancel(); Vibrate.start(vibrator, a.vibrationPattern, a.vibrationCustom, a.vibrationStrength, repeat = false) }

    // Recording state for the custom rhythm (milliseconds: on, off, on, ...)
    var recorded by remember { mutableStateOf(Vibrate.parseCustom(alarm.vibrationCustom).toList()) }
    var lastRelease by remember { mutableLongStateOf(0L) }

    Scaffold(
        containerColor = scheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Vibration", fontWeight = FontWeight.Normal) },
                navigationIcon = { IconButton(onClick = { vibrator.cancel(); onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Pattern · tap one to feel it", fontSize = 13.sp, color = scheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(scheme.surface).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                VibrationPattern.entries.forEach { p ->
                    val sel = alarm.vibrationPattern == p.id
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(if (sel) scheme.primary.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable { val a = alarm.copy(vibrationPattern = p.id); onChange(a); feel(a) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(p.label, fontSize = 16.sp, color = scheme.onSurface, modifier = Modifier.weight(1f))
                        if (sel) Icon(Icons.Default.Check, "Selected", tint = scheme.primary)
                    }
                }
            }

            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(scheme.surface).padding(16.dp)) {
                Text("Strength ${alarm.vibrationStrength}%", fontSize = 16.sp, color = scheme.onSurface)
                Text(if (hasStrength) "How hard it buzzes" else "This phone's motor only has one strength, so this has no effect here.",
                    fontSize = 12.sp, color = scheme.onSurfaceVariant)
                Slider(value = alarm.vibrationStrength.toFloat(), enabled = hasStrength,
                    onValueChange = { onChange(alarm.copy(vibrationStrength = it.toInt())) },
                    onValueChangeFinished = { feel(alarm) }, valueRange = 10f..100f)
            }

            if (alarm.vibrationPattern == VibrationPattern.CUSTOM.id) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(scheme.surface).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tap your rhythm below. Hold for a long buzz, tap quickly for short ones.", fontSize = 14.sp,
                        color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(28.dp))
                        .background(scheme.primary.copy(alpha = 0.16f))
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = {
                                val down = System.currentTimeMillis()
                                if (lastRelease != 0L && recorded.isNotEmpty() && recorded.size % 2 == 1) {
                                    recorded = recorded + (down - lastRelease).coerceIn(20L, 3000L)
                                }
                                if (recorded.size >= 20) return@detectTapGestures
                                vibrator.cancel()
                                vibrator.vibrate(android.os.VibrationEffect.createOneShot(3000L, android.os.VibrationEffect.DEFAULT_AMPLITUDE)) // feedback while held
                                tryAwaitRelease()
                                vibrator.cancel()
                                val up = System.currentTimeMillis()
                                if (recorded.size % 2 == 0) recorded = recorded + (up - down).coerceIn(20L, 3000L)
                                lastRelease = up
                            })
                        }, contentAlignment = Alignment.Center) {
                        Text(if (recorded.isEmpty()) "Tap here" else "${(recorded.size + 1) / 2} buzzes recorded",
                            color = scheme.primary, fontSize = 18.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                        OutlinedButton(onClick = { recorded = emptyList(); lastRelease = 0L; onChange(alarm.copy(vibrationCustom = "")) }) { Text("Clear") }
                        Button(onClick = {
                            // Drop a dangling gap so the list always ends on a buzz.
                            val list = if (recorded.size % 2 == 0 && recorded.isNotEmpty()) recorded.dropLast(1) else recorded
                            val a = alarm.copy(vibrationCustom = list.joinToString(","))
                            onChange(a); feel(a)
                        }, enabled = recorded.isNotEmpty()) { Text("Save & feel it") }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
