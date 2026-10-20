package com.shifadaily.app.ui.meals

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.shifadaily.app.R
import com.shifadaily.app.domain.meal.Allergen
import com.shifadaily.app.domain.meal.Budget
import com.shifadaily.app.domain.meal.CookingMethod
import com.shifadaily.app.domain.meal.DietPattern
import com.shifadaily.app.domain.meal.Lang
import com.shifadaily.app.domain.meal.MealType
import com.shifadaily.app.domain.meal.QtyUnit

@Composable
fun currentLang(): Lang = if (LocalConfiguration.current.locales[0].language == "ar") Lang.AR else Lang.EN

@StringRes
fun mealTypeLabel(t: MealType) = when (t) {
    MealType.BREAKFAST -> R.string.meal_breakfast
    MealType.LUNCH -> R.string.meal_lunch
    MealType.DINNER -> R.string.meal_dinner
    MealType.SNACK -> R.string.meal_snack
}

@StringRes
fun budgetLabel(b: Budget) = when (b) {
    Budget.LOW -> R.string.budget_low
    Budget.MEDIUM -> R.string.budget_medium
    Budget.ANY -> R.string.budget_any
}

@StringRes
fun dietLabel(d: DietPattern) = when (d) {
    DietPattern.DIABETES_CONSCIOUS -> R.string.diet_diabetes
    DietPattern.VEGETARIAN -> R.string.diet_vegetarian
    DietPattern.STANDARD -> R.string.diet_standard
}

@StringRes
fun methodLabel(m: CookingMethod) = when (m) {
    CookingMethod.NO_COOK -> R.string.method_no_cook
    CookingMethod.STOVETOP_LITTLE_OIL -> R.string.method_stovetop
    CookingMethod.OVEN -> R.string.method_oven
    CookingMethod.STEAM -> R.string.method_steam
    CookingMethod.TAGINE -> R.string.method_tagine
}

@StringRes
fun unitLabel(u: QtyUnit) = when (u) {
    QtyUnit.G -> R.string.unit_g
    QtyUnit.ML -> R.string.unit_ml
    QtyUnit.PIECE -> R.string.unit_piece
    QtyUnit.SLICE -> R.string.unit_slice
    QtyUnit.CLOVE -> R.string.unit_clove
    QtyUnit.STALK -> R.string.unit_stalk
    QtyUnit.TBSP -> R.string.unit_tbsp
    QtyUnit.TSP -> R.string.unit_tsp
    QtyUnit.BUNCH -> R.string.unit_bunch
    QtyUnit.PINCH -> R.string.unit_pinch
    QtyUnit.TO_TASTE -> R.string.unit_to_taste
}

@StringRes
fun allergenLabel(a: Allergen) = when (a) {
    Allergen.GLUTEN -> R.string.allergen_gluten
    Allergen.CRUSTACEANS -> R.string.allergen_crustaceans
    Allergen.EGGS -> R.string.allergen_eggs
    Allergen.FISH -> R.string.allergen_fish
    Allergen.PEANUTS -> R.string.allergen_peanuts
    Allergen.SOY -> R.string.allergen_soy
    Allergen.MILK -> R.string.allergen_milk
    Allergen.TREE_NUTS -> R.string.allergen_tree_nuts
    Allergen.CELERY -> R.string.allergen_celery
    Allergen.MUSTARD -> R.string.allergen_mustard
    Allergen.SESAME -> R.string.allergen_sesame
    Allergen.SULPHITES -> R.string.allergen_sulphites
    Allergen.LUPIN -> R.string.allergen_lupin
    Allergen.MOLLUSCS -> R.string.allergen_molluscs
}
