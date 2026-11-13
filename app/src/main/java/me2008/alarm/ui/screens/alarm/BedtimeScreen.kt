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
import me2008.alarm.data.AlarmRepository
import me2008.alarm.data.BedtimeRepository
import me2008.alarm.data.DayOfWeek
import me2008.alarm.service.AlarmScheduler
import me2008.alarm.ui.components.SleepDial
import me2008.alarm.ui.formatDuration
import me2008.alarm.ui.formatMinuteOfDay
import me2008.alarm.ui.formatSleepDuration
import me2008.alarm.ui.is24Hour
import me2008.alarm.ui.repeatSummary
import me2008.alarm.ui.sleepMinutes

/** Sleep schedule: choose bedtime and wake-up on the dial; saving sets a matching wake-up alarm. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BedtimeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { BedtimeRepository(context) }
    val scheme = MaterialTheme.colorScheme
    val is24 = is24Hour(context)
    val initial = remember { repo.load() }
    var bed by remember { mutableIntStateOf(initial.bedMinutes) }
    var wake by remember { mutableIntStateOf(initial.wakeMinutes) }
    var wakeAlarm by remember { mutableStateOf(initial.wakeAlarm) }
    var days by remember { mutableStateOf(initial.days) }
    var vibrate by remember { mutableStateOf(initial.vibrate) }
    var ramp by remember { mutableStateOf(initial.volumeRamp) }

    val sleep = sleepMinutes(bed, wake)
    val meetsGoal = sleep in 420..540 // 7 to 9 hours

    fun save() {
        repo.save(initial.copy(bedMinutes = bed, wakeMinutes = wake, wakeAlarm = wakeAlarm, days = days,
            vibrate = vibrate, volumeRamp = ramp))
        if (wakeAlarm) {
            AlarmRepository(context).get(BedtimeRepository.WAKE_ALARM_ID)
                ?.let { AlarmScheduler.nextTrigger(it, rollOver = true) }
                ?.let {
                    Toast.makeText(context, "Alarm will go off in ${formatDuration(it - System.currentTimeMillis(), withAnd = true)}.",
                        Toast.LENGTH_SHORT).show()
                }
        }
        onBack()
    }

    Scaffold(
        containerColor = scheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Sleep schedule", fontWeight = FontWeight.Normal) },
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
            SleepDial(bedMinutes = bed, wakeMinutes = wake, onBedChange = { bed = it }, onWakeChange = { wake = it },
                modifier = Modifier.padding(horizontal = 8.dp))

            // Times and total sleep, like the summary under the dial in the Health app
            OptionGroup {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Bedtime", fontSize = 13.sp, color = scheme.onSurfaceVariant)
                        Text(formatMinuteOfDay(bed, is24), fontSize = 24.sp, fontWeight = FontWeight.Medium, color = scheme.onSurface)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Wake up", fontSize = 13.sp, color = scheme.onSurfaceVariant)
                        Text(formatMinuteOfDay(wake, is24), fontSize = 24.sp, fontWeight = FontWeight.Medium, color = scheme.onSurface)
                    }
                }
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatSleepDuration(sleep), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                    Text(if (meetsGoal) "This schedule meets your sleep goal." else "Most adults need 7 to 9 hours of sleep.",
                        fontSize = 14.sp, textAlign = TextAlign.Center,
                        color = if (meetsGoal) scheme.primary else scheme.onSurfaceVariant)
                }
            }

            Text("Alarm Options", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = scheme.onBackground,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp))
            OptionGroup {
                SettingsToggle("Wake Up Alarm", "Rings at your wake-up time", wakeAlarm) { wakeAlarm = it }
                if (wakeAlarm) {
                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Repeat", fontSize = 16.sp, color = scheme.onSurface)
                            Text(repeatSummary(days), fontSize = 14.sp, color = scheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DayOfWeek.entries.forEach { day ->
                                val sel = day in days
                                Box(
                                    Modifier.size(38.dp).clip(CircleShape)
                                        .background(if (sel) scheme.primary else scheme.surfaceVariant)
                                        .clickable { days = if (sel) days - day else days + day },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(day.shortName.take(1), fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                        color = if (sel) scheme.onPrimary else scheme.onSurface)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsToggle("Vibrate", null, vibrate) { vibrate = it }
                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsToggle("Gradually louder", "Volume ramps up while it rings", ramp) { ramp = it }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
