package com.shifadaily.app.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.core.net.toUri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.shifadaily.app.alarm.ReminderNotifier

data class ReminderReadiness(
    val notificationsAllowed: Boolean,
    /** Always true below Android 12. */
    val exactAlarmsAllowed: Boolean,
    /** The "with sound" channel exists, is enabled and has a sound. Null if not created yet. */
    val soundChannelOk: Boolean?,
    val ringerMode: Int,
    val notificationVolume: Int,
    val notificationVolumeMax: Int,
    val alarmVolume: Int,
    val dndActive: Boolean,
    /** True when Android battery optimisation applies to the app (the default). */
    val batteryOptimized: Boolean,
) {
    val essentialOk get() = notificationsAllowed && exactAlarmsAllowed

    companion object {
        fun read(context: Context): ReminderReadiness {
            val nmCompat = NotificationManagerCompat.from(context)
            val permission = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            val am = context.getSystemService(AlarmManager::class.java)
            val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
            val nm = context.getSystemService(NotificationManager::class.java)
            val ch = nm.getNotificationChannel(ReminderNotifier.CH_SOUND)
            val soundOk = ch?.let { it.importance >= NotificationManager.IMPORTANCE_DEFAULT && it.sound != null }
            val audio = context.getSystemService(AudioManager::class.java)
            val pm = context.getSystemService(PowerManager::class.java)
            return ReminderReadiness(
                notificationsAllowed = permission && nmCompat.areNotificationsEnabled(),
                exactAlarmsAllowed = exact,
                soundChannelOk = soundOk,
                ringerMode = audio.ringerMode,
                notificationVolume = audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION),
                notificationVolumeMax = audio.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION),
                alarmVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM),
                dndActive = nm.currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL,
                batteryOptimized = !pm.isIgnoringBatteryOptimizations(context.packageName),
            )
        }
    }
}

/** Re-reads readiness every time the screen resumes (user may come back from system settings). */
@Composable
fun rememberReminderReadiness(): ReminderReadiness {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var state by remember { mutableStateOf(ReminderReadiness.read(context)) }
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) state = ReminderReadiness.read(context) }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return state
}

/** Intents to the relevant system settings screens. Nothing is changed silently. */
object SystemSettings {
    fun openNotificationSettings(context: Context) = start(
        context,
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
    )

    fun openChannelSettings(context: Context, channel: String) = start(
        context,
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, channel),
    )

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= 31) {
            start(context, Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()))
        }
    }

    /** Opens the battery-optimisation list; we do not request the exemption directly. */
    fun openBatterySettings(context: Context) = start(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

    fun openAppDetails(context: Context) =
        start(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))

    private fun start(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: RuntimeException) {
            // ActivityNotFound / SecurityException on some OEM builds: fall back once to app details.
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}
