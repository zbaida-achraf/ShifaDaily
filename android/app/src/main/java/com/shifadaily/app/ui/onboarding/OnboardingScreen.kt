package com.shifadaily.app.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shifadaily.app.AppContainer
import com.shifadaily.app.R
import com.shifadaily.app.data.prefs.AppSettings
import com.shifadaily.app.ui.components.NoticeCard
import com.shifadaily.app.util.LocaleHelper
import com.shifadaily.app.util.SystemSettings
import com.shifadaily.app.util.rememberReminderReadiness
import kotlinx.coroutines.launch
import java.time.Instant

private const val STEPS = 5

@Composable
fun OnboardingScreen(container: AppContainer, settings: AppSettings, onFinished: (addMedication: Boolean) -> Unit) {
    // Survives the activity recreation triggered by a language change.
    var step by rememberSaveable { mutableIntStateOf(0) }
    var accepted by rememberSaveable { mutableStateOf(settings.disclaimerAccepted) }
    val scope = rememberCoroutineScope()

    fun finish(add: Boolean) = scope.launch {
        container.settings.setOnboardingDone()
        container.engine.reconcile("onboarding-done")
        onFinished(add)
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
        LinearProgressIndicator(progress = { (step + 1f) / STEPS }, modifier = Modifier.fillMaxWidth())
        Text(
            stringResource(R.string.onboarding_step, step + 1, STEPS),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (step) {
                0 -> WelcomeStep(settings.language ?: LocaleHelper.appliedLanguage() ?: LocaleHelper.deviceDefault()) { tag ->
                    scope.launch {
                        container.settings.setLanguage(tag)
                        LocaleHelper.apply(tag)
                    }
                }
                1 -> DisclaimerStep(accepted) { accepted = it }
                2 -> PrivacyStep()
                3 -> RemindersStep(container)
                else -> FirstMedicationStep()
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { step-- }) { Text(stringResource(R.string.back)) } else Spacer(Modifier.size(1.dp))
            when (step) {
                STEPS - 1 -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { finish(false) }) { Text(stringResource(R.string.onboarding_skip_add)) }
                    Button(onClick = { finish(true) }) { Text(stringResource(R.string.onboarding_add_med)) }
                }
                1 -> Button(
                    enabled = accepted,
                    onClick = { scope.launch { container.settings.setDisclaimerAccepted(); step++ } },
                ) { Text(stringResource(R.string.next)) }
                else -> Button(onClick = {
                    if (step == 0 && settings.language == null) {
                        val tag = LocaleHelper.appliedLanguage() ?: LocaleHelper.deviceDefault()
                        scope.launch { container.settings.setLanguage(tag); LocaleHelper.apply(tag) }
                    }
                    step++
                }) { Text(stringResource(R.string.next)) }
            }
        }
    }
}

@Composable
private fun StepHeader(icon: ImageVector, title: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(14.dp).size(32.dp))
    }
    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
}

@Composable
private fun WelcomeStep(current: String, onLanguage: (String) -> Unit) {
    StepHeader(Icons.Filled.Favorite, stringResource(R.string.onboarding_welcome_title))
    Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Text(stringResource(R.string.onboarding_welcome_body), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.size(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.choose_language), style = MaterialTheme.typography.titleMedium)
    }
    // Language names are always shown in their own language.
    LanguageButton("العربية", "ar", current == "ar", onLanguage)
    LanguageButton("English", "en", current == "en", onLanguage)
}

@Composable
private fun LanguageButton(label: String, tag: String, selected: Boolean, onLanguage: (String) -> Unit) {
    val modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    if (selected) {
        Button(onClick = { onLanguage(tag) }, modifier = modifier) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    } else {
        OutlinedButton(onClick = { onLanguage(tag) }, modifier = modifier) { Text(label, style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun DisclaimerStep(accepted: Boolean, onAccepted: (Boolean) -> Unit) {
    StepHeader(Icons.Filled.Favorite, stringResource(R.string.onboarding_purpose_title))
    Text(stringResource(R.string.onboarding_purpose_body), style = MaterialTheme.typography.bodyLarge)
    NoticeCard(stringResource(R.string.health_disclaimer), warning = true)
    Text(stringResource(R.string.onboarding_reminder_limits), style = MaterialTheme.typography.bodyMedium)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .toggleable(value = accepted, role = Role.Checkbox, onValueChange = onAccepted),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = accepted, onCheckedChange = null)
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.onboarding_i_understand), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PrivacyStep() {
    StepHeader(Icons.Filled.Lock, stringResource(R.string.onboarding_privacy_title))
    Text(stringResource(R.string.onboarding_privacy_body), style = MaterialTheme.typography.bodyLarge)
    listOf(R.string.privacy_point_local, R.string.privacy_point_no_account, R.string.privacy_point_no_tracking, R.string.privacy_point_backup, R.string.privacy_point_erase)
        .forEach { res ->
            Row {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(res), style = MaterialTheme.typography.bodyMedium)
            }
        }
}

@Composable
private fun RemindersStep(container: AppContainer) {
    val context = LocalContext.current
    val readiness = rememberReminderReadiness()
    var testScheduled by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* state re-read on resume */ }

    StepHeader(Icons.Filled.NotificationsActive, stringResource(R.string.onboarding_reminders_title))
    Text(stringResource(R.string.onboarding_reminders_body), style = MaterialTheme.typography.bodyLarge)

    if (readiness.notificationsAllowed) {
        NoticeCard(stringResource(R.string.check_notifications_ok))
    } else {
        NoticeCard(stringResource(R.string.check_notifications_off), warning = true) {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else SystemSettings.openNotificationSettings(context)
            }) { Text(stringResource(R.string.allow_notifications)) }
        }
    }
    if (readiness.exactAlarmsAllowed) {
        NoticeCard(stringResource(R.string.check_exact_ok))
    } else {
        NoticeCard(stringResource(R.string.check_exact_off), warning = true) {
            Button(onClick = { SystemSettings.openExactAlarmSettings(context) }) { Text(stringResource(R.string.open_alarm_settings)) }
        }
    }
    Text(stringResource(R.string.test_reminder_explain), style = MaterialTheme.typography.bodyMedium)
    OutlinedButton(
        enabled = readiness.notificationsAllowed,
        onClick = {
            container.notifier.ensureChannels(null)
            container.scheduler.scheduleTest(Instant.now().plusSeconds(60))
            testScheduled = true
        },
    ) { Text(stringResource(R.string.send_test_reminder)) }
    if (testScheduled) Text(stringResource(R.string.test_reminder_scheduled), color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun FirstMedicationStep() {
    StepHeader(Icons.Filled.CheckCircle, stringResource(R.string.onboarding_first_med_title))
    Text(stringResource(R.string.onboarding_first_med_body), style = MaterialTheme.typography.bodyLarge)
    NoticeCard(stringResource(R.string.dose_as_prescribed_hint))
}
