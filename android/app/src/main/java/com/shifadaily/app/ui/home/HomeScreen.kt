package com.shifadaily.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.shifadaily.app.AppContainer
import com.shifadaily.app.R
import com.shifadaily.app.alarm.AlarmMode
import com.shifadaily.app.data.db.DoseRecordEntity
import com.shifadaily.app.data.prefs.AppSettings
import com.shifadaily.app.domain.dose.DoseRules
import com.shifadaily.app.domain.dose.DoseStatus
import com.shifadaily.app.ui.Routes
import com.shifadaily.app.ui.components.ChipKind
import com.shifadaily.app.ui.components.EmptyState
import com.shifadaily.app.ui.components.NoticeCard
import com.shifadaily.app.ui.components.SectionTitle
import com.shifadaily.app.ui.components.ShifaCard
import com.shifadaily.app.ui.components.StatusChip
import com.shifadaily.app.ui.components.chipKind
import com.shifadaily.app.ui.components.statusLabel
import com.shifadaily.app.ui.meals.mealTypeLabel
import com.shifadaily.app.ui.navigateTopLevel
import com.shifadaily.app.util.Formats
import com.shifadaily.app.util.isolate
import com.shifadaily.app.util.LocaleHelper
import com.shifadaily.app.util.SystemSettings
import com.shifadaily.app.util.rememberReminderReadiness
import java.time.ZoneId

@Composable
fun HomeScreen(
    container: AppContainer,
    settings: AppSettings,
    nav: NavHostController,
    pendingDoseId: Long?,
    onDoseHandled: () -> Unit,
) {
    val vm: HomeViewModel = viewModel { HomeViewModel(container) }
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val readiness = rememberReminderReadiness()
    var doseDialog by remember { mutableStateOf<DoseRecordEntity?>(null) }

    LaunchedEffect(pendingDoseId) {
        if (pendingDoseId != null) {
            doseDialog = vm.record(pendingDoseId)
            onDoseHandled()
        }
    }

    val s = state ?: return
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            val hour = s.now.atZone(ZoneId.systemDefault()).hour
            val greeting = when (hour) {
                in 4..11 -> R.string.greeting_morning
                in 12..17 -> R.string.greeting_afternoon
                else -> R.string.greeting_evening
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(greeting), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                    Text(Formats.longDate(context, s.today), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Missing essentials, shown only when relevant.
        if (s.hasMedications && !readiness.notificationsAllowed) {
            item {
                NoticeCard(stringResource(R.string.home_warn_notifications), warning = true) {
                    TextButton(onClick = { SystemSettings.openNotificationSettings(context) }) { Text(stringResource(R.string.fix)) }
                }
            }
        }
        if (s.hasMedications && !readiness.exactAlarmsAllowed) {
            item {
                NoticeCard(stringResource(R.string.home_warn_exact), warning = true) {
                    TextButton(onClick = { SystemSettings.openExactAlarmSettings(context) }) { Text(stringResource(R.string.fix)) }
                }
            }
        }

        item { SectionTitle(stringResource(R.string.home_today)) }
        if (!s.hasMedications) {
            item {
                ShifaCard {
                    EmptyState(Icons.Filled.Medication, stringResource(R.string.home_no_meds_title), stringResource(R.string.home_no_meds_body)) {
                        Button(onClick = { nav.navigate(Routes.medEdit()) }) { Text(stringResource(R.string.add_medication)) }
                    }
                }
            }
        } else if (s.items.isEmpty()) {
            item { ShifaCard { Text(stringResource(R.string.home_nothing_today), style = MaterialTheme.typography.bodyLarge) } }
        } else {
            items(s.items, key = { "${it.med?.id}-${it.time}-${it.record?.id}" }) { item ->
                TodayRow(item, onTake = { vm.take(item) }, onSkip = { vm.skip(item) }, onOpen = {
                    item.med?.let { nav.navigate(Routes.medDetail(it.id)) }
                })
            }
        }

        s.lastReconcile?.let { r ->
            if (r.nextAlarm != null) {
                item {
                    val t = r.nextAlarm.atZone(ZoneId.systemDefault())
                    val label = if (t.toLocalDate() == s.today) Formats.time(context, t.toLocalTime())
                    else Formats.date(context, t.toLocalDate()) + " · " + Formats.time(context, t.toLocalTime())
                    Text(
                        stringResource(if (r.mode == AlarmMode.EXACT) R.string.home_next_reminder else R.string.home_next_reminder_inexact, label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (s.attention.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.home_attention)) }
            items(s.attention, key = { "att-${it.id}" }) { r ->
                ShifaCard(onClick = { doseDialog = r }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(isolate(r.medicationName), style = MaterialTheme.typography.titleSmall)
                            Text(
                                Formats.date(context, r.scheduledDate) + " · " + Formats.time(context, r.scheduledTime),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        StatusChip(if (r.status == DoseStatus.MISSED && !r.notified) R.string.status_not_delivered else statusLabel(r.status), chipKind(r.status))
                    }
                }
            }
        }

        if (s.hasMedications) {
            item {
                ShifaCard {
                    Text(stringResource(R.string.home_adherence_title), style = MaterialTheme.typography.titleSmall)
                    if (s.adherence.total == 0) {
                        Text(stringResource(R.string.home_adherence_empty), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(
                            androidx.compose.ui.platform.LocalResources.current.getQuantityString(R.plurals.home_adherence_value, s.adherence.total, s.adherence.taken, s.adherence.total),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    TextButton(onClick = { nav.navigate(Routes.HISTORY) }) {
                        Icon(Icons.Filled.History, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.view_history))
                    }
                }
            }
        }

        item { SectionTitle(stringResource(R.string.home_meal_idea, stringResource(mealTypeLabel(s.mealType)))) }
        item {
            val lang = LocaleHelper.normalize(settings.language) ?: "en"
            val recipe = s.meal
            if (recipe == null) {
                ShifaCard { Text(stringResource(R.string.meals_none), style = MaterialTheme.typography.bodyMedium) }
            } else {
                ShifaCard(onClick = { nav.navigate(Routes.recipe(recipe.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                if (lang == "ar") recipe.title.ar else recipe.title.en,
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                if (lang == "ar") recipe.description.ar else recipe.description.en,
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AssistChip(onClick = { nav.navigateTopLevel(Routes.MEALS) }, label = { Text(stringResource(R.string.open_meal_assistant)) },
                    leadingIcon = { Icon(Icons.Filled.Restaurant, contentDescription = null, modifier = Modifier.size(18.dp)) })
                AssistChip(onClick = { nav.navigate(Routes.FOOD_GUIDE) }, label = { Text(stringResource(R.string.food_guide)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) })
            }
        }
    }

    doseDialog?.let { r ->
        DoseActionDialog(r, settings, onAction = { st -> vm.actOnRecord(r.id, st); doseDialog = null }, onDismiss = { doseDialog = null })
    }
}

@Composable
private fun TodayRow(item: TodayItem, onTake: () -> Unit, onSkip: () -> Unit, onOpen: () -> Unit) {
    val context = LocalContext.current
    ShifaCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Formats.time(context, item.time), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(isolate(item.name), style = MaterialTheme.typography.titleSmall)
                Text(isolate(item.dose), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val (label, kind) = when (item.state) {
                TodayState.UPCOMING -> R.string.status_upcoming to ChipKind.NEUTRAL
                TodayState.DUE -> R.string.status_due to ChipKind.WARN
                TodayState.TAKEN -> R.string.status_taken to ChipKind.GOOD
                TodayState.SKIPPED -> R.string.status_skipped to ChipKind.NEUTRAL
                TodayState.SNOOZED -> R.string.status_snoozed to ChipKind.WARN
                TodayState.MISSED -> R.string.status_missed to ChipKind.BAD
                TodayState.BEFORE_START -> R.string.status_before_start to ChipKind.NEUTRAL
            }
            StatusChip(label, kind)
        }
        if (item.state in setOf(TodayState.UPCOMING, TodayState.DUE, TodayState.SNOOZED, TodayState.BEFORE_START)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onTake) { Text(stringResource(R.string.action_taken)) }
                OutlinedButton(onClick = onSkip) { Text(stringResource(R.string.action_skip)) }
            }
        }
    }
}

@Composable
fun DoseActionDialog(record: DoseRecordEntity, settings: AppSettings, onAction: (DoseStatus) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val canSnooze = DoseRules.isOpen(record.status) && DoseRules.canSnooze(record.snoozeCount, settings.maxSnoozes)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(isolate(record.medicationName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(isolate(record.doseLabel), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.scheduled_for, Formats.date(context, record.scheduledDate) + " · " + Formats.time(context, record.scheduledTime)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.current_status) + " ", style = MaterialTheme.typography.bodyMedium)
                    StatusChip(statusLabel(record.status), chipKind(record.status))
                }
                if (canSnooze) {
                    TextButton(onClick = { onAction(DoseStatus.SNOOZED) }) {
                        Text(stringResource(R.string.action_snooze_minutes, settings.snoozeMinutes))
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onAction(DoseStatus.TAKEN) }) { Text(stringResource(R.string.action_taken)) } },
        dismissButton = { OutlinedButton(onClick = { onAction(DoseStatus.SKIPPED) }) { Text(stringResource(R.string.action_skip)) } },
    )
}
