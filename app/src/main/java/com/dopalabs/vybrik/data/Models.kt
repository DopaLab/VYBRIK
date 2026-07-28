package com.dopalabs.vybrik.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

enum class ReminderCategory(val label: String) {
    DAILY("Daily life"),
    ONE_TIME("One-time"),
    HOLIDAY("Holidays")
}

enum class AlertMode { ALARM, NOTIFICATION, SILENT }

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val note: String = "",
    val triggerAtMillis: Long,
    val category: ReminderCategory,
    val tabId: String = "daily",
    val repeatsDaily: Boolean = false,
    val repeatDaysMask: Int = 0,
    val alertMode: AlertMode = AlertMode.ALARM,
    val soundUri: String = "",
    val enabled: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val repeats: Boolean get() = repeatsDaily || repeatDaysMask != 0

    fun displayTitle(): String = title.trim()

    fun nextTrigger(nowMillis: Long = System.currentTimeMillis()): Long {
        if (!repeats) return triggerAtMillis
        val zone = ZoneId.systemDefault()
        val chosenTime = Instant.ofEpochMilli(triggerAtMillis).atZone(zone).toLocalTime()
        val firstDate = Instant.ofEpochMilli(maxOf(nowMillis, triggerAtMillis)).atZone(zone).toLocalDate()
        val mask = if (repeatsDaily && repeatDaysMask == 0) EVERY_DAY_MASK else repeatDaysMask
        for (offset in 0..7) {
            val date = firstDate.plusDays(offset.toLong())
            val dayBit = 1 shl (date.dayOfWeek.value - DayOfWeek.MONDAY.value)
            if (mask and dayBit == 0) continue
            val candidate = LocalDateTime.of(date, chosenTime).atZone(zone).toInstant().toEpochMilli()
            if (candidate > nowMillis && candidate >= triggerAtMillis) return candidate
        }
        return triggerAtMillis
    }

    companion object { const val EVERY_DAY_MASK = 0b1111111 }
}

data class CountdownTab(val id: String, val name: String)
