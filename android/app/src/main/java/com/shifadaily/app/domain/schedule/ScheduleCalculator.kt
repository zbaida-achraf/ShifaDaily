package com.shifadaily.app.domain.schedule

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Pure recurrence calculations. No Android dependencies, fully unit-tested.
 *
 * DST handling relies on [ZonedDateTime.of]:
 *  - in a gap (local time does not exist) the time is shifted forward by the gap length;
 *  - in an overlap (local time happens twice) the earlier offset is used, so each
 *    (date, time) maps to exactly one instant.
 */
object ScheduleCalculator {

    /** Upper bound for "next occurrence" searches; covers the largest interval (365 days). */
    private const val MAX_SEARCH_DAYS = 800L

    fun isScheduledOn(spec: ScheduleSpec, date: LocalDate): Boolean {
        if (date.isBefore(spec.startDate)) return false
        if (spec.endDate != null && date.isAfter(spec.endDate)) return false
        return when (spec.type) {
            ScheduleType.DAILY -> true
            ScheduleType.WEEKDAYS -> date.dayOfWeek in spec.weekdays
            ScheduleType.INTERVAL -> {
                val n = spec.intervalDays.coerceAtLeast(1)
                ChronoUnit.DAYS.between(spec.startDate, date) % n == 0L
            }
        }
    }

    fun instantOf(date: LocalDate, time: java.time.LocalTime, zone: ZoneId): Instant =
        ZonedDateTime.of(date, time, zone).toInstant()

    /** All occurrences on one local date, sorted by time. */
    fun occurrencesOn(medicationId: Long, spec: ScheduleSpec, date: LocalDate, zone: ZoneId): List<Occurrence> {
        if (!isScheduledOn(spec, date)) return emptyList()
        return spec.times.distinct().sorted().map { t ->
            Occurrence(OccurrenceKey(medicationId, date, t), instantOf(date, t, zone))
        }
    }

    /** Occurrences with `fromExclusive < instant <= toInclusive`, sorted by instant. */
    fun occurrencesBetween(
        medicationId: Long,
        spec: ScheduleSpec,
        fromExclusive: Instant,
        toInclusive: Instant,
        zone: ZoneId,
    ): List<Occurrence> {
        if (!toInclusive.isAfter(fromExclusive)) return emptyList()
        // One extra day on each side covers offsets and DST shifts.
        var date = fromExclusive.atZone(zone).toLocalDate().minusDays(1)
        val last = toInclusive.atZone(zone).toLocalDate().plusDays(1)
        val out = ArrayList<Occurrence>()
        while (!date.isAfter(last)) {
            for (o in occurrencesOn(medicationId, spec, date, zone)) {
                if (o.instant.isAfter(fromExclusive) && !o.instant.isAfter(toInclusive)) out += o
            }
            date = date.plusDays(1)
        }
        return out.sortedWith(compareBy({ it.instant }, { it.key.time }))
    }

    /**
     * First occurrence strictly after [after] whose key is not in [skip].
     * Returns null when the schedule has ended.
     */
    fun nextOccurrence(
        medicationId: Long,
        spec: ScheduleSpec,
        after: Instant,
        zone: ZoneId,
        skip: Set<OccurrenceKey> = emptySet(),
    ): Occurrence? {
        var date = after.atZone(zone).toLocalDate().minusDays(1)
        if (date.isBefore(spec.startDate)) date = spec.startDate
        repeat(MAX_SEARCH_DAYS.toInt()) {
            if (spec.endDate != null && date.isAfter(spec.endDate)) return null
            for (o in occurrencesOn(medicationId, spec, date, zone)) {
                if (o.instant.isAfter(after) && o.key !in skip) return o
            }
            date = date.plusDays(1)
        }
        return null
    }

    /** True when the schedule produces at least one occurrence (used by validation). */
    fun hasAnyOccurrence(spec: ScheduleSpec): Boolean {
        if (spec.times.isEmpty()) return false
        var date = spec.startDate
        val limit = spec.endDate ?: spec.startDate.plusDays(MAX_SEARCH_DAYS)
        while (!date.isAfter(limit)) {
            if (isScheduledOn(spec, date)) return true
            date = date.plusDays(1)
        }
        return false
    }
}
