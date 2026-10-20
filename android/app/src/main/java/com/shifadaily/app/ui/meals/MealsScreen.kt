package com.shifadaily.app.ui.meals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.shifadaily.app.AppContainer
import com.shifadaily.app.R
import com.shifadaily.app.data.prefs.AppSettings
import com.shifadaily.app.domain.meal.IngredientCatalog
import com.shifadaily.app.domain.meal.MealType
import com.shifadaily.app.domain.meal.PantryParser
import com.shifadaily.app.domain.meal.RecipeCatalog
import com.shifadaily.app.domain.meal.RecipeEngine
import com.shifadaily.app.domain.meal.Suggestion
import com.shifadaily.app.domain.meal.SuggestionFilters
import com.shifadaily.app.ui.Routes
import com.shifadaily.app.ui.components.NoticeCard
import com.shifadaily.app.ui.components.SectionTitle
import com.shifadaily.app.ui.components.ShifaCard
import com.shifadaily.app.ui.components.ShifaTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealsScreen(container: AppContainer, settings: AppSettings, nav: NavHostController) {
    val pantry by container.meals.observePantry().collectAsState(initial = emptyList())
    val prefs by container.meals.observePreferences().collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val lang = currentLang()
    var input by rememberSaveable { mutableStateOf("") }
    var unmatched by remember { mutableStateOf<List<String>>(emptyList()) }
    var mealType by rememberSaveable { mutableStateOf<MealType?>(null) }
    var rotation by rememberSaveable { mutableIntStateOf(0) }

    val p = prefs
    val pantryKeys = pantry.mapNotNull { it.ingredientKey }.toSet()
    val result = remember(pantryKeys, p, mealType, settings) {
        p?.let {
            RecipeEngine.suggest(
                RecipeCatalog.all, pantryKeys, it,
                SuggestionFilters(mealType, settings.maxCookingMinutes, settings.budget, settings.dietPattern),
            )
        }
    }

    fun submit() {
        val parsed = PantryParser.parse(input)
        scope.launch {
            container.meals.addPantryKeys(parsed.keys)
            parsed.unmatched.forEach { container.meals.addPantryCustom(it) }
        }
        unmatched = parsed.unmatched
        input = ""
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = { ShifaTopBar(stringResource(R.string.nav_meals)) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { nav.navigate(Routes.PANTRY) }, label = { Text(stringResource(R.string.pantry)) },
                        leadingIcon = { Icon(Icons.Filled.Kitchen, null, Modifier.size(18.dp)) })
                    AssistChip(onClick = { nav.navigate(Routes.FOOD_PREFS) }, label = { Text(stringResource(R.string.food_preferences)) },
                        leadingIcon = { Icon(Icons.Filled.Tune, null, Modifier.size(18.dp)) })
                    AssistChip(onClick = { nav.navigate(Routes.SAVED) }, label = { Text(stringResource(R.string.saved_recipes)) },
                        leadingIcon = { Icon(Icons.Filled.Bookmark, null, Modifier.size(18.dp)) })
                    AssistChip(onClick = { nav.navigate(Routes.FOOD_GUIDE) }, label = { Text(stringResource(R.string.food_guide)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.size(18.dp)) })
                }
            }
            item {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(300) },
                    label = { Text(stringResource(R.string.what_do_you_have)) },
                    placeholder = { Text(stringResource(R.string.what_do_you_have_example)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (input.isNotBlank()) submit() }),
                )
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                    Button(enabled = input.isNotBlank(), onClick = { submit() }) { Text(stringResource(R.string.add_to_pantry)) }
                }
                if (unmatched.isNotEmpty()) {
                    Text(
                        stringResource(R.string.unmatched_items, unmatched.joinToString("، ".takeIf { lang == com.shifadaily.app.domain.meal.Lang.AR } ?: ", ")),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (pantry.isNotEmpty()) {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        pantry.take(30).forEach { item ->
                            val name = item.ingredientKey?.let { IngredientCatalog[it]?.name?.get(lang) } ?: item.customName.orEmpty()
                            InputChip(selected = false, onClick = { scope.launch { container.meals.removePantry(item.id) } },
                                label = { Text(name) },
                                trailingIcon = { Icon(Icons.Filled.Close, stringResource(R.string.cd_remove_item, name), Modifier.size(16.dp)) })
                        }
                    }
                }
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = mealType == null, onClick = { mealType = null }, label = { Text(stringResource(R.string.all_meals)) })
                    MealType.entries.forEach { t ->
                        FilterChip(selected = mealType == t, onClick = { mealType = t }, label = { Text(stringResource(mealTypeLabel(t))) })
                    }
                }
            }
            val res = result ?: return@LazyColumn
            if (res.hiddenForConflicts > 0) {
                item { NoticeCard(pluralString(R.plurals.hidden_for_conflicts, res.hiddenForConflicts)) }
            }
            if (res.suggestions.isEmpty()) {
                item { NoticeCard(stringResource(R.string.meals_none)) }
            } else {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle(stringResource(R.string.suggestions), Modifier.weight(1f))
                        TextButton(onClick = { rotation++ }) {
                            Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.new_suggestion))
                        }
                    }
                }
                val list = res.suggestions.let { l -> val k = rotation % l.size; l.drop(k) + l.take(k) }
                items(list, key = { it.recipe.id }) { s -> SuggestionCard(s, lang) { nav.navigate(Routes.recipe(s.recipe.id)) } }
            }
            item {
                Text(stringResource(R.string.meal_disclaimer_short), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SuggestionCard(s: Suggestion, lang: com.shifadaily.app.domain.meal.Lang, onClick: () -> Unit) {
    ShifaCard(onClick = onClick) {
        Text(s.recipe.title.get(lang), style = MaterialTheme.typography.titleMedium)
        Text(s.recipe.description.get(lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.minutes_short, s.recipe.minutes), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.have_need_count, s.have.size, s.need.size),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
            )
        }
        if (s.autoRemoved.isNotEmpty()) {
            val names = s.autoRemoved.keys.mapNotNull { IngredientCatalog[it]?.name?.get(lang) }
            Text(stringResource(R.string.auto_removed, names.joinToString(", ")), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun pluralString(id: Int, count: Int): String =
    androidx.compose.ui.platform.LocalResources.current.getQuantityString(id, count, count)
