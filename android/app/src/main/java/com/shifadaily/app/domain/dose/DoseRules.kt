package com.shifadaily.app.domain.dose

import java.time.Duration
import java.time.Instant

enum class DoseStatus {
    /** Reminder shown, no answer yet. */
    PENDING,
    TAKEN,
    SKIPPED,
    /** Snoozed; will be re-announced at snoozeUntil. */
    SNOOZED,
    /** No answer within [DoseRules.NO_RESPONSE_AFTER], or the reminder could not be delivered. */
    MISSED,
}

enum class EventSource { NOTIFICATION, APP, SYSTEM }

object DoseRules {
    /** A shown reminder with no answer becomes "missed (no response)" after this. */
    val NO_RESPONSE_AFTER: Duration = Duration.ofHours(6)

    val SNOOZE_CHOICES_MINUTES = listOf(5, 10, 15, 30)
    val MAX_SNOOZE_CHOICES = listOf(1, 2, 3, 5)

    /** Returns when the snoozed reminder should re-alert, or null if the snooze limit is reached. */
    fun snoozeUntil(now: Instant, snoozeMinutes: Int, snoozeCount: Int, maxSnoozes: Int): Instant? {
        if (snoozeCount >= maxSnoozes) return null
        val minutes = snoozeMinutes.coerceIn(1, 120)
        return now.plus(Duration.ofMinutes(minutes.toLong()))
    }

    fun canSnooze(snoozeCount: Int, maxSnoozes: Int): Boolean = snoozeCount < maxSnoozes

    fun isOpen(status: DoseStatus) = status == DoseStatus.PENDING || status == DoseStatus.SNOOZED
}
