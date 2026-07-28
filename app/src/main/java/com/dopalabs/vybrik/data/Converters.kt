package com.dopalabs.vybrik.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromCategory(value: ReminderCategory): String = value.name
    @TypeConverter fun toCategory(value: String): ReminderCategory = ReminderCategory.valueOf(value)
    @TypeConverter fun fromAlertMode(value: AlertMode): String = value.name
    @TypeConverter fun toAlertMode(value: String): AlertMode = AlertMode.valueOf(value)
}
