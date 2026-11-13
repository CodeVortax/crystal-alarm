// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me2008.alarm.data.AlarmRepository
import me2008.alarm.service.AlarmForegroundService
import me2008.alarm.service.AlarmReceiver
import me2008.alarm.service.AlarmScheduler
import me2008.alarm.ui.theme.AlarmTheme
import kotlin.random.Random

/** Fullscreen activity shown when an alarm fires. */
class AlarmAlertActivity : ComponentActivity() {
    private var alarmId: String? = null
    private var puzzleAnswer: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmId = intent.getStringExtra(AlarmForegroundService.EXTRA_ALARM_ID)
        val alarm = alarmId?.let { AlarmRepository(this).get(it) }
        if (alarm?.puzzleDismiss == true) puzzleAnswer = genPuzzle()

        setContent { AlarmTheme { AlertScreen(alarmId, puzzleAnswer, ::snooze, ::dismiss) } }
    }

    private fun genPuzzle(): Int {
        val a = Random.nextInt(1, 20); val b = Random.nextInt(1, 20)
        val op = Random.nextInt(3); val ans = when (op) { 0 -> a + b; 1 -> a - b; else -> a * b }
        puzzleText = when (op) { 0 -> "$a + $b = ?"; 1 -> "$a - $b = ?"; else -> "$a × $b = ?" }
        return ans
    }

    private fun snooze(min: Int) {
        val alarm = alarmId?.let { AlarmRepository(this).get(it) } ?: return
        val intent = Intent(this, AlarmReceiver::class.java).apply { putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id) }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
        val pi = PendingIntent.getBroadcast(this, 2000 + alarm.id.hashCode() % 1000, intent, flags)
        (getSystemService(ALARM_SERVICE) as android.app.AlarmManager)
            .setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + min * 60_000L, pi)
        AlarmForegroundService.stop(this); AlarmScheduler.schedule(this, alarm); disableIfOneShot(alarm); finish()
    }

    /** A one-time alarm has done its job once it rang, so switch it off in the list. */
    private fun disableIfOneShot(alarm: me2008.alarm.data.Alarm) {
        if (alarm.repeatDays.isEmpty()) AlarmRepository(this).save(alarm.copy(enabled = false))
    }

    private fun dismiss() {
        AlarmForegroundService.stop(this)
        alarmId?.let { AlarmRepository(this).get(it) }?.let { AlarmScheduler.schedule(this, it); disableIfOneShot(it) }
        finish()
    }

    companion object { var puzzleText: String = "?" }
}

@Composable
private fun AlertScreen(alarmId: String?, puzzleAnswer: Int?, onSnooze: (Int) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val alarm = alarmId?.let { AlarmRepository(context).get(it) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(if (alarm != null) String.format("%02d:%02d", alarm.hour, alarm.minute) else "--:--",
                fontSize = 72.sp, fontWeight = FontWeight.Light, letterSpacing = 6.sp, color = MaterialTheme.colorScheme.onBackground)
            if (alarm?.label?.isNotBlank() == true) Text(alarm.label, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(48.dp))

            if (puzzleAnswer != null) {
                var answer by remember { mutableStateOf("") }; var wrong by remember { mutableStateOf(false) }
                Text("Solve to dismiss:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(AlarmAlertActivity.puzzleText, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = answer, onValueChange = { answer = it.filter { c -> c.isDigit() || c == '-' }; wrong = false },
                    label = { Text("Answer") }, singleLine = true, isError = wrong, modifier = Modifier.width(150.dp), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center))
                if (wrong) Text("Wrong answer, try again", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { if (answer.toIntOrNull() == puzzleAnswer) onDismiss() else wrong = true }, modifier = Modifier.fillMaxWidth(0.5f)) { Text("Dismiss") }
            } else {
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(0.6f).height(56.dp)) { Text("Dismiss", fontSize = 18.sp) }
            }

            Spacer(Modifier.height(20.dp))
            Text("Snooze", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                intArrayOf(5, 10, 15, 30).forEach { min -> OutlinedButton(onClick = { onSnooze(min) }) { Text("${min}m") } }
            }
        }
    }
}
