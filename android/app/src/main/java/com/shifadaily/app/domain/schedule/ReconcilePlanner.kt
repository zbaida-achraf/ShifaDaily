package com.shifadaily.app.domain.schedule

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

data class ReconcilePlan(
    /** Due now (or slightly late): post one reminder each. */
    val notifyNow: List<Occurrence>,
    /** Passed while no reminder could be shown (device off, app force-stopped...). Record as missed, no alarm burst. */
    val missedUndelivered: List<Occurrence>,
    /** Earliest future scheduled occurrence across all medications, or null. */
    val nextOccurrence: Instant?,
)

/**
 * Decides what the reminder engine must do at a given moment. Pure and deterministic,
 * so duplicate prevention and catch-up behaviour are unit-testable.
 * See docs/ADR-001-medication-alarms.md §6.
 */
object ReconcilePlanner {
    /** Late by at most this much: still show a normal reminder. */
    val CATCH_UP_GRACE: Duration = Duration.ofMinutes(30)

    /** Never look further back than this when the app was not running. */
    val MAX_LOOKBACK: Duration = Duration.ofHours(24)

    /** Alarms may be delivered a few seconds early/late; treat these as "due". */
    val DUE_TOLERANCE: Duration = Duration.ofSeconds(5)

    /** Small overlap with the previous run; duplicates are filtered by key. */
    private val OVERLAP: Duration = Duration.ofMinutes(2)

    fun plan(
        meds: List<MedSchedule>,
        existingKeys: Set<OccurrenceKey>,
        lastRun: Instant?,
        now: Instant,
        zone: ZoneId,
    ): ReconcilePlan {
        val dueLimit = now.plus(DUE_TOLERANCE)
        val earliest = now.minus(MAX_LOOKBACK)
        val lookbackStart = when {
            lastRun == null -> now.minus(CATCH_UP_GRACE)
            lastRun.isBefore(earliest) -> earliest
            else -> lastRun.minus(OVERLAP)
        }

        val notify = ArrayList<Occurrence>()
        val missed = ArrayList<Occurrence>()
        var next: Instant? = null

        for (med in meds) {
            val from = maxOf(lookbackStart, med.effectiveFrom)
            for (o in ScheduleCalculator.occurrencesBetween(med.medicationId, med.spec, from, dueLimit, zone)) {
                if (o.key in existingKeys) continue
                val lateness = Duration.between(o.instant, now)
                if (lateness <= CATCH_UP_GRACE) notify += o else missed += o
            }
            val after = maxOf(dueLimit, med.effectiveFrom)
            val n = ScheduleCalculator.nextOccurrence(med.medicationId, med.spec, after, zone, existingKeys)
            if (n != null && (next == null || n.instant.isBefore(next))) next = n.instant
        }
        return ReconcilePlan(
            notifyNow = notify.sortedBy { it.instant },
            missedUndelivered = missed.sortedBy { it.instant },
            nextOccurrence = next,
        )
    }
}
