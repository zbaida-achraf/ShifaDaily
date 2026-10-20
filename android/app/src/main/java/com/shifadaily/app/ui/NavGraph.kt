package com.shifadaily.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shifadaily.app.AppContainer
import com.shifadaily.app.R
import com.shifadaily.app.data.prefs.AppSettings
import com.shifadaily.app.ui.home.HomeScreen
import com.shifadaily.app.ui.meals.FoodGuideScreen
import com.shifadaily.app.ui.meals.FoodPreferencesScreen
import com.shifadaily.app.ui.meals.MealsScreen
import com.shifadaily.app.ui.meals.PantryScreen
import com.shifadaily.app.ui.meals.RecipeScreen
import com.shifadaily.app.ui.meals.SavedRecipesScreen
import com.shifadaily.app.ui.meds.HistoryScreen
import com.shifadaily.app.ui.meds.MedicationDetailScreen
import com.shifadaily.app.ui.meds.MedicationEditScreen
import com.shifadaily.app.ui.meds.MedicationsScreen
import com.shifadaily.app.ui.onboarding.OnboardingScreen
import com.shifadaily.app.ui.settings.InfoPage
import com.shifadaily.app.ui.settings.InfoScreen
import com.shifadaily.app.ui.settings.ReminderCheckScreen
import com.shifadaily.app.ui.settings.SchedulerLogScreen
import com.shifadaily.app.ui.settings.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val MEDS = "meds"
    const val MED_EDIT = "med_edit?id={id}"
    const val MED_DETAIL = "med/{id}"
    const val HISTORY = "history"
    const val MEALS = "meals"
    const val RECIPE = "recipe/{rid}?saved={saved}"
    const val PANTRY = "pantry"
    const val FOOD_PREFS = "food_prefs"
    const val SAVED = "saved"
    const val FOOD_GUIDE = "food_guide"
    const val SETTINGS = "settings"
    const val REMINDER_CHECK = "reminder_check"
    const val INFO = "info/{page}"
    const val LOG = "scheduler_log"

    fun medEdit(id: Long? = null) = if (id == null) "med_edit" else "med_edit?id=$id"
    fun medDetail(id: Long) = "med/$id"
    fun recipe(id: String, saved: Long? = null) = if (saved == null) "recipe/$id" else "recipe/$id?saved=$saved"
    fun info(page: InfoPage) = "info/${page.name}"
}

private data class TopLevel(val route: String, val icon: ImageVector, val label: Int)

private val topLevels = listOf(
    TopLevel(Routes.HOME, Icons.Filled.Home, R.string.nav_home),
    TopLevel(Routes.MEDS, Icons.Filled.Medication, R.string.nav_medications),
    TopLevel(Routes.MEALS, Icons.Filled.RestaurantMenu, R.string.nav_meals),
    TopLevel(Routes.SETTINGS, Icons.Filled.Settings, R.string.nav_settings),
)

@Composable
fun ShifaNavHost(
    container: AppContainer,
    settings: AppSettings?,
    pendingDoseId: Long?,
    onDoseHandled: () -> Unit,
) {
    if (settings == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val nav = rememberNavController()
    // Fixed at first composition; finishing onboarding navigates explicitly.
    val start = androidx.compose.runtime.remember { if (settings.onboardingDone) Routes.HOME else Routes.ONBOARDING }
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBar = topLevels.any { it.route == current }

    Scaffold(
        // Only the bottom bar pads content; each screen handles its own system insets.
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    topLevels.forEach { item ->
                        NavigationBarItem(
                            selected = current == item.route,
                            onClick = { nav.navigateTopLevel(item.route) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(container, settings, onFinished = { addMedication ->
                    nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    if (addMedication) nav.navigate(Routes.medEdit())
                })
            }
            composable(Routes.HOME) {
                HomeScreen(container, settings, nav, pendingDoseId, onDoseHandled)
            }
            composable(Routes.MEDS) { MedicationsScreen(container, nav) }
            composable(
                Routes.MED_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { entry ->
                val id = entry.arguments?.getLong("id")?.takeIf { it > 0 }
                MedicationEditScreen(container, id, onDone = { nav.popBackStack() })
            }
            composable(Routes.MED_DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                MedicationDetailScreen(container, entry.arguments!!.getLong("id"), nav)
            }
            composable(Routes.HISTORY) { HistoryScreen(container, onBack = { nav.popBackStack() }) }
            composable(Routes.MEALS) { MealsScreen(container, settings, nav) }
            composable(
                Routes.RECIPE,
                arguments = listOf(
                    navArgument("rid") { type = NavType.StringType },
                    navArgument("saved") { type = NavType.LongType; defaultValue = -1L },
                ),
            ) { entry ->
                RecipeScreen(
                    container, settings,
                    recipeId = entry.arguments!!.getString("rid")!!,
                    savedId = entry.arguments!!.getLong("saved").takeIf { it > 0 },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.PANTRY) { PantryScreen(container, onBack = { nav.popBackStack() }) }
            composable(Routes.FOOD_PREFS) { FoodPreferencesScreen(container, settings, onBack = { nav.popBackStack() }) }
            composable(Routes.SAVED) { SavedRecipesScreen(container, nav) }
            composable(Routes.FOOD_GUIDE) { FoodGuideScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.SETTINGS) { SettingsScreen(container, settings, nav) }
            composable(Routes.REMINDER_CHECK) { ReminderCheckScreen(container, settings, onBack = { nav.popBackStack() }) }
            composable(Routes.LOG) { SchedulerLogScreen(container, onBack = { nav.popBackStack() }) }
            composable(Routes.INFO, arguments = listOf(navArgument("page") { type = NavType.StringType })) { entry ->
                val page = runCatching { InfoPage.valueOf(entry.arguments!!.getString("page")!!) }.getOrDefault(InfoPage.ABOUT)
                InfoScreen(page, onBack = { nav.popBackStack() })
            }
        }
    }
}

fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        // HOME is always the root once onboarding is done (onboarding pops itself).
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
