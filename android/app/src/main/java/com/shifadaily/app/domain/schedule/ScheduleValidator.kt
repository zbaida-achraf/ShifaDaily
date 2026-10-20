package com.shifadaily.app.domain.schedule

enum class MedicationError {
    NAME_REQUIRED,
    NAME_TOO_LONG,
    DOSE_REQUIRED,
    NO_TIMES,
    DUPLICATE_TIMES,
    NO_WEEKDAYS,
    INTERVAL_OUT_OF_RANGE,
    END_BEFORE_START,
    NO_OCCURRENCES,
}

object ScheduleValidator {
    const val MAX_NAME = 80
    const val MAX_INTERVAL = 365

    fun validate(name: String, doseAmount: String, spec: ScheduleSpec): List<MedicationError> {
        val errors = ArrayList<MedicationError>()
        val trimmed = name.trim()
        if (trimmed.isEmpty()) errors += MedicationError.NAME_REQUIRED
        if (trimmed.length > MAX_NAME) errors += MedicationError.NAME_TOO_LONG
        if (doseAmount.isBlank()) errors += MedicationError.DOSE_REQUIRED
        if (spec.times.isEmpty()) errors += MedicationError.NO_TIMES
        if (spec.times.size != spec.times.distinct().size) errors += MedicationError.DUPLICATE_TIMES
        if (spec.type == ScheduleType.WEEKDAYS && spec.weekdays.isEmpty()) errors += MedicationError.NO_WEEKDAYS
        if (spec.type == ScheduleType.INTERVAL && spec.intervalDays !in 2..MAX_INTERVAL) {
            errors += MedicationError.INTERVAL_OUT_OF_RANGE
        }
        if (spec.endDate != null && spec.endDate.isBefore(spec.startDate)) errors += MedicationError.END_BEFORE_START
        // Contradiction check (e.g. "Mondays only" between a Tuesday and a Thursday).
        if (errors.isEmpty() && !ScheduleCalculator.hasAnyOccurrence(spec)) errors += MedicationError.NO_OCCURRENCES
        return errors
    }
}
