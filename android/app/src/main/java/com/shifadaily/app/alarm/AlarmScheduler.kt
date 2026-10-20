package com.shifadaily.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.Instant

enum class AlarmMode { EXACT, INEXACT, NONE, FAILED }

/**
 * Owns the single rolling OS alarm (see ADR-001). Request code 0 is the dose alarm,
 * request code 1 the optional "test reminder".
 */
class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun scheduleNext(at: Instant): AlarmMode = set(REQ_DOSE, AlarmReceiver.ACTION_FIRE, at)

    fun scheduleTest(at: Instant): AlarmMode = set(REQ_TEST, AlarmReceiver.ACTION_TEST, at)

    fun cancelNext() {
        alarmManager.cancel(pendingIntent(REQ_DOSE, AlarmReceiver.ACTION_FIRE))
    }

    private fun set(requestCode: Int, action: String, at: Instant): AlarmMode {
        val pi = pendingIntent(requestCode, action)
        val trigger = at.toEpochMilli()
        return try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
                AlarmMode.EXACT
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
                AlarmMode.INEXACT
            }
        } catch (e: SecurityException) {
            // Exact access revoked between the check and the call: fall back to inexact.
            runCatching { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi) }
                .fold({ AlarmMode.INEXACT }, { AlarmMode.FAILED })
        }
    }

    private fun pendingIntent(requestCode: Int, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        private const val REQ_DOSE = 0
        private const val REQ_TEST = 1
    }
}
