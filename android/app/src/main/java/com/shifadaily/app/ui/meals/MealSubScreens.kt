package com.shifadaily.app.ui.meals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.shifadaily.app.AppContainer
import com.shifadaily.app.R
import com.shifadaily.app.data.db.FoodPreferenceEntity
import com.shifadaily.app.data.db.PreferenceKind
import com.shifadaily.app.data.prefs.AppSettings
import com.shifadaily.app.domain.meal.Allergen
import com.shifadaily.app.domain.meal.Budget
import com.shifadaily.app.domain.meal.DietPattern
import com.shifadaily.app.domain.meal.IngredientCatalog
import com.shifadaily.app.domain.meal.RecipeCatalog
import com.shifadaily.app.domain.meal.Tier
import com.shifadaily.app.ui.Routes
import com.shifadaily.app.ui.components.ConfirmDialog
import com.shifadaily.app.ui.components.EmptyState
import com.shifadaily.app.ui.components.NoticeCard
import com.shifadaily.app.ui.components.SectionTitle
import com.shifadaily.app.ui.components.ShifaCard
import com.shifadaily.app.ui.components.ShifaTopBar
import com.shifadaily.app.ui.components.TierBadge
import com.shifadaily.app.util.Formats
import kotlinx.coroutines.launch
import java.time.ZoneId

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantryScreen(container: AppContainer, onBack: () -> Unit) {
    val pantry by container.meals.observePantry().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val lang = currentLang()
    var adding by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        ShifaTopBar(stringResource(R.string.pantry), onBack = onBack) {
            if (pantry.isNotEmpty()) IconButton(onClick = { confirmClear = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.clear_pantry)) }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.pantry_explain), style = MaterialTheme.typography.bodyMedium)
            if (pantry.isEmpty()) {
                EmptyState(Icons.Filled.Kitchen, stringResource(R.string.pantry_empty_title), stringResource(R.string.pantry_empty_body))
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pantry.forEach { item ->
                        val name = item.ingredientKey?.let { IngredientCatalog[it]?.name?.get(lang) } ?: item.customName.orEmpty()
                        InputChip(selected = false, onClick = { scope.launch { container.meals.removePantry(item.id) } }, label = { Text(name) },
                            trailingIcon = { Icon(Icons.Filled.Close, stringResource(R.string.cd_remove_item, name), Modifier.size(16.dp)) })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = custom, onValueChange = { custom = it.take(60) }, singleLine = true,
                    label = { Text(stringResource(R.string.add_free_text)) }, modifier = Modifier.weight(1f))
                IconButton(enabled = custom.isNotBlank(), onClick = {
                    val parsed = com.shifadaily.app.domain.meal.PantryParser.parse(custom)
                    scope.launch {
                        container.meals.addPantryKeys(parsed.keys)
                        parsed.unmatched.forEach { container.meals.addPantryCustom(it) }
                    }
                    custom = ""
                }) { Icon(Icons.Filled.Add, stringResource(R.string.add)) }
            }
            OutlinedButton(onClick = { adding = true }) { Text(stringResource(R.string.choose_from_list)) }
        }
    }
    if (adding) {
        val have = pantry.mapNotNull { it.ingredientKey }.toSet()
        PickIngredientDialog(stringResource(R.string.choose_from_list), IngredientCatalog.all.filter { it.key !in have }, lang, searchable = true,
            onPick = { scope.launch { container.meals.addPantryKeys(listOf(it.key)) } }, onDismiss = { adding = false })
    }
    if (confirmClear) {
        ConfirmDialog(stringResource(R.string.clear_pantry), stringResource(R.string.clear_pantry_body), stringResource(R.string.clear),
            onConfirm = { confirmClear = false; scope.launch { container.meals.clearPantry() } }, onDismiss = { confirmClear = false }, destructive = true)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodPreferencesScreen(container: AppContainer, settings: AppSettings, onBack: () -> Unit) {
    val prefs by container.meals.observePreferenceEntities().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val lang = currentLang()
    var picking by remember { mutableStateOf<PreferenceKind?>(null) }
    var customFor by remember { mutableStateOf<PreferenceKind?>(null) }
    var customText by remember { mutableStateOf("") }

    Scaffold(topBar = { ShifaTopBar(stringResource(R.string.food_preferences), onBack = onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { NoticeCard(stringResource(R.string.allergy_vs_preference), warning = true) }

            item { SectionTitle(stringResource(R.string.allergies)) }
            item {
                Text(stringResource(R.string.allergen_groups), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Allergen.entries.forEach { a ->
                        val existing = prefs.firstOrNull { it.kind == PreferenceKind.ALLERGY && it.allergen == a }
                        FilterChip(
                            selected = existing != null,
                            onClick = {
                                scope.launch {
                                    if (existing != null) container.meals.removePreference(existing.id)
                                    else container.meals.addPreference(PreferenceKind.ALLERGY, allergen = a)
                                }
                            },
                            label = { Text(stringResource(allergenLabel(a))) },
                        )
                    }
                }
            }
            listOf(
                PreferenceKind.ALLERGY to R.string.allergy_items,
                PreferenceKind.AVOID to R.string.avoid_items,
                PreferenceKind.LIKE to R.string.like_items,
                PreferenceKind.DISLIKE to R.string.dislike_items,
            ).forEach { (kind, title) ->
                item {
                    ShifaCard {
                        Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                        val entries = prefs.filter { it.kind == kind && it.allergen == null }
                        if (entries.isEmpty()) Text(stringResource(R.string.none_yet), style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            entries.forEach { e -> PrefChip(e, lang) { scope.launch { container.meals.removePreference(e.id) } } }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { picking = kind }) { Text(stringResource(R.string.choose_from_list)) }
                            TextButton(onClick = { customFor = kind; customText = "" }) { Text(stringResource(R.string.add_free_text)) }
                        }
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.meal_settings)) }
            item {
                Text(stringResource(R.string.dietary_pattern), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DietPattern.entries.forEach { d ->
                        FilterChip(selected = settings.dietPattern == d, onClick = { scope.launch { container.settings.setDietPattern(d) } },
                            label = { Text(stringResource(dietLabel(d))) })
                    }
                }
            }
            item {
                Text(stringResource(R.string.max_cooking_time), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 15, 30, 45, 60).forEach { m ->
                        FilterChip(selected = settings.maxCookingMinutes == m, onClick = { scope.launch { container.settings.setMaxCookingMinutes(m) } },
                            label = { Text(if (m == 0) stringResource(R.string.no_limit) else stringResource(R.string.minutes_short, m)) })
                    }
                }
            }
            item {
                Text(stringResource(R.string.budget), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Budget.entries.forEach { b ->
                        FilterChip(selected = settings.budget == b, onClick = { scope.launch { container.settings.setBudget(b) } },
                            label = { Text(stringResource(budgetLabel(b))) })
                    }
                }
            }
            item {
                Text(stringResource(R.string.default_servings), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..6).forEach { n ->
                        FilterChip(selected = settings.defaultServings == n, onClick = { scope.launch { container.settings.setDefaultServings(n) } },
                            label = { Text(n.toString()) })
                    }
                }
            }
        }
    }

    picking?.let { kind ->
        val taken = prefs.filter { it.kind == kind }.mapNotNull { it.ingredientKey }.toSet()
        PickIngredientDialog(stringResource(R.string.choose_from_list), IngredientCatalog.all.filter { it.key !in taken }, lang, searchable = true,
            onPick = { ing -> scope.launch { container.meals.addPreference(kind, ingredientKey = ing.key) }; picking = null },
            onDismiss = { picking = null })
    }
    customFor?.let { kind ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { customFor = null },
            title = { Text(stringResource(R.string.add_free_text)) },
            text = {
                Column {
                    OutlinedTextField(value = customText, onValueChange = { customText = it.take(60) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (kind == PreferenceKind.ALLERGY) {
                        Text(stringResource(R.string.custom_allergy_note), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = customText.isNotBlank(), onClick = {
                    val t = customText
                    scope.launch { container.meals.addPreference(kind, custom = t) }
                    customFor = null
                }) { Text(stringResource(R.string.add)) }
            },
            dismissButton = { TextButton(onClick = { customFor = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun PrefChip(e: FoodPreferenceEntity, lang: com.shifadaily.app.domain.meal.Lang, onRemove: () -> Unit) {
    val name = e.ingredientKey?.let { IngredientCatalog[it]?.name?.get(lang) } ?: e.customName.orEmpty()
    InputChip(selected = false, onClick = onRemove, label = { Text(name) },
        trailingIcon = { Icon(Icons.Filled.Close, stringResource(R.string.cd_remove_item, name), Modifier.size(16.dp)) })
}

@Composable
fun SavedRecipesScreen(container: AppContainer, nav: NavHostController) {
    val saved by container.meals.observeSaved().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val lang = currentLang()
    val context = LocalContext.current
    Scaffold(topBar = { ShifaTopBar(stringResource(R.string.saved_recipes), onBack = { nav.popBackStack() }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (saved.isEmpty()) {
                item { EmptyState(Icons.Filled.Bookmark, stringResource(R.string.saved_empty_title), stringResource(R.string.saved_empty_body)) }
            }
            items(saved, key = { it.id }) { s ->
                val r = RecipeCatalog[s.baseRecipeId] ?: return@items
                ShifaCard(onClick = { nav.navigate(Routes.recipe(r.id, s.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.title.get(lang), style = MaterialTheme.typography.titleSmall)
                            val d = s.savedAt.atZone(ZoneId.systemDefault()).toLocalDate()
                            Text(stringResource(R.string.saved_on, Formats.date(context, d)), style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { scope.launch { container.meals.deleteSaved(s.id) } }) {
                            Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    }
}

/** The user-supplied leaflet, presented with attribution. See docs/FOOD-GUIDE-SOURCE.md. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodGuideScreen(onBack: () -> Unit) {
    val lang = currentLang()
    Scaffold(topBar = { ShifaTopBar(stringResource(R.string.food_guide), onBack = onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { NoticeCard(stringResource(R.string.guide_source)) }
            listOf(
                Triple(Tier.FREELY, R.string.tier_freely, R.string.tier_freely_desc),
                Triple(Tier.LIMITED, R.string.tier_limited, R.string.tier_limited_desc),
                Triple(Tier.OCCASIONAL, R.string.tier_occasional, R.string.tier_occasional_desc),
            ).forEach { (tier, title, desc) ->
                item {
                    ShifaCard {
                        TierBadge(tier)
                        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val pictured = IngredientCatalog.byTier(tier).filter { it.pictured }
                        val inferred = IngredientCatalog.byTier(tier).filter { !it.pictured }
                        Text(pictured.joinToString(" · ") { it.name.get(lang) }, style = MaterialTheme.typography.bodyMedium)
                        if (inferred.isNotEmpty()) {
                            Text(stringResource(R.string.guide_inferred, inferred.joinToString(" · ") { it.name.get(lang) }),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { SectionTitle(stringResource(R.string.guide_meals_title)) }
            item {
                ShifaCard {
                    Text(stringResource(R.string.guide_breakfast_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.guide_breakfast_body), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.guide_lunch_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.guide_lunch_body), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.guide_snack_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.guide_snack_body), style = MaterialTheme.typography.bodyMedium)
                }
            }
            item { SectionTitle(stringResource(R.string.guide_info_title)) }
            item { ShifaCard { Text(stringResource(R.string.guide_info_body), style = MaterialTheme.typography.bodyMedium) } }
            item { SectionTitle(stringResource(R.string.guide_exchanges_title)) }
            item {
                ShifaCard {
                    Text(stringResource(R.string.guide_exchanges_intro), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf(
                        R.string.guide_ex_grains_title to R.string.guide_ex_grains,
                        R.string.guide_ex_fats_title to R.string.guide_ex_fats,
                        R.string.guide_ex_starches_title to R.string.guide_ex_starches,
                        R.string.guide_ex_fruits_title to R.string.guide_ex_fruits,
                    ).forEach { (t, b) ->
                        Text(stringResource(t), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(b), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { SectionTitle(stringResource(R.string.guide_tips_title)) }
            item {
                ShifaCard {
                    listOf(R.string.guide_tip1, R.string.guide_tip2, R.string.guide_tip3, R.string.guide_tip4).forEachIndexed { i, t ->
                        Text("${i + 1}. " + stringResource(t), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { NoticeCard(stringResource(R.string.guide_disclaimer), warning = true) }
        }
    }
}
