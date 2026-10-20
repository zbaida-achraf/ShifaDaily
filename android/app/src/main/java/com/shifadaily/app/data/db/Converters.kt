package com.shifadaily.app.data.db

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

class Converters {
    @TypeConverter fun instantToLong(v: Instant?): Long? = v?.toEpochMilli()
    @TypeConverter fun longToInstant(v: Long?): Instant? = v?.let(Instant::ofEpochMilli)

    @TypeConverter fun dateToLong(v: LocalDate?): Long? = v?.toEpochDay()
    @TypeConverter fun longToDate(v: Long?): LocalDate? = v?.let(LocalDate::ofEpochDay)

    /** Minutes since midnight. */
    @TypeConverter fun timeToInt(v: LocalTime?): Int? = v?.let { it.hour * 60 + it.minute }
    @TypeConverter fun intToTime(v: Int?): LocalTime? = v?.let { LocalTime.of(it / 60, it % 60) }

    @TypeConverter fun timesToString(v: List<LocalTime>): String =
        // Locale.ROOT: an Arabic default locale must not produce Arabic-Indic digits here.
        v.sorted().joinToString(",") { String.format(Locale.ROOT, "%02d:%02d", it.hour, it.minute) }

    @TypeConverter fun stringToTimes(v: String): List<LocalTime> =
        v.split(',').filter { it.isNotBlank() }.map { LocalTime.parse(it.trim()) }

    /** Bit mask, Monday = bit 0. */
    @TypeConverter fun daysToInt(v: Set<DayOfWeek>): Int = v.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }
    @TypeConverter fun intToDays(v: Int): Set<DayOfWeek> =
        DayOfWeek.entries.filter { v and (1 shl (it.value - 1)) != 0 }.toSet()
}
