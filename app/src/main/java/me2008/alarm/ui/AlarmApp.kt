// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me2008.alarm.data.AlarmRepository
import me2008.alarm.ui.components.ScreenHeader
import me2008.alarm.ui.screens.alarm.AlarmEditScreen
import me2008.alarm.ui.screens.alarm.AlarmListScreen
import me2008.alarm.ui.screens.alarm.BedtimeScreen
import me2008.alarm.ui.screens.clock.ClockScreen
import me2008.alarm.ui.screens.settings.SettingsScreen
import me2008.alarm.ui.screens.stopwatch.StopwatchScreen
import me2008.alarm.ui.screens.timer.TimerScreen
import me2008.alarm.ui.theme.AlarmTheme

/** The four main tabs, in the order of the bottom bar. */
enum class Tab(val label: String, val icon: ImageVector) {
    ALARM("Alarm", Icons.Outlined.Alarm),
    WORLD_CLOCK("World clock", Icons.Outlined.Schedule),
    STOPWATCH("Stopwatch", Icons.Outlined.Timer),
    TIMER("Timer", Icons.Outlined.HourglassEmpty),
}

sealed interface Screen {
    data object Main : Screen
    data class EditAlarm(val alarmId: String?) : Screen
    data object Settings : Screen
    data object Bedtime : Screen
}

@Composable
fun AlarmApp() {
    AlarmTheme {
        val context = LocalContext.current
        val repository = remember { AlarmRepository(context) }
        var screen by remember { mutableStateOf<Screen>(Screen.Main) }
        var currentTab by remember { mutableStateOf(Tab.ALARM) }

        // System back from the editor or settings returns to the tabs instead of leaving the app.
        BackHandler(enabled = screen !is Screen.Main) { screen = Screen.Main }

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (val s = screen) {
                is Screen.EditAlarm -> AlarmEditScreen(
                    alarmId = s.alarmId, repository = repository, onBack = { screen = Screen.Main })
                is Screen.Settings -> SettingsScreen(onBack = { screen = Screen.Main })
                is Screen.Bedtime -> BedtimeScreen(onBack = { screen = Screen.Main })
                is Screen.Main -> Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = { BottomBar(currentTab) { currentTab = it } },
                ) { padding ->
                    val openSettings = { screen = Screen.Settings }
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        AnimatedContent(targetState = currentTab, transitionSpec = {
                            val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                            (slideInHorizontally { it * dir } + fadeIn()) togetherWith (slideOutHorizontally { it * -dir } + fadeOut())
                        }, label = "tabContent") { tab ->
                            when (tab) {
                                Tab.ALARM -> AlarmListScreen(repository = repository,
                                    onAddAlarm = { screen = Screen.EditAlarm(null) },
                                    onEditAlarm = { screen = Screen.EditAlarm(it) },
                                    onBedtime = { screen = Screen.Bedtime },
                                    onSettings = openSettings)
                                Tab.WORLD_CLOCK -> ClockScreen(onSettings = openSettings)
                                Tab.STOPWATCH -> Column(Modifier.fillMaxSize()) {
                                    ScreenHeader("Stopwatch", openSettings)
                                    Box(Modifier.weight(1f)) { StopwatchScreen() }
                                }
                                Tab.TIMER -> Column(Modifier.fillMaxSize()) {
                                    ScreenHeader("Timer", openSettings)
                                    Box(Modifier.weight(1f)) { TimerScreen() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    NavigationBar(containerColor = scheme.background, tonalElevation = 0.dp) {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                selected = current == tab, onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, fontSize = 11.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = scheme.onBackground, selectedTextColor = scheme.onBackground,
                    unselectedIconColor = scheme.onSurfaceVariant, unselectedTextColor = scheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent),
            )
        }
    }
}
