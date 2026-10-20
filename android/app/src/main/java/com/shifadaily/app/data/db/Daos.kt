package com.shifadaily.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.shifadaily.app.domain.dose.DoseStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE archivedAt IS NULL ORDER BY name COLLATE NOCASE")
    fun observeCurrent(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE archivedAt IS NOT NULL ORDER BY archivedAt DESC")
    fun observeArchived(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :id")
    fun observe(id: Long): Flow<MedicationEntity?>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun get(id: Long): MedicationEntity?

    @Query("SELECT * FROM medications WHERE archivedAt IS NULL AND paused = 0")
    suspend fun activeForScheduling(): List<MedicationEntity>

    @Query("SELECT * FROM medications")
    suspend fun all(): List<MedicationEntity>

    @Insert
    suspend fun insert(med: MedicationEntity): Long

    @Update
    suspend fun update(med: MedicationEntity)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun delete(id: Long)
}

data class RecordKeyRow(val medicationId: Long, val scheduledDate: LocalDate, val scheduledTime: LocalTime)

@Dao
interface DoseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(record: DoseRecordEntity): Long

    @Update
    suspend fun update(record: DoseRecordEntity)

    @Insert
    suspend fun insertEvent(event: DoseEventEntity): Long

    @Query("SELECT * FROM dose_records WHERE id = :id")
    suspend fun get(id: Long): DoseRecordEntity?

    @Query(
        "SELECT * FROM dose_records WHERE medicationId = :medId AND scheduledDate = :date AND scheduledTime = :time"
    )
    suspend fun findByKey(medId: Long, date: LocalDate, time: LocalTime): DoseRecordEntity?

    @Query("SELECT medicationId, scheduledDate, scheduledTime FROM dose_records WHERE scheduledDate >= :fromDate")
    suspend fun keysFrom(fromDate: LocalDate): List<RecordKeyRow>

    @Query("SELECT * FROM dose_records WHERE status = :status AND scheduledAt < :before")
    suspend fun withStatusBefore(status: DoseStatus, before: Instant): List<DoseRecordEntity>

    @Query("SELECT * FROM dose_records WHERE status = 'SNOOZED' AND snoozeUntil IS NOT NULL AND snoozeUntil <= :at")
    suspend fun snoozesDue(at: Instant): List<DoseRecordEntity>

    @Query("SELECT MIN(snoozeUntil) FROM dose_records WHERE status = 'SNOOZED' AND snoozeUntil > :after")
    suspend fun nextSnooze(after: Instant): Instant?

    @Query("SELECT * FROM dose_records WHERE medicationId = :medId AND status IN ('PENDING','SNOOZED')")
    suspend fun openForMedication(medId: Long): List<DoseRecordEntity>

    @Query("SELECT * FROM dose_records WHERE scheduledDate BETWEEN :from AND :to ORDER BY scheduledAt")
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<DoseRecordEntity>>

    @Query("SELECT * FROM dose_records WHERE scheduledAt <= :now AND status IN ('PENDING','SNOOZED','MISSED') AND scheduledAt >= :since ORDER BY scheduledAt DESC")
    fun observeNeedingAttention(since: Instant, now: Instant): Flow<List<DoseRecordEntity>>

    @Query("SELECT * FROM dose_records WHERE medicationId = :medId ORDER BY scheduledAt DESC LIMIT :limit")
    fun observeForMedication(medId: Long, limit: Int): Flow<List<DoseRecordEntity>>

    @Query("SELECT * FROM dose_records ORDER BY scheduledAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DoseRecordEntity>>

    @Query("SELECT * FROM dose_events WHERE doseRecordId = :recordId ORDER BY at")
    suspend fun eventsFor(recordId: Long): List<DoseEventEntity>

    @Query("SELECT * FROM dose_records")
    suspend fun allRecords(): List<DoseRecordEntity>

    @Query("SELECT * FROM dose_events")
    suspend fun allEvents(): List<DoseEventEntity>

    @Transaction
    suspend fun changeStatus(updated: DoseRecordEntity, event: DoseEventEntity) {
        update(updated)
        insertEvent(event)
    }
}

@Dao
interface SchedulerLogDao {
    @Insert
    suspend fun insert(entry: SchedulerLogEntity)

    @Query("DELETE FROM scheduler_log WHERE id NOT IN (SELECT id FROM scheduler_log ORDER BY id DESC LIMIT 200)")
    suspend fun prune()

    @Query("SELECT * FROM scheduler_log ORDER BY id DESC LIMIT 50")
    fun observeRecent(): Flow<List<SchedulerLogEntity>>
}

@Dao
interface MealDao {
    @Query("SELECT * FROM pantry_items ORDER BY addedAt DESC")
    fun observePantry(): Flow<List<PantryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPantry(item: PantryItemEntity): Long

    @Query("DELETE FROM pantry_items WHERE id = :id")
    suspend fun deletePantry(id: Long)

    @Query("DELETE FROM pantry_items WHERE ingredientKey = :key")
    suspend fun deletePantryKey(key: String)

    @Query("DELETE FROM pantry_items")
    suspend fun clearPantry()

    @Query("SELECT * FROM food_preferences ORDER BY createdAt")
    fun observePreferences(): Flow<List<FoodPreferenceEntity>>

    @Insert
    suspend fun insertPreference(pref: FoodPreferenceEntity): Long

    @Query("DELETE FROM food_preferences WHERE id = :id")
    suspend fun deletePreference(id: Long)

    @Query("SELECT * FROM saved_recipes ORDER BY savedAt DESC")
    fun observeSaved(): Flow<List<SavedRecipeEntity>>

    @Query("SELECT * FROM saved_recipes WHERE id = :id")
    suspend fun getSaved(id: Long): SavedRecipeEntity?

    @Insert
    suspend fun insertSaved(recipe: SavedRecipeEntity): Long

    @Update
    suspend fun updateSaved(recipe: SavedRecipeEntity)

    @Query("DELETE FROM saved_recipes WHERE id = :id")
    suspend fun deleteSaved(id: Long)

    @Query("SELECT * FROM pantry_items") suspend fun allPantry(): List<PantryItemEntity>
    @Query("SELECT * FROM food_preferences") suspend fun allPreferences(): List<FoodPreferenceEntity>
    @Query("SELECT * FROM saved_recipes") suspend fun allSaved(): List<SavedRecipeEntity>
}
