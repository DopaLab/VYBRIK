package com.dopalabs.vybrik.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dopalabs.vybrik.MainActivity
import com.dopalabs.vybrik.data.ReminderEntity
import com.dopalabs.vybrik.data.AlertMode

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(reminder: ReminderEntity) {
        if (reminder.alertMode == AlertMode.SILENT) {
            cancel(reminder.id)
            return
        }
        val triggerAt = reminder.nextTrigger()
        if (triggerAt <= System.currentTimeMillis()) return
        val alarmIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            Intent(context, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val showIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (reminder.alertMode == AlertMode.ALARM && canBeExact) {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), alarmIntent)
        } else if (canBeExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, alarmIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, alarmIntent)
        }
    }

    fun cancel(id: String) {
        val pending = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pending)
        pending.cancel()
    }
}
