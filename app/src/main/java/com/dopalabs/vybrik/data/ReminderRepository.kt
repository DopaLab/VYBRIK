package com.dopalabs.vybrik.data

import com.dopalabs.vybrik.alarm.AlarmScheduler
import kotlinx.coroutines.flow.Flow

class ReminderRepository(
    private val dao: ReminderDao,
    private val scheduler: AlarmScheduler
) {
    val reminders: Flow<List<ReminderEntity>> = dao.observeAll()

    suspend fun save(reminder: ReminderEntity) {
        dao.upsert(reminder)
        if (reminder.enabled) scheduler.schedule(reminder) else scheduler.cancel(reminder.id)
    }

    suspend fun delete(reminder: ReminderEntity) {
        scheduler.cancel(reminder.id)
        dao.delete(reminder)
    }

    suspend fun moveToTab(sourceTabId: String, destinationTabId: String) =
        dao.moveToTab(sourceTabId, destinationTabId)

    suspend fun rescheduleAll() {
        dao.enabled().forEach { reminder ->
            if (reminder.repeats || reminder.triggerAtMillis > System.currentTimeMillis()) {
                scheduler.schedule(reminder)
            }
        }
    }
}
