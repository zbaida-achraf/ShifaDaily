package com.shifadaily.app.data

import com.shifadaily.app.alarm.ReminderEngine
import com.shifadaily.app.data.db.AppDatabase
import com.shifadaily.app.data.db.MedicationEntity
import java.time.Instant

/** Medication CRUD. Every change re-arms reminders through [ReminderEngine.reconcile]. */
class MedicationRepository(
    private val db: AppDatabase,
    private val engine: ReminderEngine,
    private val clock: () -> Instant = Instant::now,
) {
    private val dao get() = db.medicationDao()

    fun observeCurrent() = dao.observeCurrent()
    fun observeArchived() = dao.observeArchived()
    fun observe(id: Long) = dao.observe(id)
    suspend fun get(id: Long) = dao.get(id)

    suspend fun create(med: MedicationEntity): Long {
        val now = clock()
        val id = dao.insert(med.copy(id = 0, createdAt = now, updatedAt = now, effectiveFrom = now, archivedAt = null))
        engine.reconcile("med-created")
        return id
    }

    /**
     * Saves edits. When the schedule changed, reminders restart from now so edited times
     * earlier today are not reported as missed.
     */
    suspend fun update(med: MedicationEntity) {
        val old = dao.get(med.id) ?: return
        val now = clock()
        val scheduleChanged = old.spec() != med.spec()
        dao.update(
            med.copy(
                createdAt = old.createdAt,
                updatedAt = now,
                effectiveFrom = if (scheduleChanged) now else old.effectiveFrom,
            )
        )
        if (scheduleChanged) engine.clearOpenReminders(med.id)
        engine.reconcile("med-updated")
    }

    suspend fun setPaused(id: Long, paused: Boolean) {
        val med = dao.get(id) ?: return
        val now = clock()
        // On resume, doses that fell inside the pause are not reported as missed.
        dao.update(med.copy(paused = paused, updatedAt = now, effectiveFrom = if (!paused) now else med.effectiveFrom))
        if (paused) engine.clearOpenReminders(id)
        engine.reconcile(if (paused) "med-paused" else "med-resumed")
    }

    /** Stops reminders but keeps the medication and its dose history. */
    suspend fun archive(id: Long) {
        val med = dao.get(id) ?: return
        engine.clearOpenReminders(id)
        dao.update(med.copy(archivedAt = clock(), updatedAt = clock()))
        engine.reconcile("med-archived")
    }

    suspend fun restore(id: Long) {
        val med = dao.get(id) ?: return
        val now = clock()
        dao.update(med.copy(archivedAt = null, updatedAt = now, effectiveFrom = now))
        engine.reconcile("med-restored")
    }

    /** Permanently deletes the medication AND its dose history (cascade). The UI warns first. */
    suspend fun deleteWithHistory(id: Long) {
        engine.clearOpenReminders(id)
        dao.delete(id)
        engine.reconcile("med-deleted")
    }
}
