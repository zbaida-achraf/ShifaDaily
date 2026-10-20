package com.shifadaily.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shifadaily.app.domain.dose.DoseStatus
import com.shifadaily.app.domain.dose.EventSource
import com.shifadaily.app.domain.meal.Allergen
import com.shifadaily.app.domain.schedule.ScheduleSpec
import com.shifadaily.app.domain.schedule.ScheduleType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

enum class MedicationForm { TABLET, CAPSULE, LIQUID, INJECTION, INHALER, DROPS, CREAM, PATCH, OTHER }

enum class FoodInstruction { NONE, BEFORE_FOOD, WITH_FOOD, AFTER_FOOD, EMPTY_STOMACH, CUSTOM }

/** Maps to a notification channel; see ReminderNotifier. */
enum class AlertStyle { SOUND, VIBRATE, SILENT }

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Entered exactly as prescribed, e.g. "500" or "1/2". Never computed by the app. */
    val doseAmount: String,
    val doseUnit: String,
    val form: MedicationForm,
    val formOther: String?,
    val notes: String?,
    val instructions: String?,
    val foodInstruction: FoodInstruction,
    val foodInstructionNote: String?,
    val scheduleType: ScheduleType,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val times: List<LocalTime>,
    val weekdays: Set<DayOfWeek>,
    val intervalDays: Int,
    val paused: Boolean,
    val alertStyle: AlertStyle,
    /** Occurrences at or before this instant never generate reminders (creation/edit/resume). */
    val effectiveFrom: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** Archived = reminders stopped, history kept. */
    val archivedAt: Instant?,
) {
    fun spec() = ScheduleSpec(scheduleType, startDate, endDate, times, weekdays, intervalDays)
    val doseLabel: String get() = listOf(doseAmount.trim(), doseUnit.trim()).filter { it.isNotEmpty() }.joinToString(" ")
}

@Entity(
    tableName = "dose_records",
    foreignKeys = [ForeignKey(
        entity = MedicationEntity::class,
        parentColumns = ["id"],
        childColumns = ["medicationId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [
        Index(value = ["medicationId", "scheduledDate", "scheduledTime"], unique = true),
        Index(value = ["scheduledAt"]),
        Index(value = ["status"]),
    ],
)
data class DoseRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val scheduledDate: LocalDate,
    val scheduledTime: LocalTime,
    val scheduledAt: Instant,
    val status: DoseStatus,
    /** When the user actually acted (taken/skipped), separate from scheduledAt. */
    val actionAt: Instant?,
    val snoozeUntil: Instant?,
    val snoozeCount: Int,
    /** False when the reminder could not be delivered (device off, etc.). */
    val notified: Boolean,
    /** Snapshot so history stays readable after the medication is edited. */
    val medicationName: String,
    val doseLabel: String,
)

/** Audit trail: every status change, including user corrections. */
@Entity(
    tableName = "dose_events",
    foreignKeys = [ForeignKey(
        entity = DoseRecordEntity::class,
        parentColumns = ["id"],
        childColumns = ["doseRecordId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("doseRecordId")],
)
data class DoseEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val doseRecordId: Long,
    val at: Instant,
    val fromStatus: DoseStatus?,
    val toStatus: DoseStatus,
    val source: EventSource,
    val correction: Boolean,
)

/** Local troubleshooting log. Never contains medication names or doses. */
@Entity(tableName = "scheduler_log")
data class SchedulerLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Instant,
    val event: String,
    val detail: String,
)

@Entity(tableName = "pantry_items", indices = [Index(value = ["ingredientKey"], unique = true)])
data class PantryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Catalog key, or null for a free-text item. */
    val ingredientKey: String?,
    val customName: String?,
    val addedAt: Instant,
)

enum class PreferenceKind { LIKE, DISLIKE, AVOID, ALLERGY }

@Entity(tableName = "food_preferences")
data class FoodPreferenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: PreferenceKind,
    val ingredientKey: String?,
    /** Allergy by category (e.g. all tree nuts). Only for kind = ALLERGY. */
    val allergen: Allergen?,
    val customName: String?,
    val createdAt: Instant,
)

@Entity(tableName = "saved_recipes")
data class SavedRecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val baseRecipeId: String,
    /** Encoded [com.shifadaily.app.domain.meal.Customization]. */
    val customization: String,
    val savedAt: Instant,
)
