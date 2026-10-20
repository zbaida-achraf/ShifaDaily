package com.shifadaily.app.alarm

import com.shifadaily.app.data.db.AppDatabase
import com.shifadaily.app.data.db.DoseEventEntity
import com.shifadaily.app.data.db.DoseRecordEntity
import com.shifadaily.app.data.db.MedicationEntity
import com.shifadaily.app.data.db.SchedulerLogEntity
import com.shifadaily.app.data.prefs.SettingsRepository
import com.shifadaily.app.domain.dose.DoseRules
import com.shifadaily.app.domain.dose.DoseStatus
import com.shifadaily.app.domain.dose.EventSource
import com.shifadaily.app.domain.schedule.MedSchedule
import com.shifadaily.app.domain.schedule.Occurrence
import com.shifadaily.app.domain.schedule.OccurrenceKey
import com.shifadaily.app.domain.schedule.ReconcilePlanner
import com.shifadaily.app.domain.schedule.ScheduleCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId

data class ReconcileResult(val nextAlarm: Instant?, val mode: AlarmMode, val at: Instant)

/**
 * Single entry point for everything that changes reminder state. All work happens under
 * one mutex so concurrent triggers (boot + time change + notification action) cannot
 * produce duplicate reminders. See docs/ADR-001-medication-alarms.md.
 */
class ReminderEngine(
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val scheduler: AlarmScheduler,
    private val notifier: ReminderNotifier,
    private val clock: () -> Instant = Instant::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    private val mutex = Mutex()
    private val meds get() = db.medicationDao()
    private val doses get() = db.doseDao()

    private val _last = MutableStateFlow<ReconcileResult?>(null)
    val lastResult: StateFlow<ReconcileResult?> = _last

    suspend fun reconcile(reason: String): ReconcileResult = mutex.withLock { reconcileLocked(reason) }

    // ---- user / notification actions -------------------------------------------------

    suspend fun markTaken(recordId: Long, source: EventSource, takenAt: Instant? = null) =
        act(recordId, DoseStatus.TAKEN, source, correction = false, actionAt = takenAt)

    suspend fun markSkipped(recordId: Long, source: EventSource) =
        act(recordId, DoseStatus.SKIPPED, source, correction = false)

    /** Corrects a status from history; recorded as a correction in the audit trail. */
    suspend fun correct(recordId: Long, status: DoseStatus, takenAt: Instant?) =
        act(recordId, status, EventSource.APP, correction = true, actionAt = takenAt)

    /** Returns false when the snooze limit is reached. */
    suspend fun snooze(recordId: Long, source: EventSource): Boolean = mutex.withLock {
        val r = doses.get(recordId) ?: return@withLock false
        if (!DoseRules.isOpen(r.status)) return@withLock false
        val s = settings.current()
        val until = DoseRules.snoozeUntil(clock(), s.snoozeMinutes, r.snoozeCount, s.maxSnoozes)
            ?: return@withLock false
        val updated = r.copy(status = DoseStatus.SNOOZED, snoozeUntil = until, snoozeCount = r.snoozeCount + 1)
        doses.changeStatus(updated, event(r.id, r.status, DoseStatus.SNOOZED, source, false))
        notifier.cancel(r.id)
        reconcileLocked("snooze")
        true
    }

    /**
     * Records an action for an occurrence shown on the dashboard that may not have a record yet
     * (e.g. the user takes a dose before its reminder).
     */
    suspend fun actOnOccurrence(med: MedicationEntity, occurrence: Occurrence, status: DoseStatus) = mutex.withLock {
        val existing = doses.findByKey(med.id, occurrence.key.date, occurrence.key.time)
        val now = clock()
        if (existing == null) {
            val id = doses.insertIgnore(newRecord(occurrence, med, status, notified = false, actionAt = now))
            if (id > 0) doses.insertEvent(event(id, null, status, EventSource.APP, false))
        } else if (existing.status != status) {
            doses.changeStatus(
                existing.copy(status = status, actionAt = now, snoozeUntil = null),
                event(existing.id, existing.status, status, EventSource.APP, correction = !DoseRules.isOpen(existing.status)),
            )
            notifier.cancel(existing.id)
        }
        reconcileLocked("occurrence-action")
    }

    /** Called when a medication is paused, archived or deleted: clears its visible reminders. */
    suspend fun clearOpenReminders(medicationId: Long) = mutex.withLock {
        doses.openForMedication(medicationId).forEach { notifier.cancel(it.id) }
    }

    // ---- internals --------------------------------------------------------------------

    private suspend fun act(
        recordId: Long,
        status: DoseStatus,
        source: EventSource,
        correction: Boolean,
        actionAt: Instant? = null,
    ) = mutex.withLock {
        val r = doses.get(recordId) ?: return@withLock
        if (r.status == status && !correction) {
            notifier.cancel(r.id)
            return@withLock
        }
        val at = when (status) {
            DoseStatus.TAKEN, DoseStatus.SKIPPED -> actionAt ?: clock()
            else -> r.actionAt
        }
        val updated = r.copy(status = status, actionAt = at, snoozeUntil = null)
        doses.changeStatus(updated, event(r.id, r.status, status, source, correction))
        notifier.cancel(r.id)
        reconcileLocked("dose-action")
    }

    private suspend fun reconcileLocked(reason: String): ReconcileResult {
        val now = clock()
        val zone = zone()
        val s = settings.current()
        notifier.ensureChannels(s.language)

        // 1. Shown but unanswered for too long -> missed (no response).
        for (r in doses.withStatusBefore(DoseStatus.PENDING, now.minus(DoseRules.NO_RESPONSE_AFTER))) {
            doses.changeStatus(r.copy(status = DoseStatus.MISSED), event(r.id, r.status, DoseStatus.MISSED, EventSource.SYSTEM, false))
            notifier.cancel(r.id)
        }

        val activeMeds = meds.activeForScheduling()
        val byId = activeMeds.associateBy { it.id }

        // 2. Snoozes that are due -> re-announce.
        for (r in doses.snoozesDue(now.plus(ReconcilePlanner.DUE_TOLERANCE))) {
            val updated = r.copy(status = DoseStatus.PENDING, snoozeUntil = null)
            doses.changeStatus(updated, event(r.id, r.status, DoseStatus.PENDING, EventSource.SYSTEM, false))
            val med = byId[r.medicationId]
            if (med != null) {
                notifier.showDose(updated, med, s.language, DoseRules.canSnooze(updated.snoozeCount, s.maxSnoozes), s.snoozeMinutes)
            }
        }

        // 3. Scheduled occurrences.
        val today = now.atZone(zone).toLocalDate()
        val keys = doses.keysFrom(today.minusDays(2))
            .map { OccurrenceKey(it.medicationId, it.scheduledDate, it.scheduledTime) }
            .toSet()
        val plan = ReconcilePlanner.plan(
            meds = activeMeds.map { MedSchedule(it.id, it.spec(), it.effectiveFrom) },
            existingKeys = keys,
            lastRun = settings.lastReconcile(),
            now = now,
            zone = zone,
        )
        var notified = 0
        for (o in plan.notifyNow) {
            val med = byId[o.key.medicationId] ?: continue
            val record = newRecord(o, med, DoseStatus.PENDING, notified = true, actionAt = null)
            val id = doses.insertIgnore(record)
            if (id <= 0) continue // already exists: never notify twice
            doses.insertEvent(event(id, null, DoseStatus.PENDING, EventSource.SYSTEM, false))
            notifier.showDose(record.copy(id = id), med, s.language, DoseRules.canSnooze(0, s.maxSnoozes), s.snoozeMinutes)
            notified++
        }
        var missed = 0
        for (o in plan.missedUndelivered) {
            val med = byId[o.key.medicationId] ?: continue
            val id = doses.insertIgnore(newRecord(o, med, DoseStatus.MISSED, notified = false, actionAt = null))
            if (id > 0) {
                doses.insertEvent(event(id, null, DoseStatus.MISSED, EventSource.SYSTEM, false))
                missed++
            }
        }
        if (missed > 0) notifier.showMissedSummary(missed, s.language)

        // 4. Arm the single rolling alarm.
        val nextSnooze = doses.nextSnooze(now)
        val next = listOfNotNull(plan.nextOccurrence, nextSnooze).minOrNull()
        val mode = if (next != null) scheduler.scheduleNext(next) else {
            scheduler.cancelNext()
            AlarmMode.NONE
        }
        settings.setLastReconcile(now)
        log("reconcile", "reason=$reason mode=$mode notified=$notified missed=$missed next=${next ?: "-"}")
        return ReconcileResult(next, mode, now).also { _last.value = it }
    }

    private fun newRecord(o: Occurrence, med: MedicationEntity, status: DoseStatus, notified: Boolean, actionAt: Instant?) =
        DoseRecordEntity(
            medicationId = med.id,
            scheduledDate = o.key.date,
            scheduledTime = o.key.time,
            scheduledAt = o.instant,
            status = status,
            actionAt = actionAt,
            snoozeUntil = null,
            snoozeCount = 0,
            notified = notified,
            medicationName = med.name,
            doseLabel = med.doseLabel,
        )

    private fun event(recordId: Long, from: DoseStatus?, to: DoseStatus, source: EventSource, correction: Boolean) =
        DoseEventEntity(doseRecordId = recordId, at = clock(), fromStatus = from, toStatus = to, source = source, correction = correction)

    suspend fun log(event: String, detail: String) {
        val dao = db.schedulerLogDao()
        dao.insert(SchedulerLogEntity(at = clock(), event = event, detail = detail))
        dao.prune()
    }

    /**
     * Occurrences of a medication on a given day (dashboard). Times earlier than the creation
     * time are still listed so the day is complete, but never generate a reminder.
     */
    fun occurrencesOn(med: MedicationEntity, date: java.time.LocalDate): List<Occurrence> =
        ScheduleCalculator.occurrencesOn(med.id, med.spec(), date, zone())

    fun now(): Instant = clock()
    fun zoneId(): ZoneId = zone()
}
