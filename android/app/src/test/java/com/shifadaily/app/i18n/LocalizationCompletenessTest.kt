package com.shifadaily.app.i18n

import com.shifadaily.app.domain.meal.IngredientCatalog
import com.shifadaily.app.domain.meal.RecipeCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Every English string has an Arabic translation with the same placeholders, and vice versa. */
class LocalizationCompletenessTest {
    private val res = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
    private val placeholder = Regex("%\\d+\\$[sd]")

    private fun load(dir: String): Map<String, List<String>> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "$dir/strings.xml"))
        val out = LinkedHashMap<String, List<String>>()
        val strings = doc.getElementsByTagName("string")
        for (i in 0 until strings.length) {
            val e = strings.item(i) as Element
            if (e.getAttribute("translatable") != "false") out[e.getAttribute("name")] = listOf(e.textContent)
        }
        val plurals = doc.getElementsByTagName("plurals")
        for (i in 0 until plurals.length) {
            val e = plurals.item(i) as Element
            val items = e.getElementsByTagName("item")
            out["plurals:" + e.getAttribute("name")] = (0 until items.length).map { items.item(it).textContent }
        }
        return out
    }

    @Test fun arabicAndEnglishHaveTheSameKeys() {
        val en = load("values")
        val ar = load("values-ar")
        assertEquals("Missing in Arabic", emptySet<String>(), en.keys - ar.keys)
        assertEquals("Missing in English", emptySet<String>(), ar.keys - en.keys)
    }

    @Test fun placeholdersMatch() {
        val en = load("values")
        val ar = load("values-ar")
        for ((k, v) in en) {
            if (k.startsWith("plurals:")) {
                // Arabic "one"/"two"/"zero" forms may spell the number out; "other" must carry it.
                val enPh = v.flatMap { placeholder.findAll(it).map { m -> m.value }.toList() }.toSet()
                val arOther = ar.getValue(k).last()
                assertTrue("$k: Arabic 'other' form lacks placeholder", enPh.all { it in arOther })
                continue
            }
            val a = placeholder.findAll(v.single()).map { it.value }.toSet()
            val b = placeholder.findAll(ar.getValue(k).single()).map { it.value }.toSet()
            assertEquals("Placeholder mismatch for $k", a, b)
        }
    }

    @Test fun noEmptyTranslations() {
        for (dir in listOf("values", "values-ar")) load(dir).forEach { (k, v) -> v.forEach { assertTrue("$dir/$k is empty", it.isNotBlank()) } }
    }

    @Test fun arabicStringsAreActuallyArabic() {
        val arabic = Regex("[\\u0600-\\u06FF]")
        val exempt = setOf("app_name_full", "minutes_short", "volume_value")
        load("values-ar").forEach { (k, v) ->
            if (k !in exempt) assertTrue("values-ar/$k has no Arabic text", v.any { arabic.containsMatchIn(it) })
        }
    }

    @Test fun curatedDataIsBilingual() {
        val arabic = Regex("[\\u0600-\\u06FF]")
        IngredientCatalog.all.forEach {
            assertTrue(it.key, it.name.en.isNotBlank() && arabic.containsMatchIn(it.name.ar) && arabic.containsMatchIn(it.def.ar))
        }
        RecipeCatalog.all.forEach { r ->
            assertTrue(r.id, arabic.containsMatchIn(r.title.ar) && arabic.containsMatchIn(r.description.ar))
        }
    }
}
