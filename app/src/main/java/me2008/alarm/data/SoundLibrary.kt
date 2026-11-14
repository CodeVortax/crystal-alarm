// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** A sound the user added from their own files. [uri] points at a private copy inside the app. */
data class LibrarySound(val id: String, val name: String, val uri: String)

/** The user's own alarm sounds. Picked files are copied into app storage so they stay playable. */
class SoundLibrary(private val context: Context) {
    private val prefs = context.getSharedPreferences("crystal_sound_library", Context.MODE_PRIVATE)

    fun all(): List<LibrarySound> {
        val arr = try { JSONArray(prefs.getString("items", "[]")) } catch (_: Exception) { JSONArray() }
        return (0 until arr.length()).map { arr.getJSONObject(it) }
            .map { LibrarySound(it.getString("id"), it.getString("name"), it.getString("uri")) }
            .filter { File(Uri.parse(it.uri).path ?: "").exists() }
    }

    /** Copies [source] into the app and adds it to the library, or returns null if it can't be read or is too big. */
    fun import(source: Uri): LibrarySound? {
        return try {
            val display = displayName(source) ?: "Custom sound"
            val dir = File(context.filesDir, "sounds").apply { mkdirs() }
            val id = UUID.randomUUID().toString()
            val ext = display.substringAfterLast('.', "").takeIf { it.length in 1..5 } ?: "audio"
            val file = File(dir, "$id.$ext")
            val input = context.contentResolver.openInputStream(source) ?: return null
            input.use { src -> file.outputStream().use { out -> src.copyTo(out) } }
            if (file.length() > MAX_BYTES) { file.delete(); return null }
            val item = LibrarySound(id, display.substringBeforeLast('.'), Uri.fromFile(file).toString())
            persist(all() + item)
            item
        } catch (_: Exception) {
            null
        }
    }

    fun delete(item: LibrarySound) {
        runCatching { File(Uri.parse(item.uri).path ?: "").delete() }
        persist(all().filter { it.id != item.id })
    }

    private fun displayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }

    private fun persist(list: List<LibrarySound>) {
        val json = JSONArray(list.map { JSONObject().put("id", it.id).put("name", it.name).put("uri", it.uri) })
        prefs.edit().putString("items", json.toString()).apply()
    }

    private companion object { const val MAX_BYTES = 25L * 1024 * 1024 }
}
