// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 MR.English2008. See LICENSE.
package me2008.alarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import me2008.alarm.MainActivity

/** Rings when a timer finishes: alarm sound, vibration and a notification with a Stop button. */
class TimerRingService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stopRinging() }

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Timer finished", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Shown while a finished timer rings"
                setSound(null, null); enableVibration(false)
            })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopRinging(); return START_NOT_STICKY }
        startForeground(NOTIFICATION_ID, buildNotification())
        if (player == null) play()
        // Don't ring forever if nobody is around.
        handler.removeCallbacks(autoStop)
        handler.postDelayed(autoStop, 60_000L)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(autoStop)
        player?.apply { if (isPlaying) stop(); release() }
        player = null
        vibrator?.cancel()
        super.onDestroy()
    }

    private fun stopRinging() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun play() {
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                setDataSource(this@TimerRingService, Settings.System.DEFAULT_ALARM_ALERT_URI)
                isLooping = true
                prepare(); start()
            }
        } catch (_: Exception) {
            // No sound available; vibration and the notification still tell the user.
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500, 500, 1000), 0))
    }

    private fun buildNotification(): Notification {
        val immutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
        val open = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            immutable)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, TimerRingService::class.java).setAction(ACTION_STOP), immutable or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Timer").setContentText("Time's up")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .setOngoing(true).build()
    }

    companion object {
        const val CHANNEL_ID = "crystal_timer_finished"
        const val NOTIFICATION_ID = 1002
        const val ACTION_STOP = "me2008.alarm.TIMER_STOP"
    }
}
