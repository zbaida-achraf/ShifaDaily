package com.shifadaily.app.ui.meals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shifadaily.app.AppContainer
import com.shifadaily.app.R
import com.shifadaily.app.data.prefs.AppSettings
import com.shifadaily.app.domain.meal.Customization
import com.shifadaily.app.domain.meal.Ingredient
import com.shifadaily.app.domain.meal.PantryParser
import com.shifadaily.app.domain.meal.QtyUnit
import com.shifadaily.app.domain.meal.RecipeCatalog
import com.shifadaily.app.domain.meal.RecipeEngine
import com.shifadaily.app.domain.meal.ResolvedIngredient
import com.shifadaily.app.ui.components.NoticeCard
import com.shifadaily.app.ui.components.SectionTitle
import com.shifadaily.app.ui.components.ShifaCard
import com.shifadaily.app.ui.components.ShifaTopBar
import com.shifadaily.app.ui.components.TierBadge
import kotlinx.coroutines.launch

@Composable
fun RecipeScreen(container: AppContainer, settings: AppSettings, recipeId: String, savedId: Long?, onBack: () -> Unit) {
    val recipe = RecipeCatalog[recipeId]
    if (recipe == null) {
        Scaffold(topBar = { ShifaTopBar("", onBack = onBack) }) { p -> Text(stringResource(R.string.recipe_not_found), Modifier.padding(p).padding(16.dp)) }
        return
    }
    val lang = currentLang()
    val res = androidx.compose.ui.platform.LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val pantry by container.meals.observePantry().collectAsState(initial = emptyList())
    val prefs by container.meals.observePreferences().collectAsState(initial = null)
    val pantryKeys = pantry.mapNotNull { it.ingredientKey }.toSet()

    var encoded by rememberSaveable(recipeId, savedId) { mutableStateOf<String?>(null) }
    var currentSavedId by rememberSaveable(recipeId) { mutableStateOf(savedId) }
    LaunchedEffect(recipeId, savedId, prefs) {
        if (encoded == null && prefs != null) {
            encoded = if (savedId != null) {
                container.meals.getSaved(savedId)?.customization ?: ""
            } else {
                // Optional ingredients that conflict with allergies/avoid list start removed (shown to the user).
                val auto = recipe.ingredients.filter { ri ->
                    ri.optional && RecipeEngine.conflict(com.shifadaily.app.domain.meal.IngredientCatalog.require(ri.key), prefs!!, settings.dietPattern) != null
                }.map { it.key }.toSet()
                Customization(servings = settings.defaultServings.takeIf { it != recipe.servings }, removed = auto).encode()
            }
        }
    }
    val enc = encoded ?: return
    val cust = Customization.decode(enc)
    val resolved = RecipeEngine.resolve(recipe, cust, lang)
    fun update(c: Customization) { encoded = c.encode() }

    var menuFor by remember { mutableStateOf<String?>(null) }
    var replaceFor by remember { mutableStateOf<ResolvedIngredient?>(null) }
    var qtyFor by remember { mutableStateOf<ResolvedIngredient?>(null) }
    var adding by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { ShifaTopBar(recipe.title.get(lang), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(recipe.description.get(lang), style = MaterialTheme.typography.bodyLarge)
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.minutes_short, recipe.minutes) + " · " + stringResource(methodLabel(recipe.method)),
                        style = MaterialTheme.typography.labelLarge)
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.servings), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { update(cust.copy(servings = (resolved.servings - 1).coerceAtLeast(1))) }) {
                        Icon(Icons.Filled.Remove, stringResource(R.string.cd_decrease))
                    }
                    Text(resolved.servings.toString(), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { update(cust.copy(servings = (resolved.servings + 1).coerceAtMost(20))) }) {
                        Icon(Icons.Filled.Add, stringResource(R.string.cd_increase))
                    }
                }
            }
            item { SectionTitle(stringResource(R.string.ingredients)) }
            items(resolved.ingredients, key = { it.originalKey }) { ri ->
                val have = ri.key in pantryKeys
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (have) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = stringResource(if (have) R.string.cd_have else R.string.cd_need),
                        tint = if (have) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        val qty = RecipeEngine.displayQty(ri.qty, ri.unit)
                        val unit = stringResource(unitLabel(ri.unit))
                        val line = if (qty != null) "$qty $unit · ${ri.ingredient.name.get(lang)}" else "${ri.ingredient.name.get(lang)} · $unit"
                        Text(line, style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            TierBadge(ri.ingredient.tier)
                            if (ri.optional) Text(stringResource(R.string.optional), style = MaterialTheme.typography.labelSmall)
                            ri.replacedFrom?.let { from ->
                                Text(stringResource(R.string.replaces, com.shifadaily.app.domain.meal.IngredientCatalog.require(from).name.get(lang)),
                                    style = MaterialTheme.typography.labelSmall)
                            }
                            if (ri.added) Text(stringResource(R.string.added_by_you), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Column {
                        IconButton(onClick = { menuFor = ri.originalKey }) {
                            Icon(Icons.Filled.MoreVert, stringResource(R.string.cd_ingredient_options, ri.ingredient.name.get(lang)))
                        }
                        DropdownMenu(expanded = menuFor == ri.originalKey, onDismissRequest = { menuFor = null }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(if (have) R.string.mark_not_available else R.string.mark_available)) },
                                onClick = {
                                    menuFor = null
                                    scope.launch { if (have) container.meals.removePantryKey(ri.key) else container.meals.addPantryKeys(listOf(ri.key)) }
                                },
                            )
                            val p = prefs
                            if (!ri.added && p != null && RecipeEngine.substitutesFor(recipe, ri.originalKey, p, settings.dietPattern).isNotEmpty()) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.replace)) }, onClick = { menuFor = null; replaceFor = ri })
                            }
                            if (ri.qty != null && ri.unit != QtyUnit.TO_TASTE) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.adjust_quantity)) }, onClick = { menuFor = null; qtyFor = ri })
                            }
                            if (ri.replacedFrom != null) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.undo_replace)) }, onClick = {
                                    menuFor = null; update(cust.copy(replaced = cust.replaced - ri.originalKey))
                                })
                            }
                            if (!ri.core) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.remove)) }, onClick = {
                                    menuFor = null
                                    update(
                                        if (ri.added) cust.copy(added = cust.added - ri.key, qtyOverrides = cust.qtyOverrides - ri.key)
                                        else cust.copy(removed = cust.removed + ri.originalKey, replaced = cust.replaced - ri.originalKey)
                                    )
                                })
                            }
                        }
                    }
                }
            }
            val removedOriginals = recipe.ingredients.filter { it.key in cust.removed && !it.core }
            if (removedOriginals.isNotEmpty()) {
                item {
                    Column {
                        Text(stringResource(R.string.removed_ingredients), style = MaterialTheme.typography.labelLarge)
                        removedOriginals.forEach { r ->
                            val name = com.shifadaily.app.domain.meal.IngredientCatalog.require(r.key).name.get(lang)
                            TextButton(onClick = { update(cust.copy(removed = cust.removed - r.key)) }) {
                                Text(stringResource(R.string.restore_item, name))
                            }
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = { adding = true }) {
                    Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.add_ingredient))
                }
            }
            item { SectionTitle(stringResource(R.string.steps)) }
            itemsIndexed(resolved.steps) { i, step ->
                Row {
                    Text("${i + 1}.", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(28.dp))
                    Text(step, style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (cust.replaced.isNotEmpty() || cust.added.isNotEmpty()) {
                item { NoticeCard(stringResource(R.string.customised_note)) }
            }
            item {
                val d = RecipeEngine.dietarySummary(resolved, lang)
                ShifaCard {
                    Text(stringResource(R.string.dietary_considerations), style = MaterialTheme.typography.titleSmall)
                    if (d.freely.isNotEmpty()) Text(stringResource(R.string.diet_freely_list, d.freely.joinToString(", ")), style = MaterialTheme.typography.bodySmall)
                    if (d.limited.isNotEmpty()) Text(stringResource(R.string.diet_limited_list, d.limited.joinToString(", ")), style = MaterialTheme.typography.bodySmall)
                    if (d.occasional.isNotEmpty()) Text(stringResource(R.string.diet_occasional_list, d.occasional.joinToString(", ")), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.diet_method, stringResource(methodLabel(d.method))), style = MaterialTheme.typography.bodySmall)
                    if (d.allergens.isNotEmpty()) {
                        Text(
                            stringResource(R.string.contains_allergens, d.allergens.joinToString(", ") { res.getString(allergenLabel(it)) }),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(stringResource(R.string.diet_portion_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { NoticeCard(stringResource(R.string.allergy_disclaimer), warning = true) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { update(Customization()) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.reset)) }
                    Button(onClick = {
                        scope.launch {
                            currentSavedId = container.meals.save(recipe.id, cust, currentSavedId)
                            snackbar.showSnackbar(res.getString(R.string.recipe_saved))
                        }
                    }, modifier = Modifier.weight(1f)) { Text(stringResource(if (currentSavedId != null) R.string.update_saved else R.string.save_recipe)) }
                }
            }
        }
    }

    replaceFor?.let { ri ->
        val options = prefs?.let { RecipeEngine.substitutesFor(recipe, ri.originalKey, it, settings.dietPattern) }.orEmpty()
        PickIngredientDialog(stringResource(R.string.replace_title, ri.ingredient.name.get(lang)), options, lang, searchable = false,
            onPick = { ing -> update(cust.copy(replaced = cust.replaced + (ri.originalKey to ing.key))); replaceFor = null },
            onDismiss = { replaceFor = null })
    }
    if (adding) {
        val options = prefs?.let { RecipeEngine.addable(recipe, cust, it, settings.dietPattern) }.orEmpty()
        PickIngredientDialog(stringResource(R.string.add_ingredient), options, lang, searchable = true,
            onPick = { ing -> update(cust.copy(added = cust.added + ing.key)); adding = false },
            onDismiss = { adding = false })
    }
    qtyFor?.let { ri ->
        var text by remember(ri.originalKey) { mutableStateOf(RecipeEngine.displayQty(ri.qty, ri.unit)?.let { toPlain(it) } ?: "") }
        AlertDialog(
            onDismissRequest = { qtyFor = null },
            title = { Text(stringResource(R.string.adjust_quantity)) },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it.take(8) },
                    label = { Text(ri.ingredient.name.get(lang) + " (" + stringResource(unitLabel(ri.unit)) + ")") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = text.replace(',', '.').replace('٫', '.').toDoubleOrNull()
                    if (v != null && v > 0) {
                        // Stored for the base servings so it keeps scaling with the serving count.
                        val base = v * recipe.servings / resolved.servings
                        update(cust.copy(qtyOverrides = cust.qtyOverrides + (ri.originalKey to base)))
                    }
                    qtyFor = null
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { qtyFor = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun toPlain(s: String) = s.replace("¼", ".25").replace("½", ".5").replace("¾", ".75").let { if (it.startsWith(".")) "0$it" else it }

@Composable
fun PickIngredientDialog(
    title: String,
    options: List<Ingredient>,
    lang: com.shifadaily.app.domain.meal.Lang,
    searchable: Boolean,
    onPick: (Ingredient) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = if (query.isBlank()) options else {
        val q = PantryParser.normalize(query)
        options.filter { ing -> (listOf(ing.name.en, ing.name.ar) + ing.synonyms).any { PantryParser.normalize(it).contains(q) } }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (searchable) {
                    OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text(stringResource(R.string.search)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                if (filtered.isEmpty()) Text(stringResource(R.string.no_options), Modifier.padding(top = 8.dp))
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(filtered, key = { it.key }) { ing ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onPick(ing) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(ing.name.get(lang), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            TierBadge(ing.tier)
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
