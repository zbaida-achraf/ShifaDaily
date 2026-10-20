package com.shifadaily.app.domain

import com.shifadaily.app.data.db.Converters
import com.shifadaily.app.domain.dose.DoseRules
import com.shifadaily.app.domain.schedule.MedicationError
import com.shifadaily.app.domain.schedule.ScheduleSpec
import com.shifadaily.app.domain.schedule.ScheduleType
import com.shifadaily.app.domain.schedule.ScheduleValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

class RulesAndValidationTest {
    private val start = LocalDate.of(2026, 10, 19)
    private fun spec(type: ScheduleType = ScheduleType.DAILY, times: List<LocalTime> = listOf(LocalTime.of(8, 0)),
                     days: Set<DayOfWeek> = emptySet(), interval: Int = 1, end: LocalDate? = null) =
        ScheduleSpec(type, start, end, times, days, interval)

    @Test fun validScheduleHasNoErrors() {
        assertTrue(ScheduleValidator.validate("Metformin", "500", spec()).isEmpty())
    }

    @Test fun missingFieldsAreReported() {
        val e = ScheduleValidator.validate("  ", "", spec(times = emptyList()))
        assertTrue(MedicationError.NAME_REQUIRED in e)
        assertTrue(MedicationError.DOSE_REQUIRED in e)
        assertTrue(MedicationError.NO_TIMES in e)
    }

    @Test fun contradictoryAndIncompleteSchedulesAreRejected() {
        assertEquals(listOf(MedicationError.NO_WEEKDAYS), ScheduleValidator.validate("A", "1", spec(ScheduleType.WEEKDAYS)))
        assertEquals(listOf(MedicationError.INTERVAL_OUT_OF_RANGE), ScheduleValidator.validate("A", "1", spec(ScheduleType.INTERVAL, interval = 1)))
        assertEquals(listOf(MedicationError.END_BEFORE_START), ScheduleValidator.validate("A", "1", spec(end = start.minusDays(1))))
        val t = LocalTime.of(9, 0)
        assertEquals(listOf(MedicationError.DUPLICATE_TIMES), ScheduleValidator.validate("A", "1", spec(times = listOf(t, t))))
        // Sunday only, but the window is Mon 19 -> Wed 21.
        assertEquals(listOf(MedicationError.NO_OCCURRENCES),
            ScheduleValidator.validate("A", "1", spec(ScheduleType.WEEKDAYS, days = setOf(DayOfWeek.SUNDAY), end = start.plusDays(2))))
    }

    @Test fun snoozeHonoursIntervalAndLimit() {
        val now = Instant.parse("2026-10-19T08:00:00Z")
        assertEquals(now.plusSeconds(600), DoseRules.snoozeUntil(now, 10, 0, 3))
        assertEquals(now.plusSeconds(600), DoseRules.snoozeUntil(now, 10, 2, 3))
        assertNull(DoseRules.snoozeUntil(now, 10, 3, 3))
        // Out-of-range values are clamped, never zero or unbounded.
        assertEquals(now.plusSeconds(60), DoseRules.snoozeUntil(now, 0, 0, 3))
        assertEquals(now.plusSeconds(7200), DoseRules.snoozeUntil(now, 999, 0, 3))
    }

    @Test fun convertersRoundTripAndIgnoreArabicDefaultLocale() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            val c = Converters()
            val times = listOf(LocalTime.of(20, 30), LocalTime.of(8, 5))
            val s = c.timesToString(times)
            assertEquals("08:05,20:30", s)
            assertEquals(times.sorted(), c.stringToTimes(s))
            val days = setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY)
            assertEquals(days, c.intToDays(c.daysToInt(days)))
            assertEquals(LocalTime.of(23, 59), c.intToTime(c.timeToInt(LocalTime.of(23, 59))))
        } finally {
            Locale.setDefault(saved)
        }
    }
}
