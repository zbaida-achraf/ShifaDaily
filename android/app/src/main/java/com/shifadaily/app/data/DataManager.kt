package com.shifadaily.app.data

import android.content.Context
import android.net.Uri
import com.shifadaily.app.BuildConfig
import com.shifadaily.app.alarm.AlarmScheduler
import com.shifadaily.app.alarm.ReminderNotifier
import com.shifadaily.app.data.db.AppDatabase
import com.shifadaily.app.data.prefs.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Export to a user-chosen file, and full local erase. Nothing is ever uploaded. */
class DataManager(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val notifier: ReminderNotifier,
    private val scheduler: AlarmScheduler,
) {
    /** Writes a human-readable JSON export to [uri] (chosen via the system file picker). */
    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "ShifaDaily")
        root.put("appVersion", BuildConfig.VERSION_NAME)
        root.put("exportedAt", Instant.now().toString())
        root.put("format", 1)

        root.put("medications", JSONArray().apply {
            db.medicationDao().all().forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id); put("name", m.name); put("doseAmount", m.doseAmount); put("doseUnit", m.doseUnit)
                    put("form", m.form.name); put("formOther", m.formOther ?: JSONObject.NULL)
                    put("notes", m.notes ?: JSONObject.NULL); put("instructions", m.instructions ?: JSONObject.NULL)
                    put("foodInstruction", m.foodInstruction.name); put("foodInstructionNote", m.foodInstructionNote ?: JSONObject.NULL)
                    put("scheduleType", m.scheduleType.name); put("startDate", m.startDate.toString())
                    put("endDate", m.endDate?.toString() ?: JSONObject.NULL)
                    put("times", JSONArray(m.times.map { it.toString() }))
                    put("weekdays", JSONArray(m.weekdays.map { it.name }))
                    put("intervalDays", m.intervalDays); put("paused", m.paused); put("alertStyle", m.alertStyle.name)
                    put("createdAt", m.createdAt.toString()); put("archivedAt", m.archivedAt?.toString() ?: JSONObject.NULL)
                })
            }
        })
        root.put("doseRecords", JSONArray().apply {
            db.doseDao().allRecords().forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id); put("medicationId", r.medicationId); put("medicationName", r.medicationName)
                    put("dose", r.doseLabel); put("scheduledDate", r.scheduledDate.toString()); put("scheduledTime", r.scheduledTime.toString())
                    put("scheduledAt", r.scheduledAt.toString()); put("status", r.status.name)
                    put("actionAt", r.actionAt?.toString() ?: JSONObject.NULL); put("snoozeCount", r.snoozeCount); put("notified", r.notified)
                })
            }
        })
        root.put("doseEvents", JSONArray().apply {
            db.doseDao().allEvents().forEach { e ->
                put(JSONObject().apply {
                    put("doseRecordId", e.doseRecordId); put("at", e.at.toString()); put("from", e.fromStatus?.name ?: JSONObject.NULL)
                    put("to", e.toStatus.name); put("source", e.source.name); put("correction", e.correction)
                })
            }
        })
        val meal = db.mealDao()
        root.put("pantry", JSONArray(meal.allPantry().map { it.ingredientKey ?: it.customName }))
        root.put("foodPreferences", JSONArray().apply {
            meal.allPreferences().forEach { p ->
                put(JSONObject().apply {
                    put("kind", p.kind.name); put("ingredient", p.ingredientKey ?: JSONObject.NULL)
                    put("allergen", p.allergen?.name ?: JSONObject.NULL); put("custom", p.customName ?: JSONObject.NULL)
                })
            }
        })
        root.put("savedRecipes", JSONArray().apply {
            meal.allSaved().forEach { s ->
                put(JSONObject().apply { put("recipe", s.baseRecipeId); put("customization", s.customization); put("savedAt", s.savedAt.toString()) })
            }
        })

        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
            ?: error("Cannot open output")
    }

    /** Erases every local record and preference, cancels alarms and notifications. */
    suspend fun eraseAll() = withContext(Dispatchers.IO) {
        scheduler.cancelNext()
        notifier.cancelAll()
        db.clearAllTables()
        settings.clearAll()
    }
}
