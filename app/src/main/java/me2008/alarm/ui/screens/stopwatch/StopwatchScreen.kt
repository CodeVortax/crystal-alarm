// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.screens.stopwatch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me2008.alarm.data.StopwatchSession

@Composable
fun StopwatchScreen() {
    var session by remember { mutableStateOf(StopwatchSession()) }

    LaunchedEffect(session.isRunning) {
        if (session.isRunning) {
            val start = System.currentTimeMillis() - session.elapsedMs
            while (true) { delay(50L); session = session.copy(elapsedMs = System.currentTimeMillis() - start) }
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(48.dp))
        val ts = session.elapsedMs / 1000; val h = ts / 3600; val m = (ts % 3600) / 60; val s = ts % 60; val ms = (session.elapsedMs % 1000) / 10
        Text(if (h > 0) String.format("%d:%02d:%02d.%02d", h, m, s, ms) else String.format("%02d:%02d.%02d", m, s, ms),
            fontSize = 42.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!session.isRunning) Button(onClick = { session = session.copy(isRunning = true) }) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Start") }
            else FilledTonalButton(onClick = { session = session.copy(isRunning = false) }) { Icon(Icons.Default.Pause, null); Spacer(Modifier.width(6.dp)); Text("Stop") }
            FilledTonalButton(onClick = { if (session.isRunning) session = session.copy(laps = session.laps + session.elapsedMs) }) { Icon(Icons.Default.Flag, null); Spacer(Modifier.width(6.dp)); Text("Lap") }
            OutlinedButton(onClick = { session = StopwatchSession() }) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Reset") }
        }

        Spacer(Modifier.height(24.dp))
        if (session.laps.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Laps", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(session.laps.reversed()) { lapMs ->
                    val ls = lapMs / 1000; val lm = (ls % 3600) / 60; val lsec = ls % 60; val lms = (lapMs % 1000) / 10
                    ListItem(headlineContent = {
                        Text(if (lm > 0) String.format("Lap %d: %02d:%02d.%02d", session.laps.size - session.laps.reversed().indexOf(lapMs), lm, lsec, lms)
                        else String.format("Lap %d: %02d.%02d", session.laps.size - session.laps.reversed().indexOf(lapMs), lsec, lms),
                            fontSize = 14.sp, fontWeight = FontWeight.Light)
                    })
                }
            }
        } else { Spacer(Modifier.weight(1f)); Text("Tap Lap to record a split", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.weight(1f)) }
    }
}
