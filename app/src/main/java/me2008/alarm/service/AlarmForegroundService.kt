// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import me2008.alarm.AlarmAlertActivity
import me2008.alarm.audio.MixPlayer
import me2008.alarm.audio.Vibrate
import me2008.alarm.data.AlarmRepository
import me2008.alarm.data.allSounds

/**
 * Foreground service that plays alarm sound + vibration and shows the alert activity.
 * Call [start] to fire, [stop] to dismiss.
 */
class AlarmForegroundService : Service() {
    private var mix: MixPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(EXTRA_ALARM_ID) ?: return START_NOT_STICKY
        startForeground(NOTIFICATION_ID, buildNotification(id))
        startActivity(Intent(this, AlarmAlertActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        })
        playAlarm(id)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** Runs whenever the service ends, so Stop/Dismiss always silences the sound as well as the vibration. */
    override fun onDestroy() {
        silence()
        super.onDestroy()
    }

    private fun silence() {
        mix?.stop(); mix = null
        vibrator?.cancel()
    }

    private fun playAlarm(id: String) {
        val alarm = AlarmRepository(this).get(id) ?: return
        silence()
        mix = MixPlayer(this).also {
            it.play(alarm.allSounds(), alarm.masterVolume, if (alarm.volumeRamp) 15 else 0)
        }
        if (alarm.vibrate) Vibrate.start(vibrator, alarm.vibrationPattern, alarm.vibrationCustom, alarm.vibrationStrength)
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Alarm playing",
            NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Shown while an alarm rings"
            setSound(null, null); enableVibration(false)
        })
    }

    private fun buildNotification(alarmId: String): Notification {
        val alarm = AlarmRepository(this).get(alarmId)
        val openIntent = Intent(this, AlarmAlertActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Crystal").setContentText(alarm?.label ?: "Alarm")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(PendingIntent.getActivity(this, 0, openIntent, flags), true)
            .setOngoing(true).build()
    }

    companion object {
        const val CHANNEL_ID = "crystal_alarm_playing"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_ALARM_ID = "crystal_alarm_id"
        fun start(context: Context, id: String) {
            context.startForegroundService(Intent(context, AlarmForegroundService::class.java).apply {
                putExtra(EXTRA_ALARM_ID, id)
            })
        }
        fun stop(context: Context) { context.stopService(Intent(context, AlarmForegroundService::class.java)) }
    }
}
