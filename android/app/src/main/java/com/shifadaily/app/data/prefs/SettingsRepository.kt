package com.shifadaily.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shifadaily.app.domain.meal.Budget
import com.shifadaily.app.domain.meal.DietPattern
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    /** "en" or "ar"; null until the user picks one. */
    val language: String?,
    val theme: ThemeMode,
    val onboardingDone: Boolean,
    val disclaimerAccepted: Boolean,
    val snoozeMinutes: Int,
    val maxSnoozes: Int,
    val dietPattern: DietPattern,
    val defaultServings: Int,
    /** 0 = no limit. */
    val maxCookingMinutes: Int,
    val budget: Budget,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val language = stringPreferencesKey("language")
        val theme = stringPreferencesKey("theme")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val disclaimerAccepted = booleanPreferencesKey("disclaimer_accepted")
        val snoozeMinutes = intPreferencesKey("snooze_minutes")
        val maxSnoozes = intPreferencesKey("max_snoozes")
        val dietPattern = stringPreferencesKey("diet_pattern")
        val defaultServings = intPreferencesKey("default_servings")
        val maxCookingMinutes = intPreferencesKey("max_cooking_minutes")
        val budget = stringPreferencesKey("budget")
        val lastReconcile = longPreferencesKey("last_reconcile")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            language = p[K.language],
            theme = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            onboardingDone = p[K.onboardingDone] ?: false,
            disclaimerAccepted = p[K.disclaimerAccepted] ?: false,
            snoozeMinutes = p[K.snoozeMinutes] ?: 10,
            maxSnoozes = p[K.maxSnoozes] ?: 3,
            dietPattern = p[K.dietPattern]?.let { runCatching { DietPattern.valueOf(it) }.getOrNull() }
                ?: DietPattern.DIABETES_CONSCIOUS,
            defaultServings = p[K.defaultServings] ?: 2,
            maxCookingMinutes = p[K.maxCookingMinutes] ?: 0,
            budget = p[K.budget]?.let { runCatching { Budget.valueOf(it) }.getOrNull() } ?: Budget.ANY,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setLanguage(tag: String) = context.dataStore.edit { it[K.language] = tag }
    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[K.theme] = mode.name }
    suspend fun setOnboardingDone() = context.dataStore.edit { it[K.onboardingDone] = true }
    suspend fun setDisclaimerAccepted() = context.dataStore.edit { it[K.disclaimerAccepted] = true }
    suspend fun setSnoozeMinutes(v: Int) = context.dataStore.edit { it[K.snoozeMinutes] = v }
    suspend fun setMaxSnoozes(v: Int) = context.dataStore.edit { it[K.maxSnoozes] = v }
    suspend fun setDietPattern(v: DietPattern) = context.dataStore.edit { it[K.dietPattern] = v.name }
    suspend fun setDefaultServings(v: Int) = context.dataStore.edit { it[K.defaultServings] = v }
    suspend fun setMaxCookingMinutes(v: Int) = context.dataStore.edit { it[K.maxCookingMinutes] = v }
    suspend fun setBudget(v: Budget) = context.dataStore.edit { it[K.budget] = v.name }

    suspend fun lastReconcile(): Instant? =
        context.dataStore.data.first()[K.lastReconcile]?.let(Instant::ofEpochMilli)

    suspend fun setLastReconcile(at: Instant) = context.dataStore.edit { it[K.lastReconcile] = at.toEpochMilli() }

    suspend fun clearAll() = context.dataStore.edit { it.clear() }
}
