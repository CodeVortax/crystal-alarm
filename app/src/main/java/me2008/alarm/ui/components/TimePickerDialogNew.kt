// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialogNew(
    initialHour: Int, initialMinute: Int,
    onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit,
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Set time") },
        text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = { hour = (hour + 1) % 24 }) { Text("▲", fontSize = 12.sp) }
                    Text(String.format("%02d", hour), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { hour = (hour - 1 + 24) % 24 }) { Text("▼", fontSize = 12.sp) }
                }
                Text(" : ", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = { minute = (minute + 1) % 60 }) { Text("▲", fontSize = 12.sp) }
                    Text(String.format("%02d", minute), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { minute = (minute - 1 + 60) % 60 }) { Text("▼", fontSize = 12.sp) }
                }
            }
        } },
        confirmButton = { TextButton(onClick = { onConfirm(hour, minute) }) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
