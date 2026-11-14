// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.ui.screens.alarm

import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me2008.alarm.audio.AudioInfo
import me2008.alarm.audio.MixPlayer
import me2008.alarm.data.Alarm
import me2008.alarm.data.AlarmSound
import me2008.alarm.data.SoundLibrary
import me2008.alarm.data.allSounds
import me2008.alarm.data.withSound
import me2008.alarm.data.withoutLayer

private fun fmt(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d.%d".format(s / 60, s % 60, (ms % 1000) / 100)
}

/**
 * Full-screen sound page: the mixer (main sound plus extra layers, each with volume and a trimmed part)
 * and a chooser. Tapping a sound plays it right away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundPage(alarm: Alarm, onChange: (Alarm) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val player = remember { MixPlayer(context) }
    var previewing by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { player.stop() } }

    // index of the sound being chosen (0 = main, size = new layer), or null while on the mixer
    var picking by remember { mutableStateOf<Int?>(null) }
    var trimming by remember { mutableStateOf<Int?>(null) }

    fun stopPreview() { player.stop(); previewing = false }
    fun back() { stopPreview(); if (picking != null) picking = null else onBack() }
    BackHandler { back() }

    Scaffold(
        containerColor = scheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (picking != null) "Choose a sound" else "Sound", fontWeight = FontWeight.Normal) },
                navigationIcon = { IconButton(onClick = { back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        val sel = picking
        if (sel != null) {
            ChooserList(
                modifier = Modifier.padding(padding),
                currentUri = alarm.allSounds().getOrNull(sel)?.uri,
                onPick = { uri, name ->
                    val old = alarm.allSounds().getOrNull(sel)
                    val chosen = AlarmSound(uri, name, old?.volume ?: 100)
                    val updated = alarm.withSound(sel, chosen)
                    onChange(updated)
                    // Play what was just tapped, on its own, so it's easy to judge.
                    player.play(listOf(chosen), 100, 0); previewing = true
                },
                onDone = { stopPreview(); picking = null },
            )
        } else {
            val sounds = alarm.allSounds()
            Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Mixer", fontSize = 13.sp, color = scheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
                sounds.forEachIndexed { i, s ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(scheme.surface).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, null, tint = scheme.primary)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp).clickable { stopPreview(); picking = i }) {
                                Text(s.name, fontSize = 16.sp, color = scheme.onSurface, maxLines = 1)
                                Text(if (i == 0) "Main sound · tap to change" else "Layer $i · tap to change",
                                    fontSize = 12.sp, color = scheme.onSurfaceVariant)
                            }
                            if (i > 0) IconButton(onClick = { stopPreview(); onChange(alarm.withoutLayer(i)) }) {
                                Icon(Icons.Default.Close, "Remove layer", tint = scheme.onSurfaceVariant)
                            }
                        }
                        Text("Volume ${s.volume}%", fontSize = 13.sp, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                        Slider(value = s.volume.toFloat(), onValueChange = {
                            onChange(alarm.withSound(i, s.copy(volume = it.toInt())))
                            if (previewing) player.setVoiceVolume(i, it.toInt())
                        }, valueRange = 0f..100f)
                        val open = trimming == i
                        TextButton(onClick = { trimming = if (open) null else i }) {
                            val part = if (s.startMs > 0 || s.endMs > 0) "Part: ${fmt(s.startMs)} – ${if (s.endMs > 0) fmt(s.endMs) else "end"}" else "Play the whole sound"
                            Text(part)
                        }
                        if (open) TrimEditor(s) { onChange(alarm.withSound(i, it)) }
                    }
                }
                if (sounds.size < 4) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(scheme.surface)
                        .clickable { stopPreview(); picking = sounds.size }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, null, tint = scheme.primary)
                        Text("Add a layer", fontSize = 16.sp, color = scheme.onSurface, modifier = Modifier.padding(start = 12.dp))
                    }
                }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(scheme.surface).padding(16.dp)) {
                    Text("Master volume ${alarm.masterVolume}%", fontSize = 16.sp, color = scheme.onSurface)
                    Slider(value = alarm.masterVolume.toFloat(), onValueChange = {
                        onChange(alarm.copy(masterVolume = it.toInt())); if (previewing) player.setMaster(it.toInt())
                    }, valueRange = 10f..100f)
                }
                Button(onClick = {
                    if (previewing) stopPreview() else { player.play(alarm.allSounds(), alarm.masterVolume, 0); previewing = true }
                }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp)) {
                    Icon(if (previewing) Icons.Default.Stop else Icons.Default.PlayArrow, null)
                    Text(if (previewing) "Stop" else "Play the mix", modifier = Modifier.padding(start = 8.dp))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrimEditor(sound: AlarmSound, onChange: (AlarmSound) -> Unit) {
    val context = LocalContext.current
    val total = remember(sound.uri) { AudioInfo.durationMs(context, sound.uri) }
    if (total <= 0L) {
        Text("This sound's length can't be read, so it can't be trimmed.", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val end = if (sound.endMs in 1..total) sound.endMs else total
    var range by remember(sound.uri) { mutableStateOf(sound.startMs.toFloat()..end.toFloat()) }
    Text("${fmt(range.start.toLong())} – ${fmt(range.endInclusive.toLong())} of ${fmt(total)}", fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    RangeSlider(value = range, onValueChange = { range = it }, valueRange = 0f..total.toFloat(),
        onValueChangeFinished = {
            val e = range.endInclusive.toLong()
            onChange(sound.copy(startMs = range.start.toLong(), endMs = if (e >= total - 50) 0L else e))
        })
    Text("Only this part plays, and it repeats until you stop the alarm.", fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ChooserList(modifier: Modifier, currentUri: String?, onPick: (String, String) -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val library = remember { SoundLibrary(context) }
    var mine by remember { mutableStateOf(library.all()) }
    var selected by remember { mutableStateOf(currentUri) }
    val alarms = remember { AudioInfo.deviceSounds(context, RingtoneManager.TYPE_ALARM) }
    val ringtones = remember { AudioInfo.deviceSounds(context, RingtoneManager.TYPE_RINGTONE) }
    val notifications = remember { AudioInfo.deviceSounds(context, RingtoneManager.TYPE_NOTIFICATION) }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val item = library.import(uri)
            if (item == null) Toast.makeText(context, "Couldn't add that file (max 25 MB, audio only).", Toast.LENGTH_LONG).show()
            else { mine = library.all(); selected = item.uri; onPick(item.uri, item.name) }
        }
    }

    @Composable
    fun SoundRow(name: String, uri: String, onDelete: (() -> Unit)? = null) {
        val isSel = selected == uri
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(if (isSel) scheme.primary.copy(alpha = 0.16f) else scheme.surface)
            .clickable { selected = uri; onPick(uri, name) }.padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isSel) Icons.Default.Check else Icons.Default.MusicNote, null, tint = if (isSel) scheme.primary else scheme.onSurfaceVariant)
            Text(name, fontSize = 15.sp, color = scheme.onSurface, maxLines = 1,
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 10.dp))
            if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = scheme.onSurfaceVariant) }
        }
    }

    @Composable
    fun Header(text: String) = Text(text, fontSize = 13.sp, color = scheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 12.dp))

    Column(modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Header("Built in")
            SoundRow("Crystal chime (default)", "default")
            SoundRow("Phone default alarm", Settings.System.DEFAULT_ALARM_ALERT_URI.toString())
            Header("My sounds")
            mine.forEach { s -> SoundRow(s.name, s.uri) { library.delete(s); mine = library.all() } }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(scheme.surface)
                .clickable { importer.launch(arrayOf("audio/*")) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, null, tint = scheme.primary)
                Text("Add from your files", fontSize = 15.sp, color = scheme.primary, modifier = Modifier.padding(start = 14.dp))
            }
            if (alarms.isNotEmpty()) { Header("Alarm sounds"); alarms.forEach { (u, n) -> SoundRow(n, u.toString()) } }
            if (ringtones.isNotEmpty()) { Header("Ringtones"); ringtones.forEach { (u, n) -> SoundRow(n, u.toString()) } }
            if (notifications.isNotEmpty()) { Header("Notification sounds"); notifications.forEach { (u, n) -> SoundRow(n, u.toString()) } }
            Spacer(Modifier.height(12.dp))
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp), shape = RoundedCornerShape(28.dp)) {
            Text("Done")
        }
    }
}
