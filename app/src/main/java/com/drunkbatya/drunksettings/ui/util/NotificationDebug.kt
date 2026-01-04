package com.drunkbatya.drunksettings.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.drunkbatya.drunksettings.R

const val DEBUG_CHANNEL_ID = "debug_sound_notifications"

fun sendTestNotification(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val existing = manager.getNotificationChannel(DEBUG_CHANNEL_ID)
        if (existing == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                DEBUG_CHANNEL_ID,
                "Debug Sound",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Debug notification channel with sound"
                setSound(soundUri, audioAttributes)
            }
            manager.createNotificationChannel(channel)
        }
    }

    val notification = NotificationCompat.Builder(context, DEBUG_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Test sound notification")
        .setContentText("DrunkSettings debug notification")
        .setAutoCancel(true)
        .setSound(soundUri)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    val notificationId = (System.currentTimeMillis() and 0xFFFFFF).toInt()
    manager.notify(notificationId, notification)
}
