// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.screens.stopwatch

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me2008.alarm.service.StopwatchManager
import me2008.alarm.ui.components.CircleButton
import me2008.alarm.ui.stopwatchParts

@Composable
fun StopwatchScreen() {
    val scheme = MaterialTheme.colorScheme
    val running = StopwatchManager.running
    val laps = StopwatchManager.laps
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(running) {
        while (running) { now = SystemClock.elapsedRealtime(); delay(33L) }
        now = SystemClock.elapsedRealtime()
    }
    val elapsed = StopwatchManager.elapsed(now)
    val (main, centis) = stopwatchParts(elapsed)

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.padding(top = 24.dp, bottom = 16.dp), verticalAlignment = Alignment.Bottom) {
            Text(main, fontSize = 72.sp, fontWeight = FontWeight.Light, color = scheme.onBackground)
            Text(centis, fontSize = 40.sp, fontWeight = FontWeight.Light, color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp))
        }

        // Laps, newest first. Fastest lap in green and slowest in red once there are three or more.
        val splits = laps.mapIndexed { i, t -> t - (if (i > 0) laps[i - 1] else 0L) }
        val fastest = if (splits.size >= 3) splits.indices.minByOrNull { splits[it] } else null
        val slowest = if (splits.size >= 3) splits.indices.maxByOrNull { splits[it] } else null
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (laps.isEmpty()) {
                Text("Tap the flag to record a lap", fontSize = 14.sp, color = scheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 28.dp, vertical = 8.dp)) {
                    items(laps.indices.reversed().toList(), key = { it }) { i ->
                        val tint = when (i) { fastest -> Color(0xFF34C759); slowest -> scheme.error; else -> scheme.onSurface }
                        val (sMain, sCs) = stopwatchParts(splits[i])
                        val (tMain, tCs) = stopwatchParts(laps[i])
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Lap ${i + 1}", fontSize = 15.sp, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            Text(sMain + sCs, fontSize = 17.sp, color = tint, modifier = Modifier.width(96.dp))
                            Text(tMain + tCs, fontSize = 17.sp, color = scheme.onSurface)
                        }
                    }
                }
            }
        }

        Row(Modifier.padding(top = 16.dp, bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(40.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (running) {
                CircleButton(onClick = { StopwatchManager.lap() }, size = 72.dp) { Icon(Icons.Default.Flag, "Lap") }
            } else {
                CircleButton(onClick = { StopwatchManager.reset() }, size = 72.dp, enabled = elapsed > 0L) {
                    Icon(Icons.Default.Refresh, "Reset")
                }
            }
            CircleButton(onClick = { if (running) StopwatchManager.pause() else StopwatchManager.start() },
                size = 88.dp, primary = true) {
                Icon(if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (running) "Pause" else "Start", modifier = Modifier.size(40.dp))
            }
        }
    }
}
