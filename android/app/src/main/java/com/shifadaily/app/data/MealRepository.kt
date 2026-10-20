package com.shifadaily.app.data

import com.shifadaily.app.data.db.AppDatabase
import com.shifadaily.app.data.db.FoodPreferenceEntity
import com.shifadaily.app.data.db.PantryItemEntity
import com.shifadaily.app.data.db.PreferenceKind
import com.shifadaily.app.data.db.SavedRecipeEntity
import com.shifadaily.app.domain.meal.Allergen
import com.shifadaily.app.domain.meal.Customization
import com.shifadaily.app.domain.meal.PreferenceSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class MealRepository(private val db: AppDatabase, private val clock: () -> Instant = Instant::now) {
    private val dao get() = db.mealDao()

    fun observePantry(): Flow<List<PantryItemEntity>> = dao.observePantry()

    suspend fun addPantryKeys(keys: Collection<String>) {
        keys.forEach { dao.insertPantry(PantryItemEntity(ingredientKey = it, customName = null, addedAt = clock())) }
    }

    suspend fun addPantryCustom(name: String) {
        val n = name.trim().take(60)
        if (n.isNotEmpty()) dao.insertPantry(PantryItemEntity(ingredientKey = null, customName = n, addedAt = clock()))
    }

    suspend fun removePantry(id: Long) = dao.deletePantry(id)
    suspend fun removePantryKey(key: String) = dao.deletePantryKey(key)
    suspend fun clearPantry() = dao.clearPantry()

    fun observePreferenceEntities(): Flow<List<FoodPreferenceEntity>> = dao.observePreferences()

    fun observePreferences(): Flow<PreferenceSet> = dao.observePreferences().map(::toPreferenceSet)

    suspend fun addPreference(kind: PreferenceKind, ingredientKey: String? = null, allergen: Allergen? = null, custom: String? = null) {
        val c = custom?.trim()?.take(60)?.takeIf { it.isNotEmpty() }
        if (ingredientKey == null && allergen == null && c == null) return
        dao.insertPreference(FoodPreferenceEntity(kind = kind, ingredientKey = ingredientKey, allergen = allergen, customName = c, createdAt = clock()))
    }

    suspend fun removePreference(id: Long) = dao.deletePreference(id)

    fun observeSaved(): Flow<List<SavedRecipeEntity>> = dao.observeSaved()
    suspend fun getSaved(id: Long) = dao.getSaved(id)

    suspend fun save(recipeId: String, customization: Customization, existingId: Long?): Long {
        val enc = customization.encode()
        return if (existingId != null) {
            val old = dao.getSaved(existingId)
            if (old != null) {
                dao.updateSaved(old.copy(customization = enc, savedAt = clock()))
                existingId
            } else dao.insertSaved(SavedRecipeEntity(baseRecipeId = recipeId, customization = enc, savedAt = clock()))
        } else {
            dao.insertSaved(SavedRecipeEntity(baseRecipeId = recipeId, customization = enc, savedAt = clock()))
        }
    }

    suspend fun deleteSaved(id: Long) = dao.deleteSaved(id)

    companion object {
        fun toPreferenceSet(list: List<FoodPreferenceEntity>): PreferenceSet {
            fun keys(k: PreferenceKind) = list.filter { it.kind == k }.mapNotNull { it.ingredientKey }.toSet()
            fun custom(k: PreferenceKind) = list.filter { it.kind == k }.mapNotNull { it.customName }
            return PreferenceSet(
                allergyKeys = keys(PreferenceKind.ALLERGY),
                allergens = list.filter { it.kind == PreferenceKind.ALLERGY }.mapNotNull { it.allergen }.toSet(),
                allergyCustom = custom(PreferenceKind.ALLERGY),
                avoidKeys = keys(PreferenceKind.AVOID),
                avoidCustom = custom(PreferenceKind.AVOID),
                likeKeys = keys(PreferenceKind.LIKE),
                dislikeKeys = keys(PreferenceKind.DISLIKE),
            )
        }
    }
}
