package com.shifadaily.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shifadaily.app.AppContainer
import com.shifadaily.app.alarm.ReconcileResult
import com.shifadaily.app.data.db.DoseRecordEntity
import com.shifadaily.app.data.db.MedicationEntity
import com.shifadaily.app.domain.dose.DoseStatus
import com.shifadaily.app.domain.dose.EventSource
import com.shifadaily.app.domain.meal.MealType
import com.shifadaily.app.domain.meal.Recipe
import com.shifadaily.app.domain.meal.RecipeCatalog
import com.shifadaily.app.domain.meal.RecipeEngine
import com.shifadaily.app.domain.meal.SuggestionFilters
import com.shifadaily.app.domain.schedule.Occurrence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class TodayState { UPCOMING, DUE, TAKEN, SKIPPED, SNOOZED, MISSED, BEFORE_START }

data class TodayItem(
    val med: MedicationEntity?,
    val occurrence: Occurrence?,
    val record: DoseRecordEntity?,
    val name: String,
    val dose: String,
    val time: java.time.LocalTime,
    val at: Instant,
    val state: TodayState,
)

data class Adherence(val taken: Int, val total: Int)

data class HomeState(
    val now: Instant,
    val today: LocalDate,
    val items: List<TodayItem>,
    val attention: List<DoseRecordEntity>,
    val adherence: Adherence,
    val hasMedications: Boolean,
    val meal: Recipe?,
    val mealType: MealType,
    val lastReconcile: ReconcileResult?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val c: AppContainer) : ViewModel() {

    private val ticker = flow {
        while (true) {
            emit(Instant.now())
            delay(30_000)
        }
    }

    private val dayFlow = ticker.map { it.atZone(ZoneId.systemDefault()).toLocalDate() }

    val state: StateFlow<HomeState?> = combine(
        ticker,
        c.medications.observeCurrent(),
        dayFlow.flatMapLatest { day -> c.db.doseDao().observeBetween(day.minusDays(6), day) },
        combine(c.meals.observePantry(), c.meals.observePreferences(), c.settings.settings) { a, b, s -> Triple(a, b, s) },
        c.engine.lastResult,
    ) { now, meds, records, (pantry, prefs, settings), last ->
        val zone = ZoneId.systemDefault()
        val today = now.atZone(zone).toLocalDate()
        val todayRecords = records.filter { it.scheduledDate == today }
        val byKey = todayRecords.associateBy { Triple(it.medicationId, it.scheduledDate, it.scheduledTime) }

        val items = ArrayList<TodayItem>()
        val covered = HashSet<Long>()
        for (med in meds.filter { !it.paused }) {
            for (o in c.engine.occurrencesOn(med, today)) {
                val rec = byKey[Triple(med.id, today, o.key.time)]
                if (rec != null) covered += rec.id
                val st = when {
                    rec != null -> stateOf(rec.status)
                    !o.instant.isAfter(med.effectiveFrom) -> TodayState.BEFORE_START
                    o.instant.isAfter(now) -> TodayState.UPCOMING
                    else -> TodayState.DUE
                }
                items += TodayItem(med, o, rec, med.name, med.doseLabel, o.key.time, o.instant, st)
            }
        }
        // Records for today whose medication is paused or archived are still shown.
        val medsById = meds.associateBy { it.id }
        for (r in todayRecords) {
            if (r.id in covered) continue
            items += TodayItem(medsById[r.medicationId], null, r, r.medicationName, r.doseLabel, r.scheduledTime, r.scheduledAt, stateOf(r.status))
        }
        items.sortBy { it.at }

        val finished = records.filter { it.status != DoseStatus.PENDING && it.status != DoseStatus.SNOOZED }
        val attention = records.filter {
            it.scheduledDate.isBefore(today) && (it.status == DoseStatus.MISSED || it.status == DoseStatus.PENDING) &&
                Duration.between(it.scheduledAt, now) < Duration.ofDays(2)
        }

        val mealType = mealTypeFor(now.atZone(zone).hour)
        val suggestions = RecipeEngine.suggest(
            RecipeCatalog.all,
            pantry.mapNotNull { it.ingredientKey }.toSet(),
            prefs,
            SuggestionFilters(mealType = mealType, maxMinutes = settings.maxCookingMinutes, budget = settings.budget, diet = settings.dietPattern),
        ).suggestions
        // Stable for the whole day; prefers recipes that match the pantry.
        val top = suggestions.filter { it.score >= (suggestions.firstOrNull()?.score ?: 0.0) - 0.001 }
        val meal = top.getOrNull(today.dayOfYear % top.size.coerceAtLeast(1))?.recipe

        HomeState(
            now = now,
            today = today,
            items = items,
            attention = attention,
            adherence = Adherence(finished.count { it.status == DoseStatus.TAKEN }, finished.size),
            hasMedications = meds.isNotEmpty(),
            meal = meal,
            mealType = mealType,
            lastReconcile = last,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun take(item: TodayItem) = act(item, DoseStatus.TAKEN)
    fun skip(item: TodayItem) = act(item, DoseStatus.SKIPPED)

    private fun act(item: TodayItem, status: DoseStatus) {
        viewModelScope.launch {
            val r = item.record
            when {
                r != null && status == DoseStatus.TAKEN -> c.engine.markTaken(r.id, EventSource.APP)
                r != null -> c.engine.markSkipped(r.id, EventSource.APP)
                item.med != null && item.occurrence != null -> c.engine.actOnOccurrence(item.med, item.occurrence, status)
            }
        }
    }

    suspend fun record(id: Long) = c.db.doseDao().get(id)

    fun actOnRecord(id: Long, status: DoseStatus) = viewModelScope.launch {
        when (status) {
            DoseStatus.TAKEN -> c.engine.markTaken(id, EventSource.APP)
            DoseStatus.SKIPPED -> c.engine.markSkipped(id, EventSource.APP)
            DoseStatus.SNOOZED -> c.engine.snooze(id, EventSource.APP)
            else -> Unit
        }
    }

    companion object {
        fun stateOf(s: DoseStatus) = when (s) {
            DoseStatus.PENDING -> TodayState.DUE
            DoseStatus.TAKEN -> TodayState.TAKEN
            DoseStatus.SKIPPED -> TodayState.SKIPPED
            DoseStatus.SNOOZED -> TodayState.SNOOZED
            DoseStatus.MISSED -> TodayState.MISSED
        }

        fun mealTypeFor(hour: Int) = when (hour) {
            in 4..10 -> MealType.BREAKFAST
            in 11..14 -> MealType.LUNCH
            in 15..17 -> MealType.SNACK
            else -> MealType.DINNER
        }
    }
}
