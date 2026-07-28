package com.dopalabs.vybrik.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dopalabs.vybrik.data.VybrikDatabase
import com.dopalabs.vybrik.data.AlertMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = VybrikDatabase.get(context).reminderDao().byId(id) ?: return@launch
                if (reminder.alertMode != AlertMode.SILENT) AlarmNotifier.show(context, reminder)
                if (reminder.repeats) {
                    AlarmScheduler(context).schedule(reminder)
                } else {
                    VybrikDatabase.get(context).reminderDao().update(reminder.copy(enabled = false))
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object { const val EXTRA_REMINDER_ID = "reminder_id" }
}
