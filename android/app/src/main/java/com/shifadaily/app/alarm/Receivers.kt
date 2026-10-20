package com.shifadaily.app.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shifadaily.app.ShifaApp
import com.shifadaily.app.domain.dose.EventSource
import kotlinx.coroutines.launch

/** Runs [block] off the main thread while keeping the broadcast alive (goAsync). */
private fun BroadcastReceiver.runAsync(context: Context, block: suspend (ShifaApp) -> Unit) {
    val app = context.applicationContext as ShifaApp
    val pending = goAsync()
    app.container.appScope.launch {
        try {
            block(app)
        } finally {
            pending.finish()
        }
    }
}

/** Fires for the single rolling dose alarm and for the optional test reminder. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runAsync(context) { app ->
            val c = app.container
            when (intent.action) {
                ACTION_FIRE -> c.engine.reconcile("alarm")
                ACTION_TEST -> {
                    c.notifier.ensureChannels(c.settings.current().language)
                    c.notifier.showTest(c.settings.current().language)
                    c.engine.log("test", "delivered")
                }
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.shifadaily.app.action.FIRE"
        const val ACTION_TEST = "com.shifadaily.app.action.TEST"
    }
}

/** Taken / Snooze / Skip buttons on a reminder. Dismissing a notification records nothing. */
class DoseActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
        if (id <= 0) return
        runAsync(context) { app ->
            val engine = app.container.engine
            when (intent.action) {
                ACTION_TAKEN -> engine.markTaken(id, EventSource.NOTIFICATION)
                ACTION_SKIP -> engine.markSkipped(id, EventSource.NOTIFICATION)
                ACTION_SNOOZE -> engine.snooze(id, EventSource.NOTIFICATION)
            }
        }
    }

    companion object {
        const val ACTION_TAKEN = "com.shifadaily.app.action.TAKEN"
        const val ACTION_SNOOZE = "com.shifadaily.app.action.SNOOZE"
        const val ACTION_SKIP = "com.shifadaily.app.action.SKIP"
        const val EXTRA_RECORD_ID = "record_id"
    }
}

/**
 * Restores the rolling alarm after events that clear or invalidate it: reboot, app update,
 * clock or time-zone change, and exact-alarm permission grant.
 */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reason = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> "boot"
            Intent.ACTION_MY_PACKAGE_REPLACED -> "app-updated"
            Intent.ACTION_TIME_CHANGED -> "time-changed"
            Intent.ACTION_TIMEZONE_CHANGED -> "timezone-changed"
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> "exact-permission"
            else -> return
        }
        runAsync(context) { app -> app.container.engine.reconcile(reason) }
    }
}
