// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Type-free exact time entry: two snapping wheels, hours (0-23) and minutes (0-59). */
@Composable
fun TimePickerDialogNew(
    initialHour: Int, initialMinute: Int,
    onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit,
) {
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set time") },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                NumberWheel(0..23, hour, { hour = it }, label = "hr")
                NumberWheel(0..59, minute, { minute = it }, label = "min")
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(hour, minute) }) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
