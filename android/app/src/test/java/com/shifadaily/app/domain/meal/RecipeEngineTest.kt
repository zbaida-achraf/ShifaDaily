package com.shifadaily.app.domain.meal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeEngineTest {

    @Test fun catalogIsConsistent() {
        assertEquals("duplicate ingredient keys", IngredientCatalog.all.size, IngredientCatalog.all.map { it.key }.toSet().size)
        assertEquals("duplicate recipe ids", RecipeCatalog.all.size, RecipeCatalog.all.map { it.id }.toSet().size)
        for (r in RecipeCatalog.all) {
            val keys = r.ingredients.map { it.key }
            assertEquals("duplicate ingredient in ${r.id}", keys.size, keys.toSet().size)
            for (ri in r.ingredients) {
                assertNotNull("${r.id}: unknown ${ri.key}", IngredientCatalog[ri.key])
                ri.substitutes.forEach { assertNotNull("${r.id}: unknown substitute $it", IngredientCatalog[it]) }
                assertFalse("${r.id}: ${ri.key} cannot be both optional and core", ri.optional && ri.core)
            }
            for (step in r.steps) {
                step.requires.forEach { assertTrue("${r.id}: step requires unknown $it", it in keys) }
                step.parts.filterIsInstance<StepPart.Clause>().forEach { c ->
                    c.keys.forEach { assertTrue("${r.id}: clause uses unknown $it", it in keys) }
                    assertTrue("${r.id}: clause without {x}", "{x}" in c.text.en && "{x}" in c.text.ar)
                }
            }
            // No sweets ("occasionally" tier) in the curated diabetes-conscious recipes.
            assertTrue("${r.id} uses an occasional-tier ingredient",
                r.ingredients.none { IngredientCatalog.require(it.key).tier == Tier.OCCASIONAL })
        }
    }

    @Test fun everyIngredientIsMentionedInTheSteps() {
        for (r in RecipeCatalog.all) for (lang in Lang.entries) {
            val text = RecipeEngine.resolve(r, Customization(), lang).steps.joinToString(" ")
            for (ri in r.ingredients) {
                val name = IngredientCatalog.require(ri.key).def.get(lang)
                assertTrue("${r.id}/$lang: '$name' never appears in steps", name in text)
            }
        }
    }

    @Test fun removedIngredientsNeverRemainInInstructions() {
        for (r in RecipeCatalog.all) for (ri in r.ingredients.filter { !it.core }) for (lang in Lang.entries) {
            val resolved = RecipeEngine.resolve(r, Customization(removed = setOf(ri.key)), lang)
            val name = IngredientCatalog.require(ri.key).def.get(lang)
            assertFalse("${r.id}/$lang: '${ri.key}' removed but listed", resolved.ingredients.any { it.key == ri.key })
            resolved.steps.forEach { assertFalse("${r.id}/$lang: '$name' still in step after removal: $it", containsName(it, name, r, ri.key, lang)) }
        }
    }

    @Test fun replacedIngredientsAreRenamedInInstructions() {
        for (r in RecipeCatalog.all) for (ri in r.ingredients) for (sub in ri.substitutes) for (lang in Lang.entries) {
            val resolved = RecipeEngine.resolve(r, Customization(replaced = mapOf(ri.key to sub)), lang)
            val oldName = IngredientCatalog.require(ri.key).def.get(lang)
            val newName = IngredientCatalog.require(sub).def.get(lang)
            val text = resolved.steps.joinToString(" ")
            assertTrue("${r.id}/$lang: substitute '$newName' missing", newName in text)
            resolved.steps.forEach { assertFalse("${r.id}/$lang: '$oldName' remains after replacement: $it", containsName(it, oldName, r, ri.key, lang)) }
        }
    }

    /**
     * True if [name] appears in [step] other than as part of another ingredient's name that is
     * still in the recipe (e.g. "pepper" inside "black pepper").
     */
    private fun containsName(step: String, name: String, r: Recipe, removedKey: String, lang: Lang): Boolean {
        var s = step
        r.ingredients.filter { it.key != removedKey }.forEach { other ->
            val n = IngredientCatalog.require(other.key).def.get(lang)
            if (n != name && name in n) s = s.replace(n, "")
        }
        IngredientCatalog.all.forEach { ing -> val n = ing.def.get(lang); if (n != name && name in n) s = s.replace(n, "") }
        return name in s
    }

    @Test fun coreIngredientsCannotBeRemoved() {
        val r = RecipeCatalog["courgette_egg_skillet"]!!
        val resolved = RecipeEngine.resolve(r, Customization(removed = setOf("eggs")), Lang.EN)
        assertTrue(resolved.ingredients.any { it.key == "eggs" })
    }

    @Test fun quantitiesScaleWithServings() {
        val r = RecipeCatalog["zaalouk"]!! // 4 servings, 2 aubergines, salt to taste
        val resolved = RecipeEngine.resolve(r, Customization(servings = 2), Lang.EN)
        assertEquals(1.0, resolved.ingredients.first { it.key == "eggplant" }.qty!!, 1e-9)
        assertEquals(null, resolved.ingredients.first { it.key == "salt" }.qty)
        val eight = RecipeEngine.resolve(r, Customization(servings = 8), Lang.EN)
        assertEquals(6.0, eight.ingredients.first { it.key == "tomato" }.qty!!, 1e-9)
    }

    @Test fun quantityOverrideKeepsScaling() {
        val r = RecipeCatalog["zaalouk"]!!
        // Override stored for base servings (4): 3 aubergines. At 2 servings -> 1.5.
        val resolved = RecipeEngine.resolve(r, Customization(servings = 2, qtyOverrides = mapOf("eggplant" to 3.0)), Lang.EN)
        assertEquals(1.5, resolved.ingredients.first { it.key == "eggplant" }.qty!!, 1e-9)
    }

    @Test fun substitutesOutsideCuratedListAreIgnored() {
        val r = RecipeCatalog["zaalouk"]!!
        val resolved = RecipeEngine.resolve(r, Customization(replaced = mapOf("eggplant" to "sugar")), Lang.EN)
        assertTrue(resolved.ingredients.any { it.key == "eggplant" })
        assertFalse(resolved.ingredients.any { it.key == "sugar" })
    }

    @Test fun addedIngredientGetsAStepAndAQuantity() {
        val r = RecipeCatalog["courgette_egg_skillet"]!!
        val resolved = RecipeEngine.resolve(r, Customization(added = listOf("spinach")), Lang.EN)
        val sp = resolved.ingredients.first { it.key == "spinach" }
        assertTrue(sp.added)
        assertEquals(160.0, sp.qty!!, 1e-9) // 80 g per serving x 2
        assertTrue(resolved.steps.any { "spinach" in it })
        val ar = RecipeEngine.resolve(r, Customization(added = listOf("spinach")), Lang.AR)
        assertTrue(ar.steps.any { "السبانخ" in it })
    }

    @Test fun displayQuantitiesAreKitchenFriendly() {
        assertEquals("½", RecipeEngine.displayQty(0.5, QtyUnit.PIECE))
        assertEquals("1½", RecipeEngine.displayQty(1.5, QtyUnit.TBSP))
        assertEquals("165", RecipeEngine.displayQty(163.0, QtyUnit.G))
        assertEquals(null, RecipeEngine.displayQty(null, QtyUnit.TO_TASTE))
    }

    @Test fun customizationEncodingRoundTripsAndRejectsGarbage() {
        val c = Customization(3, setOf("onion"), mapOf("fish" to "sardines"), listOf("spinach"), mapOf("olive_oil" to 0.5))
        assertEquals(c, Customization.decode(c.encode()))
        val bad = Customization.decode("s=999|r=DROP TABLE,onion|x=a>b>c|q=x:-1|zzz")
        assertEquals(null, bad.servings)
        assertEquals(setOf("onion"), bad.removed)
        assertTrue(bad.replaced.isEmpty())
        assertTrue(bad.qtyOverrides.isEmpty())
    }

    @Test fun allergiesHideRecipesAndRemoveOptionalItems() {
        val prefs = PreferenceSet(allergens = setOf(Allergen.EGGS))
        val res = RecipeEngine.suggest(RecipeCatalog.all, emptySet(), prefs, SuggestionFilters())
        assertTrue(res.suggestions.none { s -> s.recipe.ingredients.any { !it.optional && "eggs" == it.key } })
        assertTrue(res.hiddenForConflicts >= 2)

        val milkAllergy = PreferenceSet(allergens = setOf(Allergen.MILK))
        val crudites = RecipeEngine.suggest(RecipeCatalog.all, emptySet(), milkAllergy, SuggestionFilters())
            .suggestions.first { it.recipe.id == "crudites" }
        assertEquals(ConflictReason.ALLERGY, crudites.autoRemoved["yogurt"])
    }

    @Test fun customAllergyMatchesByName() {
        val prefs = PreferenceSet(allergyCustom = listOf("sésame"))
        val res = RecipeEngine.suggest(RecipeCatalog.all, emptySet(), prefs, SuggestionFilters())
        val hummus = res.suggestions.first { it.recipe.id == "hummus_veg" }
        assertEquals(ConflictReason.ALLERGY, hummus.autoRemoved["tahini"])
    }

    @Test fun vegetarianHidesMeatAndFishMains() {
        val res = RecipeEngine.suggest(RecipeCatalog.all, emptySet(), PreferenceSet(), SuggestionFilters(diet = DietPattern.VEGETARIAN))
        assertTrue(res.suggestions.none { it.recipe.id in setOf("fish_tagine", "chicken_vegetable_tagine", "baked_fish_vegetables") })
    }

    @Test fun pantryMatchRanksRecipesUsingWhatYouHave() {
        val pantry = PantryParser.parse("I have courgettes, eggs, chickpeas, tomatoes, and olive oil.")
        assertEquals(setOf("courgette", "eggs", "chickpeas", "tomato", "olive_oil"), pantry.keys.toSet())
        val res = RecipeEngine.suggest(RecipeCatalog.all, pantry.keys.toSet(), PreferenceSet(), SuggestionFilters())
        assertEquals("courgette_egg_skillet", res.suggestions.first().recipe.id)
        val top = res.suggestions.first()
        assertTrue(top.need.isEmpty() || top.need.all { IngredientCatalog.require(it).staple || it == "onion" })
    }

    @Test fun pantryParserHandlesArabicAndFrench() {
        assertEquals(setOf("tomato", "eggs", "olive_oil"), PantryParser.parse("عندي طماطم، بيض وزيت الزيتون").keys.toSet())
        assertEquals(setOf("courgette", "chickpeas", "onion"), PantryParser.parse("j'ai des courgettes, pois chiches et oignons").keys.toSet())
        val p = PantryParser.parse("tomatoes, unicorn dust")
        assertEquals(listOf("unicorn dust"), p.unmatched)
    }

    @Test fun filtersApply() {
        val res = RecipeEngine.suggest(RecipeCatalog.all, emptySet(), PreferenceSet(),
            SuggestionFilters(mealType = MealType.BREAKFAST, maxMinutes = 15, budget = Budget.LOW))
        assertTrue(res.suggestions.isNotEmpty())
        assertTrue(res.suggestions.all { MealType.BREAKFAST in it.recipe.mealTypes && it.recipe.minutes <= 15 && it.recipe.budget == Budget.LOW })
    }
}
