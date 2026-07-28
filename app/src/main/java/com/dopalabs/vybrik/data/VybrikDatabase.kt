package com.dopalabs.vybrik.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ReminderEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VybrikDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var instance: VybrikDatabase? = null

        fun get(context: Context): VybrikDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                VybrikDatabase::class.java,
                "vybrik.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN tabId TEXT NOT NULL DEFAULT 'daily'")
                db.execSQL("ALTER TABLE reminders ADD COLUMN repeatDaysMask INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE reminders SET tabId = 'one-time' WHERE category = 'ONE_TIME'")
                db.execSQL("UPDATE reminders SET tabId = 'holidays' WHERE category = 'HOLIDAY'")
                db.execSQL("UPDATE reminders SET repeatDaysMask = 127 WHERE repeatsDaily = 1")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN alertMode TEXT NOT NULL DEFAULT 'ALARM'")
                db.execSQL("ALTER TABLE reminders ADD COLUMN soundUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("DROP TABLE IF EXISTS sports_events")
            }
        }
    }
}
