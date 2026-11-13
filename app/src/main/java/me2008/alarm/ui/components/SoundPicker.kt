// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.components

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Alarm sound chooser: the built-in defaults first, then every alarm sound installed on the phone.
 * The "Crystal chime" entry is stored as the literal "default", which is what the alarm service expects.
 */
@Composable
fun SoundPicker(
    currentUri: String, currentName: String,
    onDismiss: () -> Unit, onSelected: (Uri, String) -> Unit,
) {
    val context = LocalContext.current
    val defaults = remember {
        listOf(
            Uri.parse("default") to "Crystal chime (default)",
            Settings.System.DEFAULT_ALARM_ALERT_URI to "Default alarm",
            Settings.System.DEFAULT_NOTIFICATION_URI to "Default notification",
            Settings.System.DEFAULT_RINGTONE_URI to "Default ringtone",
        )
    }
    val device = remember { deviceAlarmSounds(context) }
    val scheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alarm sound") },
        text = {
            LazyColumn(Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(defaults.size) { i ->
                    val (uri, name) = defaults[i]
                    SoundRow(name, uri.toString() == currentUri) { onSelected(uri, name); onDismiss() }
                }
                if (device.isNotEmpty()) {
                    item {
                        Text("On this phone", fontSize = 13.sp, color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 4.dp))
                    }
                    items(device.size) { i ->
                        val (uri, name) = device[i]
                        SoundRow(name, uri.toString() == currentUri) { onSelected(uri, name); onDismiss() }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SoundRow(name: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(if (selected) scheme.primary.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.MusicNote, null, tint = if (selected) scheme.primary else scheme.onSurfaceVariant)
        Text(name, fontSize = 15.sp, color = scheme.onSurface, modifier = Modifier.weight(1f).padding(start = 14.dp))
        if (selected) Icon(Icons.Default.Check, "Selected", tint = scheme.primary)
    }
}

/** Alarm sounds installed on the phone (name to uri), without duplicates. Empty if the list can't be read. */
private fun deviceAlarmSounds(context: Context): List<Pair<Uri, String>> {
    val result = mutableListOf<Pair<Uri, String>>()
    try {
        val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
        val cursor = manager.cursor
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: continue
            if (result.none { it.second == title }) result += manager.getRingtoneUri(cursor.position) to title
        }
    } catch (_: Exception) {
        // No readable sound list on this device; the defaults above still work.
    }
    return result
}
