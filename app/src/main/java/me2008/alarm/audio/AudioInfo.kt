// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings

/** Small helpers for looking at sound files. */
object AudioInfo {
    /** The literal "default" means the phone's default alarm sound; everything else is a real uri. */
    fun resolve(uri: String): Uri =
        if (uri == "default" || uri.isBlank()) Settings.System.DEFAULT_ALARM_ALERT_URI else Uri.parse(uri)

    /** Length of the sound in milliseconds, or 0 if it can't be read. */
    fun durationMs(context: Context, uri: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, resolve(uri))
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** Sounds the phone ships with for [type] (RingtoneManager.TYPE_ALARM, TYPE_RINGTONE or TYPE_NOTIFICATION). */
    fun deviceSounds(context: Context, type: Int): List<Pair<Uri, String>> {
        val result = mutableListOf<Pair<Uri, String>>()
        try {
            val manager = RingtoneManager(context).apply { setType(type) }
            val cursor = manager.cursor
            while (cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: continue
                if (result.none { it.second == title }) result += manager.getRingtoneUri(cursor.position) to title
            }
        } catch (_: Exception) {
            // No readable sound list on this device.
        }
        return result
    }
}
