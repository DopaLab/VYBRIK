package com.dopalabs.vybrik

import android.app.Application
import com.dopalabs.vybrik.alarm.AlarmNotifier
import com.dopalabs.vybrik.alarm.AlarmScheduler
import com.dopalabs.vybrik.data.ReminderRepository
import com.dopalabs.vybrik.data.CountdownTabStore
import com.dopalabs.vybrik.data.VybrikDatabase

class VybrikApp : Application() {
    lateinit var database: VybrikDatabase
        private set
    lateinit var reminders: ReminderRepository
        private set
    lateinit var tabs: CountdownTabStore
        private set

    override fun onCreate() {
        super.onCreate()
        database = VybrikDatabase.get(this)
        reminders = ReminderRepository(database.reminderDao(), AlarmScheduler(this))
        tabs = CountdownTabStore(this)
        AlarmNotifier.createChannel(this)
    }
}
