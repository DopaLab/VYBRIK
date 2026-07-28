package com.dopalabs.vybrik.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dopalabs.vybrik.data.ReminderRepository
import com.dopalabs.vybrik.data.VybrikDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = VybrikDatabase.get(context)
                ReminderRepository(db.reminderDao(), AlarmScheduler(context)).rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
