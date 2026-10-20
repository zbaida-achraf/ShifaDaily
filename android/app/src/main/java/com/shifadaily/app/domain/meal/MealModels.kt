package com.shifadaily.app.domain.meal

enum class Lang { EN, AR }

/** Bilingual text for curated data (recipes, ingredients). UI strings live in res/values*. */
data class LText(val en: String, val ar: String) {
    fun get(lang: Lang) = if (lang == Lang.AR) ar else en
}

/**
 * Consumption tiers from the patient-education leaflet "من أجل نظام غذائي متوازن"
 * (For a balanced diet, Sothema, Morocco) supplied by the user.
 */
enum class Tier {
    /** إستهلاك حسب الرغبة — as desired. */
    FREELY,
    /** إستهلاك محدود — limited. */
    LIMITED,
    /** إستهلاك في بعض الأحيان بكميات قليلة — occasionally, in small amounts. */
    OCCASIONAL,
    /** Not covered by the leaflet (herbs, spices, salt...). */
    NOT_LISTED,
}

enum class IngredientCategory { VEGETABLE, FRUIT, PROTEIN, LEGUME, STARCH, DAIRY, FAT, NUT, HERB_SPICE, DRINK, SWEET }

/** The 14 major allergen groups (EU / Codex lists). */
enum class Allergen { GLUTEN, CRUSTACEANS, EGGS, FISH, PEANUTS, SOY, MILK, TREE_NUTS, CELERY, MUSTARD, SESAME, SULPHITES, LUPIN, MOLLUSCS }

enum class DietPattern { DIABETES_CONSCIOUS, VEGETARIAN, STANDARD }

enum class Budget { LOW, MEDIUM, ANY }

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }

enum class QtyUnit { G, ML, PIECE, SLICE, CLOVE, STALK, TBSP, TSP, BUNCH, PINCH, TO_TASTE }

enum class CookingMethod { NO_COOK, STOVETOP_LITTLE_OIL, OVEN, STEAM, TAGINE }

data class Ingredient(
    val key: String,
    val name: LText,
    /** Form used inside instructions ("the tomatoes" / "الطماطم"). */
    val def: LText,
    val category: IngredientCategory,
    val tier: Tier,
    /** True when the item is pictured in the leaflet; false when its tier is inferred from the leaflet text. */
    val pictured: Boolean,
    val allergens: Set<Allergen> = emptySet(),
    val vegetarian: Boolean = true,
    /** Extra words used to recognise free-text pantry entries (EN/FR/AR). */
    val synonyms: List<String> = emptyList(),
    /** Basic seasoning most kitchens have; ignored when ranking by pantry match. */
    val staple: Boolean = false,
)

data class RecipeIngredient(
    val key: String,
    /** Quantity for the recipe's base servings; null with TO_TASTE. */
    val qty: Double?,
    val unit: QtyUnit,
    val optional: Boolean = false,
    /** Core ingredients can be replaced but not removed. */
    val core: Boolean = false,
    val substitutes: List<String> = emptyList(),
    val scalable: Boolean = true,
)

/** A piece of an instruction: fixed text, or a clause that only appears while its ingredients are present. */
sealed interface StepPart {
    data class Text(val text: LText) : StepPart
    /** [text] contains "{x}", replaced with the present ingredients' names. */
    data class Clause(val keys: List<String>, val text: LText) : StepPart
}

data class Step(
    val parts: List<StepPart>,
    /** The step is dropped when all of these ingredients are removed. Empty = always kept. */
    val requires: List<String> = emptyList(),
)

data class Recipe(
    val id: String,
    val title: LText,
    val description: LText,
    val mealTypes: Set<MealType>,
    val minutes: Int,
    val servings: Int,
    val budget: Budget,
    val method: CookingMethod,
    val ingredients: List<RecipeIngredient>,
    val steps: List<Step>,
)
