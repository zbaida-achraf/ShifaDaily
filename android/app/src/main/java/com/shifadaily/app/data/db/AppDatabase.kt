package com.shifadaily.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        MedicationEntity::class,
        DoseRecordEntity::class,
        DoseEventEntity::class,
        SchedulerLogEntity::class,
        PantryItemEntity::class,
        FoodPreferenceEntity::class,
        SavedRecipeEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun doseDao(): DoseDao
    abstract fun schedulerLogDao(): SchedulerLogDao
    abstract fun mealDao(): MealDao

    companion object {
        const val NAME = "shifadaily.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                // No destructive fallback: health history must never be silently wiped.
                .build()
    }
}
