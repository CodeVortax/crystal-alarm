// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me2008.alarm.data.AlarmRepository
import me2008.alarm.ui.screens.alarm.AlarmEditScreen
import me2008.alarm.ui.screens.alarm.AlarmListScreen
import me2008.alarm.ui.screens.clock.ClockScreen
import me2008.alarm.ui.screens.settings.SettingsScreen
import me2008.alarm.ui.screens.stopwatch.StopwatchScreen
import me2008.alarm.ui.screens.timer.TimerScreen
import me2008.alarm.ui.theme.AlarmTheme

/** The four main tabs. */
enum class Tab(val label: String, val icon: ImageVector) {
    CLOCK("Clock", Icons.Outlined.Alarm),
    ALARMS("Alarms", Icons.Outlined.Alarm),
    TIMER("Timer", Icons.Outlined.Timer),
    STOPWATCH("Stopwatch", Icons.Outlined.HourglassEmpty),
}

sealed interface Screen {
    data object Main : Screen
    data class EditAlarm(val alarmId: String?) : Screen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmApp() {
    AlarmTheme {
        val context = LocalContext.current
        val repository = remember { AlarmRepository(context) }
        var screen by remember { mutableStateOf<Screen>(Screen.Main) }
        var currentTab by remember { mutableStateOf(Tab.CLOCK) }
        var showSettings by remember { mutableStateOf(false) }

        when (val s = screen) {
            is Screen.EditAlarm -> AlarmEditScreen(
                alarmId = s.alarmId, repository = repository, onBack = { screen = Screen.Main }
            )
            is Screen.Main -> Scaffold(
                topBar = {
                    TopAppBar(title = { Text("Crystal", fontWeight = FontWeight.Light, fontSize = 24.sp, letterSpacing = 4.sp) },
                        actions = { IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(Icons.Default.Settings, "Settings",
                                tint = if (showSettings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        } },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onBackground))
                },
                bottomBar = {
                    if (!showSettings) NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)) {
                        Tab.entries.forEach { tab ->
                            NavigationBarItem(selected = currentTab == tab, onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.label) }, label = { Text(tab.label, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)))
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    if (showSettings) SettingsScreen(onBack = { showSettings = false })
                    else AnimatedContent(targetState = currentTab, transitionSpec = {
                        val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (slideInHorizontally { it * dir } + fadeIn()) togetherWith (slideOutHorizontally { it * -dir } + fadeOut())
                    }, label = "tabContent") { tab ->
                        when (tab) {
                            Tab.CLOCK -> ClockScreen()
                            Tab.ALARMS -> AlarmListScreen(repository = repository,
                                onAddAlarm = { screen = Screen.EditAlarm(null) },
                                onEditAlarm = { screen = Screen.EditAlarm(it) })
                            Tab.TIMER -> TimerScreen()
                            Tab.STOPWATCH -> StopwatchScreen()
                        }
                    }
                }
            }
        }
    }
}
