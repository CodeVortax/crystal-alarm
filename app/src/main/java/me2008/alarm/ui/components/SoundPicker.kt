// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.components

import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SoundPicker(
    currentUri: String, currentName: String,
    onDismiss: () -> Unit, onSelected: (Uri, String) -> Unit,
) {
    val context = LocalContext.current
    val sounds = remember {
        mutableListOf(
            Uri.EMPTY to "Crystal chime (default)",
            Settings.System.DEFAULT_ALARM_ALERT_URI to "Default alarm",
            Settings.System.DEFAULT_NOTIFICATION_URI to "Default notification",
            Settings.System.DEFAULT_RINGTONE_URI to "Default ringtone",
        )
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Alarm sound") },
        text = { LazyColumn(modifier = Modifier.height(250.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(sounds) { (uri, name) ->
                val isSelected = uri.toString() == currentUri
                ListItem(
                    headlineContent = { Text(name, fontSize = 14.sp) },
                    leadingContent = { Icon(Icons.Default.MusicNote, null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = ListItemDefaults.colors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surface),
                    modifier = Modifier.clickable { onSelected(uri, name); onDismiss() }
                )
            }
        } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
