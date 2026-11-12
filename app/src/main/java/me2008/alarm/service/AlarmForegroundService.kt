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
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import me2008.alarm.AlarmAlertActivity
import me2008.alarm.data.AlarmRepository

/**
 * Foreground service that plays alarm sound + vibration and shows the alert activity.
 * Call [start] to fire, [stop] to dismiss.
 */
class AlarmForegroundService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var volumeRampJob: Thread? = null

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

    fun stopAlarm() {
        volumeRampJob?.interrupt()
        mediaPlayer?.apply { if (isPlaying) stop(); release() }; mediaPlayer = null
        vibrator?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun playAlarm(id: String) {
        val alarm = AlarmRepository(this).get(id) ?: return
        val soundUri = if (alarm.soundUri == "default") Settings.System.DEFAULT_ALARM_ALERT_URI
        else Uri.parse(alarm.soundUri)
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                setDataSource(this@AlarmForegroundService, soundUri)
                isLooping = true
                setVolume(if (alarm.volumeRamp) 0.05f else 1.0f, if (alarm.volumeRamp) 0.05f else 1.0f)
                prepare(); start()
            }
        } catch (_: Exception) {
            try { MediaPlayer.create(this, Settings.System.DEFAULT_ALARM_ALERT_URI)?.apply {
                isLooping = true; start()
            } } catch (_: Exception) {}
        }
        if (alarm.volumeRamp && mediaPlayer != null) {
            val player = mediaPlayer!!
            volumeRampJob = Thread {
                for (i in 1..50) {
                    if (Thread.interrupted()) return@Thread
                    val vol = minOf(1.0f, 0.05f + (i.toFloat() / 50) * 0.95f)
                    player.setVolume(vol, vol)
                    try { Thread.sleep(300L) } catch (_: InterruptedException) { return@Thread }
                }
            }.apply { start() }
        }
        if (alarm.vibrate) vibrator?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 500, 500, 500, 1000, 500, 500, 500), 1))
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
