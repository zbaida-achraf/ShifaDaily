package com.shifadaily.app.ui.settings

import android.Manifest
import android.media.AudioManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.shifadaily.app.BuildConfig
import com.shifadaily.app.R
import com.shifadaily.app.alarm.AlarmMode
import com.shifadaily.app.alarm.ReminderNotifier
import com.shifadaily.app.data.prefs.AppSettings
import com.shifadaily.app.data.prefs.ThemeMode
import com.shifadaily.app.domain.dose.DoseRules
import com.shifadaily.app.ui.Routes
import com.shifadaily.app.ui.components.ConfirmDialog
import com.shifadaily.app.ui.components.NoticeCard
import com.shifadaily.app.ui.components.SectionTitle
import com.shifadaily.app.ui.components.ShifaCard
import com.shifadaily.app.ui.components.ShifaTopBar
import com.shifadaily.app.ui.theme.LocalExtraColors
import com.shifadaily.app.util.Formats
import com.shifadaily.app.util.LocaleHelper
import com.shifadaily.app.util.SystemSettings
import com.shifadaily.app.util.rememberReminderReadiness
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

enum class InfoPage { PRIVACY, DISCLAIMER, ABOUT }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(container: AppContainer, settings: AppSettings, nav: NavHostController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val res = androidx.compose.ui.platform.LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    var eraseStep by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching { container.data.exportTo(uri) }.isSuccess
            snackbar.showSnackbar(res.getString(if (ok) R.string.export_done else R.string.export_failed))
        }
    }
    var confirmExport by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = { ShifaTopBar(stringResource(R.string.nav_settings)) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { SectionTitle(stringResource(R.string.language)) }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val current = LocaleHelper.normalize(settings.language) ?: "en"
                    listOf("ar" to "العربية", "en" to "English").forEach { (tag, label) ->
                        FilterChip(selected = current == tag, label = { Text(label) }, onClick = {
                            scope.launch { container.settings.setLanguage(tag); LocaleHelper.apply(tag) }
                        })
                    }
                }
                Text(stringResource(R.string.language_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { SectionTitle(stringResource(R.string.theme)) }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ThemeMode.SYSTEM to R.string.theme_system, ThemeMode.LIGHT to R.string.theme_light, ThemeMode.DARK to R.string.theme_dark)
                        .forEach { (m, l) ->
                            FilterChip(selected = settings.theme == m, onClick = { scope.launch { container.settings.setTheme(m) } }, label = { Text(stringResource(l)) })
                        }
                }
            }
            item { SectionTitle(stringResource(R.string.reminders)) }
            item { NavRow(stringResource(R.string.reminder_check), stringResource(R.string.reminder_check_sub)) { nav.navigate(Routes.REMINDER_CHECK) } }
            item {
                Text(stringResource(R.string.snooze_length), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DoseRules.SNOOZE_CHOICES_MINUTES.forEach { m ->
                        FilterChip(selected = settings.snoozeMinutes == m, onClick = { scope.launch { container.settings.setSnoozeMinutes(m) } },
                            label = { Text(stringResource(R.string.minutes_short, m)) })
                    }
                }
            }
            item {
                Text(stringResource(R.string.max_snoozes), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DoseRules.MAX_SNOOZE_CHOICES.forEach { n ->
                        FilterChip(selected = settings.maxSnoozes == n, onClick = { scope.launch { container.settings.setMaxSnoozes(n) } },
                            label = { Text(n.toString()) })
                    }
                }
            }
            item { NavRow(stringResource(R.string.sound_settings), stringResource(R.string.sound_settings_sub)) { SystemSettings.openNotificationSettings(context) } }

            item { SectionTitle(stringResource(R.string.your_data)) }
            item { NavRow(stringResource(R.string.export_data), stringResource(R.string.export_data_sub)) { confirmExport = true } }
            item { NavRow(stringResource(R.string.erase_data), stringResource(R.string.erase_data_sub), destructive = true) { eraseStep = 1 } }

            item { SectionTitle(stringResource(R.string.about_section)) }
            item { NavRow(stringResource(R.string.privacy_policy), null) { nav.navigate(Routes.info(InfoPage.PRIVACY)) } }
            item { NavRow(stringResource(R.string.health_disclaimer_title), null) { nav.navigate(Routes.info(InfoPage.DISCLAIMER)) } }
            item { NavRow(stringResource(R.string.about_app), stringResource(R.string.version_value, BuildConfig.VERSION_NAME)) { nav.navigate(Routes.info(InfoPage.ABOUT)) } }
            item { NavRow(stringResource(R.string.scheduler_log), stringResource(R.string.scheduler_log_sub)) { nav.navigate(Routes.LOG) } }
        }
    }

    if (confirmExport) {
        ConfirmDialog(stringResource(R.string.export_data), stringResource(R.string.export_warning), stringResource(R.string.continue_label),
            onConfirm = { confirmExport = false; exportLauncher.launch("shifadaily-export.json") }, onDismiss = { confirmExport = false })
    }
    when (eraseStep) {
        1 -> ConfirmDialog(stringResource(R.string.erase_data), stringResource(R.string.erase_warning_1), stringResource(R.string.continue_label),
            onConfirm = { eraseStep = 2 }, onDismiss = { eraseStep = 0 }, destructive = true)
        2 -> ConfirmDialog(stringResource(R.string.erase_confirm_title), stringResource(R.string.erase_warning_2), stringResource(R.string.erase_now),
            onConfirm = {
                eraseStep = 0
                scope.launch {
                    container.data.eraseAll()
                    // Restart into onboarding in a fresh task. recreate() would restore the saved
                    // navigation state and leave the user on this screen (found in emulator QA).
                    val activity = context as? android.app.Activity
                    activity?.startActivity(
                        android.content.Intent(context, com.shifadaily.app.MainActivity::class.java)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    activity?.finish()
                }
            }, onDismiss = { eraseStep = 0 }, destructive = true)
    }
}

@Composable
private fun NavRow(title: String, subtitle: String?, destructive: Boolean = false, onClick: () -> Unit) {
    ShifaCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
fun ReminderCheckScreen(container: AppContainer, settings: AppSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    val r = rememberReminderReadiness()
    val last by container.engine.lastResult.collectAsState()
    var testScheduled by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Scaffold(topBar = { ShifaTopBar(stringResource(R.string.reminder_check), onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.reminder_check_intro), style = MaterialTheme.typography.bodyMedium)

            CheckRow(r.notificationsAllowed, R.string.check_notifications_ok, R.string.check_notifications_off) {
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= 33 && !r.notificationsAllowed) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    SystemSettings.openNotificationSettings(context)
                }) { Text(stringResource(R.string.open_settings)) }
            }
            CheckRow(r.exactAlarmsAllowed, R.string.check_exact_ok, R.string.check_exact_off) {
                Button(onClick = { SystemSettings.openExactAlarmSettings(context) }) { Text(stringResource(R.string.open_alarm_settings)) }
            }
            CheckRow(r.soundChannelOk != false, R.string.check_sound_ok, R.string.check_sound_off) {
                Button(onClick = { SystemSettings.openChannelSettings(context, ReminderNotifier.CH_SOUND) }) { Text(stringResource(R.string.open_settings)) }
            }
            val volumeOk = r.ringerMode == AudioManager.RINGER_MODE_NORMAL && r.notificationVolume > 0
            CheckRow(volumeOk, R.string.check_volume_ok, when (r.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> R.string.check_volume_silent
                AudioManager.RINGER_MODE_VIBRATE -> R.string.check_volume_vibrate
                else -> R.string.check_volume_zero
            }, detail = stringResource(R.string.volume_value, r.notificationVolume, r.notificationVolumeMax)) {}
            CheckRow(!r.dndActive, R.string.check_dnd_ok, R.string.check_dnd_on) {}
            CheckRow(!r.batteryOptimized, R.string.check_battery_ok, R.string.check_battery_optimized, neutralWhenBad = true) {
                OutlinedButton(onClick = { SystemSettings.openBatterySettings(context) }) { Text(stringResource(R.string.open_battery_settings)) }
            }

            ShifaCard {
                Text(stringResource(R.string.next_reminder_title), style = MaterialTheme.typography.titleSmall)
                val l = last
                if (l?.nextAlarm == null) {
                    Text(stringResource(R.string.no_upcoming_reminder), style = MaterialTheme.typography.bodyMedium)
                } else {
                    val t = l.nextAlarm.atZone(ZoneId.systemDefault())
                    Text(Formats.date(context, t.toLocalDate()) + " · " + Formats.time(context, t.toLocalTime()), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(if (l.mode == AlarmMode.EXACT) R.string.mode_exact else R.string.mode_inexact), style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(stringResource(R.string.test_reminder_explain), style = MaterialTheme.typography.bodyMedium)
            Button(enabled = r.notificationsAllowed, onClick = {
                container.notifier.ensureChannels(settings.language)
                container.scheduler.scheduleTest(Instant.now().plusSeconds(60))
                testScheduled = true
            }) { Text(stringResource(R.string.send_test_reminder)) }
            if (testScheduled) Text(stringResource(R.string.test_reminder_scheduled), color = MaterialTheme.colorScheme.primary)

            NoticeCard(stringResource(R.string.reliability_limits), warning = true)
        }
    }
}

@Composable
private fun CheckRow(
    ok: Boolean,
    okText: Int,
    badText: Int,
    detail: String? = null,
    neutralWhenBad: Boolean = false,
    action: @Composable () -> Unit,
) {
    val extra = LocalExtraColors.current
    ShifaCard {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                if (ok) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                contentDescription = stringResource(if (ok) R.string.cd_ok else R.string.cd_attention),
                tint = if (ok) MaterialTheme.colorScheme.primary else if (neutralWhenBad) MaterialTheme.colorScheme.onSurfaceVariant else extra.warning,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(if (ok) okText else badText), style = MaterialTheme.typography.bodyMedium)
                if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!ok) Row(Modifier.padding(top = 6.dp)) { action() }
            }
        }
    }
}

@Composable
fun SchedulerLogScreen(container: AppContainer, onBack: () -> Unit) {
    val log by container.db.schedulerLogDao().observeRecent().collectAsState(initial = emptyList())
    val context = LocalContext.current
    Scaffold(topBar = { ShifaTopBar(stringResource(R.string.scheduler_log), onBack = onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item { Text(stringResource(R.string.scheduler_log_explain), style = MaterialTheme.typography.bodySmall) }
            items(log, key = { it.id }) { e ->
                val t = e.at.atZone(ZoneId.systemDefault())
                Column {
                    Text(Formats.date(context, t.toLocalDate()) + " " + Formats.time(context, t.toLocalTime()) + " · " + e.event,
                        style = MaterialTheme.typography.labelMedium)
                    // Technical detail is kept in English on purpose (troubleshooting codes, no health data).
                    Text(e.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
fun InfoScreen(page: InfoPage, onBack: () -> Unit) {
    val (title, body) = when (page) {
        InfoPage.PRIVACY -> R.string.privacy_policy to R.string.privacy_policy_body
        InfoPage.DISCLAIMER -> R.string.health_disclaimer_title to R.string.health_disclaimer_full
        InfoPage.ABOUT -> R.string.about_app to R.string.about_body
    }
    Scaffold(topBar = { ShifaTopBar(stringResource(title), onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (page == InfoPage.ABOUT) {
                Text(stringResource(R.string.app_name_full), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.version_value, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium)
            }
            Text(stringResource(body), style = MaterialTheme.typography.bodyLarge)
            if (page == InfoPage.PRIVACY || page == InfoPage.ABOUT) {
                Text(stringResource(R.string.website_link), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

