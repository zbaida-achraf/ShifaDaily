package com.shifadaily.app.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.shifadaily.app.MainActivity
import com.shifadaily.app.R
import com.shifadaily.app.data.db.AlertStyle
import com.shifadaily.app.data.db.DoseRecordEntity
import com.shifadaily.app.data.db.FoodInstruction
import com.shifadaily.app.data.db.MedicationEntity
import com.shifadaily.app.util.Formats
import com.shifadaily.app.util.LocaleHelper
import com.shifadaily.app.util.isolate

/** Builds and posts reminder notifications. All text comes from string resources. */
class ReminderNotifier(private val appContext: Context) {

    private val manager = NotificationManagerCompat.from(appContext)

    fun canPost(): Boolean {
        val granted = android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return granted && manager.areNotificationsEnabled()
    }

    /** (Re)creates channels; names are refreshed in the current language. Sound/vibration are fixed per channel. */
    fun ensureChannels(lang: String?) {
        val ctx = LocaleHelper.localizedContext(appContext, lang)
        val nm = appContext.getSystemService(NotificationManager::class.java)
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val withSound = NotificationChannel(CH_SOUND, ctx.getString(R.string.channel_sound), NotificationManager.IMPORTANCE_HIGH).apply {
            description = ctx.getString(R.string.channel_sound_desc)
            setSound(sound, attrs)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 250, 400)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
        }
        val vibrate = NotificationChannel(CH_VIBRATE, ctx.getString(R.string.channel_vibrate), NotificationManager.IMPORTANCE_HIGH).apply {
            description = ctx.getString(R.string.channel_vibrate_desc)
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 250, 400)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
        }
        val silent = NotificationChannel(CH_SILENT, ctx.getString(R.string.channel_silent), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = ctx.getString(R.string.channel_silent_desc)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
        }
        val general = NotificationChannel(CH_GENERAL, ctx.getString(R.string.channel_general), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = ctx.getString(R.string.channel_general_desc)
        }
        nm.createNotificationChannels(listOf(withSound, vibrate, silent, general))
    }

    fun channelFor(style: AlertStyle) = when (style) {
        AlertStyle.SOUND -> CH_SOUND
        AlertStyle.VIBRATE -> CH_VIBRATE
        AlertStyle.SILENT -> CH_SILENT
    }

    fun showDose(record: DoseRecordEntity, med: MedicationEntity?, lang: String?, canSnooze: Boolean, snoozeMinutes: Int) {
        if (!canPost()) return
        val ctx = LocaleHelper.localizedContext(appContext, lang)
        val time = Formats.time(ctx, record.scheduledTime)
        val title = ctx.getString(R.string.notif_dose_title, isolate(record.medicationName))
        val lines = buildList {
            add(ctx.getString(R.string.notif_dose_line, isolate(record.doseLabel), isolate(time)))
            med?.let { foodText(ctx, it) }?.let { add(it) }
        }
        val text = lines.joinToString("\n")

        val publicVersion = NotificationCompat.Builder(ctx, channelFor(med?.alertStyle ?: AlertStyle.SOUND))
            .setSmallIcon(R.drawable.ic_stat_shifa)
            .setContentTitle(ctx.getString(R.string.notif_public_title))
            .setContentText(ctx.getString(R.string.notif_public_text))
            .build()

        val builder = NotificationCompat.Builder(ctx, channelFor(med?.alertStyle ?: AlertStyle.SOUND))
            .setSmallIcon(R.drawable.ic_stat_shifa)
            .setColor(ContextCompat.getColor(ctx, R.color.brand_primary))
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setWhen(record.scheduledAt.toEpochMilli())
            .setShowWhen(true)
            .setOnlyAlertOnce(false)
            .setAutoCancel(false)
            .setContentIntent(openAppIntent(record.id))
            // Explicit group + GROUP_ALERT_CHILDREN: verified on Android 16 that without it the system
            // auto-groups a second ShifaDaily notification and posts it silently (see docs/QA-REPORT.md).
            .setGroup(GROUP_DOSES)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .addAction(0, ctx.getString(R.string.action_taken), actionIntent(DoseActionReceiver.ACTION_TAKEN, record.id))
        if (canSnooze) {
            builder.addAction(0, ctx.getString(R.string.action_snooze_minutes, snoozeMinutes), actionIntent(DoseActionReceiver.ACTION_SNOOZE, record.id))
        }
        builder.addAction(0, ctx.getString(R.string.action_skip), actionIntent(DoseActionReceiver.ACTION_SKIP, record.id))
        post(notificationId(record.id), builder.build())
        postDoseSummary(ctx, med?.alertStyle ?: AlertStyle.SOUND)
    }

    /** Silent summary that holds the explicit dose group together. */
    private fun postDoseSummary(ctx: Context, style: AlertStyle) {
        val n = NotificationCompat.Builder(ctx, channelFor(style))
            .setSmallIcon(R.drawable.ic_stat_shifa)
            .setColor(ContextCompat.getColor(ctx, R.color.brand_primary))
            .setContentTitle(ctx.getString(R.string.notif_public_title))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setGroup(GROUP_DOSES)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setSilent(true)
            .setContentIntent(openAppIntent(null))
            .build()
        post(ID_DOSE_SUMMARY, n)
    }

    /** Removes the group summary once no dose reminder is visible any more. */
    private fun cleanUpSummary() {
        val nm = appContext.getSystemService(NotificationManager::class.java)
        val active = runCatching { nm.activeNotifications }.getOrDefault(emptyArray())
        val anyDose = active.any { it.notification.group == GROUP_DOSES && it.id != ID_DOSE_SUMMARY }
        if (!anyDose) manager.cancel(ID_DOSE_SUMMARY)
    }

    fun showMissedSummary(count: Int, lang: String?) {
        if (!canPost() || count <= 0) return
        val ctx = LocaleHelper.localizedContext(appContext, lang)
        val n = NotificationCompat.Builder(ctx, CH_GENERAL)
            .setSmallIcon(R.drawable.ic_stat_shifa)
            .setContentTitle(ctx.getString(R.string.notif_missed_title))
            .setContentText(ctx.resources.getQuantityString(R.plurals.notif_missed_text, count, count))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(openAppIntent(null))
            .setGroup(GROUP_OTHER)
            .setAutoCancel(true)
            .build()
        post(ID_MISSED_SUMMARY, n)
    }

    fun showTest(lang: String?) {
        if (!canPost()) return
        val ctx = LocaleHelper.localizedContext(appContext, lang)
        val n = NotificationCompat.Builder(ctx, CH_SOUND)
            .setSmallIcon(R.drawable.ic_stat_shifa)
            .setColor(ContextCompat.getColor(ctx, R.color.brand_primary))
            .setContentTitle(ctx.getString(R.string.notif_test_title))
            .setContentText(ctx.getString(R.string.notif_test_text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent(null))
            .setGroup(GROUP_OTHER)
            .setAutoCancel(true)
            .build()
        post(ID_TEST, n)
    }

    fun cancel(recordId: Long) {
        manager.cancel(notificationId(recordId))
        cleanUpSummary()
    }

    fun cancelAll() = manager.cancelAll()

    private fun post(id: Int, n: android.app.Notification) {
        try {
            manager.notify(id, n)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call; surfaced by the Reminder check screen.
        }
    }

    private fun foodText(ctx: Context, med: MedicationEntity): String? = when (med.foodInstruction) {
        FoodInstruction.NONE -> null
        FoodInstruction.BEFORE_FOOD -> ctx.getString(R.string.food_before)
        FoodInstruction.WITH_FOOD -> ctx.getString(R.string.food_with)
        FoodInstruction.AFTER_FOOD -> ctx.getString(R.string.food_after)
        FoodInstruction.EMPTY_STOMACH -> ctx.getString(R.string.food_empty)
        FoodInstruction.CUSTOM -> med.foodInstructionNote?.takeIf { it.isNotBlank() }
    }

    private fun openAppIntent(recordId: Long?): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (recordId != null) putExtra(MainActivity.EXTRA_DOSE_ID, recordId)
        }
        return PendingIntent.getActivity(
            appContext,
            (recordId ?: -1L).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionIntent(action: String, recordId: Long): PendingIntent {
        val intent = Intent(appContext, DoseActionReceiver::class.java)
            .setAction(action)
            .putExtra(DoseActionReceiver.EXTRA_RECORD_ID, recordId)
        // Unique request code per (record, action) so extras are never overwritten.
        val index = when (action) {
            DoseActionReceiver.ACTION_TAKEN -> 0
            DoseActionReceiver.ACTION_SNOOZE -> 1
            else -> 2
        }
        val code = (recordId * 4 + index).toInt()
        return PendingIntent.getBroadcast(appContext, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val CH_SOUND = "reminders_sound"
        const val CH_VIBRATE = "reminders_vibrate"
        const val CH_SILENT = "reminders_silent"
        const val CH_GENERAL = "general"
        private const val GROUP_DOSES = "com.shifadaily.app.DOSES"
        private const val GROUP_OTHER = "com.shifadaily.app.OTHER"
        private const val ID_DOSE_SUMMARY = Int.MAX_VALUE - 3
        private const val ID_MISSED_SUMMARY = Int.MAX_VALUE - 1
        private const val ID_TEST = Int.MAX_VALUE - 2

        fun notificationId(recordId: Long): Int = (recordId % (Int.MAX_VALUE - 10)).toInt()
    }
}
