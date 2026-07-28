package com.dopalabs.vybrik

import com.dopalabs.vybrik.ui.countdownParts
import com.dopalabs.vybrik.ui.to24Hour
import com.dopalabs.vybrik.data.ReminderCategory
import com.dopalabs.vybrik.data.ReminderEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

class CountdownTest {
    @Test fun splitsLongCountdownExactly() {
        val now = 1_000L
        val target = now + ((2 * 86_400 + 3 * 3_600 + 4 * 60 + 5) * 1_000L)
        val value = countdownParts(target, now)
        assertEquals(2L, value.days)
        assertEquals(3L, value.hours)
        assertEquals(4L, value.minutes)
        assertEquals(5L, value.seconds)
        assertFalse(value.expired)
    }

    @Test fun expiredCountdownClampsToZero() {
        val value = countdownParts(1_000L, 2_000L)
        assertTrue(value.expired)
        assertEquals("NOW", value.compact())
    }

    @Test fun selectedDayRepeatDoesNotRunBeforeChosenStartDate() {
        val zone = ZoneId.systemDefault()
        val chosen = LocalDateTime.of(2030, 1, 10, 9, 0).atZone(zone).toInstant().toEpochMilli()
        val now = LocalDateTime.of(2030, 1, 1, 8, 0).atZone(zone).toInstant().toEpochMilli()
        val mondayOnly = 1 shl (DayOfWeek.MONDAY.value - 1)
        val reminder = ReminderEntity(
            title = "",
            triggerAtMillis = chosen,
            category = ReminderCategory.ONE_TIME,
            repeatDaysMask = mondayOnly
        )

        assertTrue(reminder.nextTrigger(now) >= chosen)
        val nextDay = java.time.Instant.ofEpochMilli(reminder.nextTrigger(now)).atZone(zone).dayOfWeek
        assertEquals(DayOfWeek.MONDAY, nextDay)
    }

    @Test fun twelveHourClockConvertsWithoutInvalidHours() {
        assertEquals(0, to24Hour(12, false))
        assertEquals(11, to24Hour(11, false))
        assertEquals(12, to24Hour(12, true))
        assertEquals(23, to24Hour(11, true))
    }

    @Test fun blankCountdownTitleStaysVisuallyBlank() {
        val reminder = ReminderEntity(
            title = "   ",
            triggerAtMillis = System.currentTimeMillis() + 60_000,
            category = ReminderCategory.ONE_TIME
        )
        assertEquals("", reminder.displayTitle())
    }
}
