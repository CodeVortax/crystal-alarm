// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.screens.clock

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me2008.alarm.ui.GlassClockFace

@Composable
fun ClockScreen() {
    Box(Modifier.fillMaxSize().padding(top = 16.dp), contentAlignment = Alignment.Center) {
        GlassClockFace(modifier = Modifier.fillMaxWidth(0.85f).fillMaxHeight(0.55f), showDigital = true)
    }
}
