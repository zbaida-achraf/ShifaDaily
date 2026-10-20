package com.shifadaily.app.domain.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleCalculatorTest {
    private val casa = ZoneId.of("Africa/Casablanca")
    private val paris = ZoneId.of("Europe/Paris")
    private val t8 = LocalTime.of(8, 0)
    private val t20 = LocalTime.of(20, 0)

    private fun daily(start: LocalDate, end: LocalDate? = null, vararg times: LocalTime) =
        ScheduleSpec(ScheduleType.DAILY, start, end, times.toList())

    @Test fun dailyIsScheduledEveryDayWithinBounds() {
        val s = daily(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), t8)
        assertFalse(ScheduleCalculator.isScheduledOn(s, LocalDate.of(2026, 9, 30)))
        assertTrue(ScheduleCalculator.isScheduledOn(s, LocalDate.of(2026, 10, 1)))
        assertTrue(ScheduleCalculator.isScheduledOn(s, LocalDate.of(2026, 10, 3)))
        assertFalse(ScheduleCalculator.isScheduledOn(s, LocalDate.of(2026, 10, 4)))
    }

    @Test fun weekdaysOnlyMatchSelectedDays() {
        val s = ScheduleSpec(ScheduleType.WEEKDAYS, LocalDate.of(2026, 10, 1), null, listOf(t8), setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        // 2026-10-19 is a Monday.
        assertTrue(ScheduleCalculator.isScheduledOn(s, LocalDate.of(2026, 10, 19)))
        assertFalse(ScheduleCalculator.isScheduledOn(s, LocalDate.of(2026, 10, 20)))
        assertTrue(ScheduleCalculator.isScheduledOn(s, LocalDate.of(2026, 10, 23)))
    }

    @Test fun everyNDaysIsAnchoredOnStartDate() {
        val s = ScheduleSpec(ScheduleType.INTERVAL, LocalDate.of(2026, 10, 1), null, listOf(t8), intervalDays = 3)
        val days = (0L..9L).map { LocalDate.of(2026, 10, 1).plusDays(it) }.filter { ScheduleCalculator.isScheduledOn(s, it) }
        assertEquals(listOf(1, 4, 7, 10), days.map { it.dayOfMonth })
    }

    @Test fun multipleTimesPerDayAreSortedAndDeduplicated() {
        val s = daily(LocalDate.of(2026, 10, 1), null, t20, t8, t20)
        val occ = ScheduleCalculator.occurrencesOn(1, s, LocalDate.of(2026, 10, 2), casa)
        assertEquals(listOf(t8, t20), occ.map { it.key.time })
    }

    @Test fun occurrencesBetweenIsHalfOpen() {
        val s = daily(LocalDate.of(2026, 10, 1), null, t8)
        val at8 = ZonedDateTime.of(LocalDate.of(2026, 10, 2), t8, casa).toInstant()
        assertTrue(ScheduleCalculator.occurrencesBetween(1, s, at8, at8.plusSeconds(3600), casa).isEmpty())
        assertEquals(1, ScheduleCalculator.occurrencesBetween(1, s, at8.minusSeconds(1), at8, casa).size)
    }

    @Test fun nextOccurrenceRespectsEndDate() {
        val s = daily(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), t8)
        val after = ZonedDateTime.of(LocalDate.of(2026, 10, 2), LocalTime.of(9, 0), casa).toInstant()
        assertNull(ScheduleCalculator.nextOccurrence(1, s, after, casa))
    }

    @Test fun nextOccurrenceSkipsRecordedKeys() {
        val s = daily(LocalDate.of(2026, 10, 1), null, t8)
        val after = ZonedDateTime.of(LocalDate.of(2026, 10, 5), LocalTime.of(7, 0), casa).toInstant()
        val skip = setOf(OccurrenceKey(1, LocalDate.of(2026, 10, 5), t8))
        assertEquals(LocalDate.of(2026, 10, 6), ScheduleCalculator.nextOccurrence(1, s, after, casa, skip)!!.key.date)
    }

    @Test fun dstGapShiftsForwardInsteadOfSkipping() {
        // Europe/Paris springs forward on 2026-03-29: 02:00 -> 03:00, so 02:30 does not exist.
        val s = daily(LocalDate.of(2026, 3, 28), null, LocalTime.of(2, 30))
        val occ = ScheduleCalculator.occurrencesOn(1, s, LocalDate.of(2026, 3, 29), paris).single()
        assertEquals(ZonedDateTime.of(2026, 3, 29, 3, 30, 0, 0, paris).toInstant(), occ.instant)
    }

    @Test fun dstOverlapProducesExactlyOneOccurrence() {
        // Europe/Paris falls back on 2026-10-25 (03:00 -> 02:00), so 02:30 local happens twice.
        val s = daily(LocalDate.of(2026, 10, 24), null, LocalTime.of(2, 30))
        val from = ZonedDateTime.of(2026, 10, 25, 0, 0, 0, 0, paris).toInstant()
        val to = ZonedDateTime.of(2026, 10, 25, 6, 0, 0, 0, paris).toInstant()
        val list = ScheduleCalculator.occurrencesBetween(1, s, from, to, paris)
        assertEquals(1, list.size)
        // Earlier offset (+02:00) is used.
        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), list.single().instant)
    }

    @Test fun dailyHoursStayLocalAcrossDstChange() {
        val s = daily(LocalDate.of(2026, 10, 20), null, t8)
        val from = ZonedDateTime.of(2026, 10, 23, 12, 0, 0, 0, paris).toInstant()
        val to = ZonedDateTime.of(2026, 10, 28, 12, 0, 0, 0, paris).toInstant()
        val list = ScheduleCalculator.occurrencesBetween(1, s, from, to, paris)
        assertEquals(5, list.size)
        assertTrue(list.all { it.instant.atZone(paris).toLocalTime() == t8 })
    }

    @Test fun timeZoneChangeKeepsLocalWallClock() {
        // Same schedule evaluated after travelling: 08:00 local in each zone.
        val s = daily(LocalDate.of(2026, 10, 1), null, t8)
        val d = LocalDate.of(2026, 10, 10)
        val inCasa = ScheduleCalculator.occurrencesOn(1, s, d, casa).single().instant
        val inDubai = ScheduleCalculator.occurrencesOn(1, s, d, ZoneId.of("Asia/Dubai")).single().instant
        assertEquals(t8, inCasa.atZone(casa).toLocalTime())
        assertEquals(t8, inDubai.atZone(ZoneId.of("Asia/Dubai")).toLocalTime())
        assertTrue(inDubai.isBefore(inCasa))
    }

    @Test fun hasAnyOccurrenceDetectsContradictions() {
        // Mondays only, between a Tuesday and a Thursday.
        val s = ScheduleSpec(ScheduleType.WEEKDAYS, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22), listOf(t8), setOf(DayOfWeek.MONDAY))
        assertFalse(ScheduleCalculator.hasAnyOccurrence(s))
    }
}
