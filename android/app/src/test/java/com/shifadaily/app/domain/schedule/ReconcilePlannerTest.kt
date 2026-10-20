package com.shifadaily.app.domain.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReconcilePlannerTest {
    private val zone = ZoneId.of("Africa/Casablanca")
    private val t8 = LocalTime.of(8, 0)
    private val day = LocalDate.of(2026, 10, 19)
    private val at8 = ZonedDateTime.of(day, t8, zone).toInstant()
    private val created = at8.minus(Duration.ofDays(3))
    private fun med(id: Long = 1, effective: Instant = created, vararg times: LocalTime = arrayOf(t8)) =
        MedSchedule(id, ScheduleSpec(ScheduleType.DAILY, day.minusDays(10), null, times.toList()), effective)

    @Test fun dueOccurrenceIsNotifiedOnceAndNextIsTomorrow() {
        val plan = ReconcilePlanner.plan(listOf(med()), emptySet(), at8.minusSeconds(3600), at8, zone)
        assertEquals(listOf(OccurrenceKey(1, day, t8)), plan.notifyNow.map { it.key })
        assertTrue(plan.missedUndelivered.isEmpty())
        // Next must skip the occurrence being notified now (it is not in existingKeys yet but is <= dueLimit).
        assertEquals(ZonedDateTime.of(day.plusDays(1), t8, zone).toInstant(), plan.nextOccurrence)
    }

    @Test fun existingRecordPreventsDuplicateReminder() {
        val keys = setOf(OccurrenceKey(1, day, t8))
        val plan = ReconcilePlanner.plan(listOf(med()), keys, at8.minusSeconds(3600), at8.plusSeconds(2), zone)
        assertTrue(plan.notifyNow.isEmpty())
    }

    @Test fun runningTwiceWithInsertedKeysIsIdempotent() {
        val first = ReconcilePlanner.plan(listOf(med()), emptySet(), at8.minusSeconds(60), at8, zone)
        val keys = first.notifyNow.map { it.key }.toSet()
        val second = ReconcilePlanner.plan(listOf(med()), keys, at8, at8.plusSeconds(1), zone)
        assertTrue(second.notifyNow.isEmpty())
        assertTrue(second.missedUndelivered.isEmpty())
    }

    @Test fun slightlyLateAlarmStillNotifies() {
        val now = at8.plus(Duration.ofMinutes(20))
        val plan = ReconcilePlanner.plan(listOf(med()), emptySet(), at8.minusSeconds(3600), now, zone)
        assertEquals(1, plan.notifyNow.size)
    }

    @Test fun phoneOffForHoursProducesMissedNotBurst() {
        // Last run yesterday evening; phone off; now 3 h after two scheduled doses.
        val m = med(times = arrayOf(LocalTime.of(6, 0), t8))
        val lastRun = ZonedDateTime.of(day.minusDays(1), LocalTime.of(22, 0), zone).toInstant()
        val now = at8.plus(Duration.ofHours(3))
        val plan = ReconcilePlanner.plan(listOf(m), emptySet(), lastRun, now, zone)
        assertTrue(plan.notifyNow.isEmpty())
        assertEquals(2, plan.missedUndelivered.size)
    }

    @Test fun lookbackIsCappedAt24Hours() {
        val lastRun = at8.minus(Duration.ofDays(5))
        val now = at8.plus(Duration.ofHours(2))
        val plan = ReconcilePlanner.plan(listOf(med()), emptySet(), lastRun, now, zone)
        // Only occurrences within the last 24 h: today 08:00 (yesterday's 08:00 is 26 h ago).
        assertEquals(1, plan.missedUndelivered.size)
    }

    @Test fun firstRunHasNoHistoricalCatchUp() {
        val now = at8.plus(Duration.ofHours(2))
        val plan = ReconcilePlanner.plan(listOf(med()), emptySet(), null, now, zone)
        assertTrue(plan.notifyNow.isEmpty())
        assertTrue(plan.missedUndelivered.isEmpty())
    }

    @Test fun effectiveFromHidesEarlierOccurrences() {
        // Medication added at 10:00 with an 08:00 time: no missed dose for this morning.
        val addedAt = at8.plus(Duration.ofHours(2))
        val plan = ReconcilePlanner.plan(listOf(med(effective = addedAt)), emptySet(), at8.minusSeconds(7200), addedAt.plusSeconds(5), zone)
        assertTrue(plan.notifyNow.isEmpty())
        assertTrue(plan.missedUndelivered.isEmpty())
        assertEquals(ZonedDateTime.of(day.plusDays(1), t8, zone).toInstant(), plan.nextOccurrence)
    }

    @Test fun nextOccurrenceIsEarliestAcrossMedications() {
        val a = med(1, times = arrayOf(LocalTime.of(21, 0)))
        val b = med(2, times = arrayOf(LocalTime.of(13, 0)))
        val plan = ReconcilePlanner.plan(listOf(a, b), emptySet(), at8, at8.plusSeconds(60), zone)
        assertEquals(ZonedDateTime.of(day, LocalTime.of(13, 0), zone).toInstant(), plan.nextOccurrence)
    }

    @Test fun takenEarlyIsSkippedForNextAlarm() {
        val keys = setOf(OccurrenceKey(1, day, LocalTime.of(13, 0)))
        val m = med(times = arrayOf(LocalTime.of(13, 0)))
        val plan = ReconcilePlanner.plan(listOf(m), keys, at8, at8.plusSeconds(60), zone)
        assertEquals(ZonedDateTime.of(day.plusDays(1), LocalTime.of(13, 0), zone).toInstant(), plan.nextOccurrence)
    }
}
