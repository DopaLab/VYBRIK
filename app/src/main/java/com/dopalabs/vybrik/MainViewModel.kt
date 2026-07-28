package com.dopalabs.vybrik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dopalabs.vybrik.data.ReminderCategory
import com.dopalabs.vybrik.data.ReminderEntity
import com.dopalabs.vybrik.data.CountdownTab
import com.dopalabs.vybrik.data.AlertMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val app: VybrikApp) : ViewModel() {
    val reminders: StateFlow<List<ReminderEntity>> = app.reminders.reminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tabs: StateFlow<List<CountdownTab>> = app.tabs.tabs
    fun add(
        title: String,
        note: String,
        triggerAt: Long,
        tabId: String,
        repeatDaysMask: Int,
        alertMode: AlertMode,
        soundUri: String
    ) {
        viewModelScope.launch {
            app.reminders.save(
                ReminderEntity(
                    title = title.trim(),
                    note = note.trim(),
                    triggerAtMillis = triggerAt,
                    category = ReminderCategory.ONE_TIME,
                    tabId = tabId,
                    repeatsDaily = repeatDaysMask == ReminderEntity.EVERY_DAY_MASK,
                    repeatDaysMask = repeatDaysMask,
                    alertMode = alertMode,
                    soundUri = soundUri
                )
            )
        }
    }

    fun update(
        reminder: ReminderEntity,
        title: String,
        note: String,
        triggerAt: Long,
        repeatDaysMask: Int,
        alertMode: AlertMode,
        soundUri: String
    ) {
        viewModelScope.launch {
            app.reminders.save(
                reminder.copy(
                    title = title.trim(),
                    note = note.trim(),
                    triggerAtMillis = triggerAt,
                    repeatsDaily = repeatDaysMask == ReminderEntity.EVERY_DAY_MASK,
                    repeatDaysMask = repeatDaysMask,
                    alertMode = alertMode,
                    soundUri = soundUri,
                    enabled = true
                )
            )
        }
    }

    fun addTab(name: String) = app.tabs.add(name)
    fun renameTab(id: String, name: String) = app.tabs.rename(id, name)
    fun removeTab(id: String) {
        val destination = tabs.value.firstOrNull { it.id != id } ?: return
        viewModelScope.launch {
            app.reminders.moveToTab(id, destination.id)
            app.tabs.remove(id)
        }
    }

    fun delete(reminder: ReminderEntity) = viewModelScope.launch { app.reminders.delete(reminder) }
}
