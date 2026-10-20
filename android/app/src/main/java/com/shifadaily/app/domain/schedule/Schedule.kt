package com.shifadaily.app.domain.schedule

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

enum class ScheduleType { DAILY, WEEKDAYS, INTERVAL }

/**
 * A medication schedule expressed in local wall-clock terms.
 * Times are local times of day: 08:00 stays 08:00 after travel or a DST change.
 */
data class ScheduleSpec(
    val type: ScheduleType,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val times: List<LocalTime>,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val intervalDays: Int = 1,
)

/** Stable identity of one scheduled dose. Used for de-duplication. */
data class OccurrenceKey(
    val medicationId: Long,
    val date: LocalDate,
    val time: LocalTime,
)

data class Occurrence(
    val key: OccurrenceKey,
    val instant: Instant,
)

/** Inputs the reconcile planner needs about one active medication. */
data class MedSchedule(
    val medicationId: Long,
    val spec: ScheduleSpec,
    /** Occurrences at or before this instant are ignored (creation / edit / resume time). */
    val effectiveFrom: Instant,
)
