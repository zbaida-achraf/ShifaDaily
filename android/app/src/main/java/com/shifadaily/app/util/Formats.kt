package com.shifadaily.app.util

import android.content.Context
import android.text.format.DateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Locale-aware formatting. Digits stay Western (0-9) in Arabic, as commonly used in the
 * Maghreb; month/day names and AM/PM markers follow the selected language.
 */
object Formats {
    fun currentLocale(context: Context): Locale = context.resources.configuration.locales[0]

    fun time(context: Context, t: LocalTime): String {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        return DateTimeFormatter.ofPattern(pattern, currentLocale(context)).format(t)
    }

    fun date(context: Context, d: LocalDate): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(currentLocale(context)).format(d)

    fun longDate(context: Context, d: LocalDate): String =
        DateTimeFormatter.ofPattern("EEEE d MMMM", currentLocale(context)).format(d)

    fun shortDay(context: Context, d: java.time.DayOfWeek): String =
        d.getDisplayName(java.time.format.TextStyle.SHORT, currentLocale(context))
}
