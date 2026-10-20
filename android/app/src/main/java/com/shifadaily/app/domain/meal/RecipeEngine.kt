package com.shifadaily.app.domain.meal

import kotlin.math.abs
import kotlin.math.roundToInt

/** User changes applied on top of a curated recipe. */
data class Customization(
    val servings: Int? = null,
    val removed: Set<String> = emptySet(),
    /** original key -> substitute key */
    val replaced: Map<String, String> = emptyMap(),
    val added: List<String> = emptyList(),
    /** Quantity overrides expressed for the recipe's BASE servings (so they scale too). */
    val qtyOverrides: Map<String, Double> = emptyMap(),
) {
    fun encode(): String = buildList {
        servings?.let { add("s=$it") }
        if (removed.isNotEmpty()) add("r=" + removed.sorted().joinToString(","))
        if (replaced.isNotEmpty()) add("x=" + replaced.entries.sortedBy { it.key }.joinToString(",") { "${it.key}>${it.value}" })
        if (added.isNotEmpty()) add("a=" + added.joinToString(","))
        if (qtyOverrides.isNotEmpty()) add("q=" + qtyOverrides.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" })
    }.joinToString("|")

    companion object {
        private val KEY = Regex("^[a-z_]{1,40}$")

        /** Tolerant decoder: unknown or malformed parts are ignored. */
        fun decode(raw: String?): Customization {
            if (raw.isNullOrBlank()) return Customization()
            var c = Customization()
            for (part in raw.split('|')) {
                val (k, v) = part.split('=', limit = 2).let { if (it.size == 2) it[0] to it[1] else return@let null } ?: continue
                c = when (k) {
                    "s" -> c.copy(servings = v.toIntOrNull()?.takeIf { it in 1..20 })
                    "r" -> c.copy(removed = v.split(',').filter { KEY.matches(it) }.toSet())
                    "x" -> c.copy(replaced = v.split(',').mapNotNull { e ->
                        val p = e.split('>'); if (p.size == 2 && KEY.matches(p[0]) && KEY.matches(p[1])) p[0] to p[1] else null
                    }.toMap())
                    "a" -> c.copy(added = v.split(',').filter { KEY.matches(it) }.distinct())
                    "q" -> c.copy(qtyOverrides = v.split(',').mapNotNull { e ->
                        val p = e.split(':'); val d = p.getOrNull(1)?.toDoubleOrNull()
                        if (p.size == 2 && KEY.matches(p[0]) && d != null && d > 0 && d < 100000) p[0] to d else null
                    }.toMap())
                    else -> c
                }
            }
            return c
        }
    }
}

data class ResolvedIngredient(
    /** Effective key (the substitute when replaced). */
    val key: String,
    val ingredient: Ingredient,
    val qty: Double?,
    val unit: QtyUnit,
    val optional: Boolean,
    val core: Boolean,
    val added: Boolean,
    val replacedFrom: String?,
    /** Original recipe key, used to address customization. */
    val originalKey: String,
)

data class ResolvedRecipe(
    val recipe: Recipe,
    val servings: Int,
    val ingredients: List<ResolvedIngredient>,
    val steps: List<String>,
)

data class PreferenceSet(
    val allergyKeys: Set<String> = emptySet(),
    val allergens: Set<Allergen> = emptySet(),
    val allergyCustom: List<String> = emptyList(),
    val avoidKeys: Set<String> = emptySet(),
    val avoidCustom: List<String> = emptyList(),
    val likeKeys: Set<String> = emptySet(),
    val dislikeKeys: Set<String> = emptySet(),
)

enum class ConflictReason { ALLERGY, AVOID, NOT_VEGETARIAN }

data class Suggestion(
    val recipe: Recipe,
    val have: List<String>,
    val need: List<String>,
    /** Optional ingredients removed automatically because of allergies / avoid list / diet. */
    val autoRemoved: Map<String, ConflictReason>,
    val score: Double,
) {
    fun defaultCustomization() = Customization(removed = autoRemoved.keys)
}

data class SuggestionResult(val suggestions: List<Suggestion>, val hiddenForConflicts: Int)

data class SuggestionFilters(
    val mealType: MealType? = null,
    /** 0 = no limit */
    val maxMinutes: Int = 0,
    val budget: Budget = Budget.ANY,
    val diet: DietPattern = DietPattern.DIABETES_CONSCIOUS,
)

data class DietarySummary(
    val freely: List<String>,
    val limited: List<String>,
    val occasional: List<String>,
    val notListed: List<String>,
    val allergens: Set<Allergen>,
    val method: CookingMethod,
)

object RecipeEngine {

    // ---------- conflicts ----------

    fun conflict(ing: Ingredient, prefs: PreferenceSet, diet: DietPattern): ConflictReason? {
        val names = listOf(ing.name.en, ing.name.ar, ing.def.en, ing.def.ar) + ing.synonyms
        fun matchesCustom(list: List<String>) = list.any { custom ->
            val c = PantryParser.normalize(custom)
            c.length >= 2 && names.any { n -> PantryParser.normalize(n).let { it == c || it.contains(c) || c.contains(it) } }
        }
        if (ing.key in prefs.allergyKeys || ing.allergens.any { it in prefs.allergens } || matchesCustom(prefs.allergyCustom)) {
            return ConflictReason.ALLERGY
        }
        if (ing.key in prefs.avoidKeys || matchesCustom(prefs.avoidCustom)) return ConflictReason.AVOID
        if (diet == DietPattern.VEGETARIAN && !ing.vegetarian) return ConflictReason.NOT_VEGETARIAN
        return null
    }

    /** Ingredients that can replace [original] in [recipe] for this user. */
    fun substitutesFor(recipe: Recipe, originalKey: String, prefs: PreferenceSet, diet: DietPattern): List<Ingredient> {
        val ri = recipe.ingredients.firstOrNull { it.key == originalKey } ?: return emptyList()
        return ri.substitutes.mapNotNull { IngredientCatalog[it] }.filter { conflict(it, prefs, diet) == null }
    }

    /** Catalog ingredients that may be added to [recipe]; excludes conflicts, sweets and items already present. */
    fun addable(recipe: Recipe, customization: Customization, prefs: PreferenceSet, diet: DietPattern): List<Ingredient> {
        val present = recipe.ingredients.map { customization.replaced[it.key] ?: it.key }.toSet() + customization.added
        return IngredientCatalog.all.filter {
            it.key !in present && it.tier != Tier.OCCASIONAL && it.category != IngredientCategory.DRINK &&
                conflict(it, prefs, diet) == null
        }
    }

    // ---------- resolution ----------

    fun resolve(recipe: Recipe, c: Customization, lang: Lang): ResolvedRecipe {
        val servings = (c.servings ?: recipe.servings).coerceIn(1, 20)
        val factor = servings.toDouble() / recipe.servings

        val ingredients = ArrayList<ResolvedIngredient>()
        for (ri in recipe.ingredients) {
            if (ri.key in c.removed && !ri.core) continue
            val effective = c.replaced[ri.key]?.takeIf { it in ri.substitutes } ?: ri.key
            val ing = IngredientCatalog[effective] ?: continue
            val base = c.qtyOverrides[ri.key] ?: ri.qty
            val qty = base?.let { if (ri.scalable) it * factor else it }
            ingredients += ResolvedIngredient(
                key = effective, ingredient = ing, qty = qty, unit = ri.unit, optional = ri.optional, core = ri.core,
                added = false, replacedFrom = if (effective != ri.key) ri.key else null, originalKey = ri.key,
            )
        }
        for (key in c.added) {
            val ing = IngredientCatalog[key] ?: continue
            if (ingredients.any { it.key == key }) continue
            val (perServing, unit) = IngredientCatalog.defaultAddQty(ing)
            val base = c.qtyOverrides[key] ?: perServing?.times(recipe.servings)
            ingredients += ResolvedIngredient(key, ing, base?.times(factor), unit, optional = true, core = false,
                added = true, replacedFrom = null, originalKey = key)
        }
        return ResolvedRecipe(recipe, servings, ingredients, renderSteps(recipe, c, lang))
    }

    /** Maps an original key to the key actually used, or null if removed. */
    private fun effectiveKey(recipe: Recipe, c: Customization, key: String): String? {
        val ri = recipe.ingredients.firstOrNull { it.key == key } ?: return null
        if (key in c.removed && !ri.core) return null
        return c.replaced[key]?.takeIf { it in ri.substitutes } ?: key
    }

    fun renderSteps(recipe: Recipe, c: Customization, lang: Lang): List<String> {
        val out = ArrayList<String>()
        for (step in recipe.steps) {
            if (step.requires.isNotEmpty() && step.requires.all { effectiveKey(recipe, c, it) == null }) continue
            val sb = StringBuilder()
            for (part in step.parts) {
                when (part) {
                    is StepPart.Text -> sb.append(part.text.get(lang))
                    is StepPart.Clause -> {
                        val names = part.keys.mapNotNull { effectiveKey(recipe, c, it) }
                            .distinct()
                            .mapNotNull { IngredientCatalog[it]?.def?.get(lang) }
                        if (names.isNotEmpty()) sb.append(part.text.get(lang).replace("{x}", joinNames(names, lang)))
                    }
                }
            }
            val text = tidy(sb.toString())
            if (text.isNotBlank()) out += text
        }
        // Steps for ingredients the user added, inserted before the final step.
        val added = c.added.mapNotNull { IngredientCatalog[it] }
        if (added.isNotEmpty()) {
            val extra = addedSteps(added, recipe.method, lang)
            val at = (out.size - 1).coerceAtLeast(0)
            out.addAll(at, extra)
        }
        return out
    }

    private fun addedSteps(added: List<Ingredient>, method: CookingMethod, lang: Lang): List<String> {
        val groups = added.groupBy {
            when (it.category) {
                IngredientCategory.PROTEIN -> 0
                IngredientCategory.VEGETABLE, IngredientCategory.LEGUME -> 1
                IngredientCategory.HERB_SPICE -> 2
                else -> 3
            }
        }
        val raw = method == CookingMethod.NO_COOK
        return groups.toSortedMap().map { (g, items) ->
            val names = joinNames(items.map { it.def.get(lang) }, lang)
            val t = when (g) {
                0 -> LText("Cook $names separately until fully cooked, then add it to the dish.",
                    "اطهُ $names على حدة حتى ينضج تمامًا، ثم أضفه إلى الطبق.")
                1 -> if (raw) LText("Wash, chop and mix in $names.", "اغسل $names وقطّعه وأضفه إلى الخليط.")
                else LText("Add $names and cook until tender.", "أضف $names واطهُ حتى ينضج.")
                2 -> LText("Season with $names.", "تبّل ب$names.")
                else -> LText("Serve with $names.", "قدّم الطبق مع $names.")
            }
            t.get(lang)
        }
    }

    fun joinNames(names: List<String>, lang: Lang): String = when (names.size) {
        0 -> ""
        1 -> names[0]
        else -> if (lang == Lang.AR) {
            names.dropLast(1).joinToString("، ") + " و" + names.last()
        } else {
            names.dropLast(1).joinToString(", ") + " and " + names.last()
        }
    }

    private fun tidy(s: String): String {
        var r = s.replace(Regex(" {2,}"), " ").replace(" .", ".").replace(" ,", ",").replace(" ،", "،").trim()
        r = r.replace(Regex("^[,،.]\\s*"), "")
        // Capitalise the first Latin letter (clauses may start a sentence).
        if (r.isNotEmpty() && r[0].isLowerCase()) r = r[0].uppercaseChar() + r.substring(1)
        return r
    }

    // ---------- quantities ----------

    /** Kitchen-friendly rounding for display. */
    fun displayQty(qty: Double?, unit: QtyUnit): String? {
        if (qty == null || unit == QtyUnit.TO_TASTE) return null
        val v = when (unit) {
            QtyUnit.G, QtyUnit.ML -> if (qty >= 50) (qty / 5).roundToInt() * 5.0 else qty.roundToInt().toDouble().coerceAtLeast(1.0)
            QtyUnit.PIECE, QtyUnit.SLICE, QtyUnit.STALK, QtyUnit.BUNCH, QtyUnit.TBSP, QtyUnit.TSP, QtyUnit.CLOVE ->
                ((qty * 4).roundToInt() / 4.0).coerceAtLeast(0.25)
            QtyUnit.PINCH -> qty.roundToInt().coerceAtLeast(1).toDouble()
            QtyUnit.TO_TASTE -> return null
        }
        return formatNumber(v)
    }

    fun formatNumber(v: Double): String {
        val whole = v.toInt()
        val frac = v - whole
        val f = when {
            abs(frac) < 0.01 -> ""
            abs(frac - 0.25) < 0.01 -> "¼"
            abs(frac - 0.5) < 0.01 -> "½"
            abs(frac - 0.75) < 0.01 -> "¾"
            else -> return String.format(java.util.Locale.ROOT, "%.1f", v)
        }
        return when {
            f.isEmpty() -> whole.toString()
            whole == 0 -> f
            else -> "$whole$f"
        }
    }

    // ---------- suggestions ----------

    fun suggest(
        recipes: List<Recipe>,
        pantryKeys: Set<String>,
        prefs: PreferenceSet,
        filters: SuggestionFilters,
    ): SuggestionResult {
        var hidden = 0
        val out = ArrayList<Suggestion>()
        for (r in recipes) {
            if (filters.mealType != null && filters.mealType !in r.mealTypes) continue
            if (filters.maxMinutes > 0 && r.minutes > filters.maxMinutes) continue
            if (!budgetAllows(filters.budget, r.budget)) continue

            val autoRemoved = LinkedHashMap<String, ConflictReason>()
            var blocked = false
            for (ri in r.ingredients) {
                val ing = IngredientCatalog[ri.key] ?: continue
                val reason = conflict(ing, prefs, filters.diet) ?: continue
                // Optional items are dropped visibly; a required conflicting item hides the recipe.
                // We never swap a required ingredient silently.
                if (ri.optional) autoRemoved[ri.key] = reason else blocked = true
            }
            if (blocked) { hidden++; continue }

            val used = r.ingredients.filter { it.key !in autoRemoved }
            val required = used.filter { !it.optional }
            val ranked = required.filter { IngredientCatalog[it.key]?.staple != true }
            val have = used.filter { it.key in pantryKeys }.map { it.key }
            val need = required.filter { it.key !in pantryKeys }.map { it.key }
            val haveRanked = ranked.count { it.key in pantryKeys }
            var score = if (ranked.isEmpty()) 0.0 else haveRanked.toDouble() / ranked.size
            score += 0.15 * used.count { it.key in prefs.likeKeys }
            score -= 0.25 * used.count { it.key in prefs.dislikeKeys }
            out += Suggestion(r, have, need, autoRemoved, score)
        }
        out.sortWith(compareByDescending<Suggestion> { it.score }.thenBy { it.recipe.minutes }.thenBy { it.recipe.id })
        return SuggestionResult(out, hidden)
    }

    private fun budgetAllows(filter: Budget, recipe: Budget) = when (filter) {
        Budget.ANY -> true
        Budget.MEDIUM -> recipe != Budget.ANY
        Budget.LOW -> recipe == Budget.LOW
    }

    // ---------- transparency ----------

    fun dietarySummary(resolved: ResolvedRecipe, lang: Lang): DietarySummary {
        fun names(t: Tier) = resolved.ingredients.filter { it.ingredient.tier == t }.map { it.ingredient.name.get(lang) }
        return DietarySummary(
            freely = names(Tier.FREELY),
            limited = names(Tier.LIMITED),
            occasional = names(Tier.OCCASIONAL),
            notListed = names(Tier.NOT_LISTED),
            allergens = resolved.ingredients.flatMap { it.ingredient.allergens }.toSet(),
            method = resolved.recipe.method,
        )
    }
}
