package com.dopalabs.vybrik.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dopalabs.vybrik.MainActivity
import com.dopalabs.vybrik.R
import com.dopalabs.vybrik.data.AlertMode
import com.dopalabs.vybrik.data.ReminderEntity

object AlarmNotifier {
    private const val NOTIFICATION_CHANNEL_ID = "vybrik_notifications"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Countdown notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "VYBRIK countdown notifications" }
        )
    }

    fun show(context: Context, reminder: ReminderEntity) {
        val isAlarm = reminder.alertMode == AlertMode.ALARM
        val channelId = if (isAlarm) ensureAlarmChannel(context, reminder.soundUri) else NOTIFICATION_CHANNEL_ID
        val contentIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentText(reminder.note.ifBlank { "Your countdown has reached zero." })
            .setCategory(if (isAlarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_EVENT)
            .setPriority(if (isAlarm) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
        if (reminder.displayTitle().isNotBlank()) builder.setContentTitle(reminder.displayTitle())
        val notification = builder.build()
        runCatching {
            NotificationManagerCompat.from(context).notify(reminder.id.hashCode(), notification)
        }
    }

    private fun ensureAlarmChannel(context: Context, soundUri: String): String {
        val sound = soundUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        listOf("android", "com.android.systemui").forEach { packageName ->
            runCatching { context.grantUriPermission(packageName, sound, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        val channelId = "vybrik_alarm_${sound.toString().hashCode()}"
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Countdown alarm", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "VYBRIK alarms using the selected sound"
                enableVibration(true)
                setSound(sound, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            }
        )
        return channelId
    }
}
