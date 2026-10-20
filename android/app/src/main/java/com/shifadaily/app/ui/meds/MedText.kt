package com.shifadaily.app.ui.meds

import android.content.Context
import androidx.annotation.StringRes
import com.shifadaily.app.R
import com.shifadaily.app.data.db.AlertStyle
import com.shifadaily.app.data.db.FoodInstruction
import com.shifadaily.app.data.db.MedicationEntity
import com.shifadaily.app.data.db.MedicationForm
import com.shifadaily.app.domain.schedule.MedicationError
import com.shifadaily.app.domain.schedule.ScheduleType
import com.shifadaily.app.util.Formats
import java.time.DayOfWeek
import java.time.temporal.WeekFields

@StringRes
fun formLabel(f: MedicationForm) = when (f) {
    MedicationForm.TABLET -> R.string.form_tablet
    MedicationForm.CAPSULE -> R.string.form_capsule
    MedicationForm.LIQUID -> R.string.form_liquid
    MedicationForm.INJECTION -> R.string.form_injection
    MedicationForm.INHALER -> R.string.form_inhaler
    MedicationForm.DROPS -> R.string.form_drops
    MedicationForm.CREAM -> R.string.form_cream
    MedicationForm.PATCH -> R.string.form_patch
    MedicationForm.OTHER -> R.string.form_other
}

@StringRes
fun foodLabel(f: FoodInstruction) = when (f) {
    FoodInstruction.NONE -> R.string.food_none
    FoodInstruction.BEFORE_FOOD -> R.string.food_before
    FoodInstruction.WITH_FOOD -> R.string.food_with
    FoodInstruction.AFTER_FOOD -> R.string.food_after
    FoodInstruction.EMPTY_STOMACH -> R.string.food_empty
    FoodInstruction.CUSTOM -> R.string.food_custom
}

@StringRes
fun alertLabel(a: AlertStyle) = when (a) {
    AlertStyle.SOUND -> R.string.alert_sound
    AlertStyle.VIBRATE -> R.string.alert_vibrate
    AlertStyle.SILENT -> R.string.alert_silent
}

@StringRes
fun scheduleTypeLabel(t: ScheduleType) = when (t) {
    ScheduleType.DAILY -> R.string.schedule_daily
    ScheduleType.WEEKDAYS -> R.string.schedule_weekdays
    ScheduleType.INTERVAL -> R.string.schedule_interval
}

@StringRes
fun errorLabel(e: MedicationError) = when (e) {
    MedicationError.NAME_REQUIRED -> R.string.err_name_required
    MedicationError.NAME_TOO_LONG -> R.string.err_name_too_long
    MedicationError.DOSE_REQUIRED -> R.string.err_dose_required
    MedicationError.NO_TIMES -> R.string.err_no_times
    MedicationError.DUPLICATE_TIMES -> R.string.err_duplicate_times
    MedicationError.NO_WEEKDAYS -> R.string.err_no_weekdays
    MedicationError.INTERVAL_OUT_OF_RANGE -> R.string.err_interval
    MedicationError.END_BEFORE_START -> R.string.err_end_before_start
    MedicationError.NO_OCCURRENCES -> R.string.err_no_occurrences
}

/** Weekdays in the locale's order (Saturday/Sunday/Monday first). */
fun orderedWeekdays(context: Context): List<DayOfWeek> {
    val first = WeekFields.of(Formats.currentLocale(context)).firstDayOfWeek
    return (0..6).map { first.plus(it.toLong()) }
}

fun scheduleSummary(context: Context, med: MedicationEntity): String {
    val times = med.times.sorted().joinToString("، ".takeIf { isArabic(context) } ?: ", ") { Formats.time(context, it) }
    val pattern = when (med.scheduleType) {
        ScheduleType.DAILY -> context.getString(R.string.schedule_daily)
        ScheduleType.WEEKDAYS -> orderedWeekdays(context).filter { it in med.weekdays }
            .joinToString(" ") { Formats.shortDay(context, it) }
        ScheduleType.INTERVAL -> context.resources.getQuantityString(R.plurals.every_n_days, med.intervalDays, med.intervalDays)
    }
    val base = "$pattern · $times"
    return med.endDate?.let { base + " · " + context.getString(R.string.until_date, Formats.date(context, it)) } ?: base
}

private fun isArabic(context: Context) = Formats.currentLocale(context).language == "ar"
