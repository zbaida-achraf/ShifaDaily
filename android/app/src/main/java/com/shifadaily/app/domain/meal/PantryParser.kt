package com.shifadaily.app.domain.meal

import java.text.Normalizer

data class ParsedPantry(val keys: List<String>, val unmatched: List<String>)

/**
 * Turns free text such as "I have courgettes, eggs, chickpeas, tomatoes, and olive oil"
 * (English, French or Arabic) into catalog keys. Purely local, no network.
 */
object PantryParser {
    private val SEPARATORS = Regex("\\s*(?:[,،;\\n]+|\\band\\b|\\bet\\b|\\s+و(?=\\S)|\\s+و\\s+)\\s*", RegexOption.IGNORE_CASE)
    private val LEADING = listOf(
        "i have", "i've got", "we have", "j'ai", "nous avons", "il y a", "j ai",
        "عندي", "لدي", "لديّ", "عندنا", "كاين",
    )

    fun normalize(s: String): String {
        val noAccents = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        return noAccents
            .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ة', 'ه').replace('ى', 'ي')
            .replace(Regex("[^\\p{L}\\p{N} ']"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private val index: List<Pair<String, String>> by lazy {
        IngredientCatalog.all.flatMap { ing ->
            (listOf(ing.key.replace('_', ' '), ing.name.en, ing.name.ar, ing.def.en, ing.def.ar) + ing.synonyms)
                .map { normalize(it) to ing.key }
        }.filter { it.first.isNotEmpty() }.sortedByDescending { it.first.length }
    }

    fun parse(text: String): ParsedPantry {
        var t = text.trim()
        val lower = normalize(t)
        for (lead in LEADING) {
            val nl = normalize(lead)
            if (lower.startsWith("$nl ") || lower.startsWith("$nl:")) {
                t = t.substring(lead.length.coerceAtMost(t.length)).trimStart(' ', ':')
                break
            }
        }
        val keys = LinkedHashSet<String>()
        val unmatched = ArrayList<String>()
        for (raw in t.split(SEPARATORS)) {
            val item = raw.trim().trimEnd('.', '!', '?').trim()
            if (item.isEmpty()) continue
            val key = match(item)
            if (key != null) keys += key else unmatched += item
        }
        return ParsedPantry(keys.toList(), unmatched.distinct())
    }

    fun match(item: String): String? {
        val n = normalize(item)
        if (n.isEmpty()) return null
        index.firstOrNull { it.first == n }?.let { return it.second }
        // Simple plural / article tolerance: "the tomatoes", "les tomates", "الطماطم".
        val stripped = n.removePrefix("the ").removePrefix("les ").removePrefix("des ").removePrefix("du ").removePrefix("de ")
            .removePrefix("some ").removePrefix("ال")
        index.firstOrNull { it.first == stripped || it.first == stripped.removeSuffix("s") || it.first.removePrefix("ال") == stripped }
            ?.let { return it.second }
        // Longest catalog phrase contained in the item ("2 red bell peppers").
        return index.firstOrNull { it.first.length >= 3 && Regex("(^|\\s)${Regex.escape(it.first)}(\\s|$)").containsMatchIn(n) }?.second
    }
}
