package com.shifadaily.app

import android.app.Application
import android.os.Build
import com.shifadaily.app.alarm.AlarmScheduler
import com.shifadaily.app.alarm.ReminderEngine
import com.shifadaily.app.alarm.ReminderNotifier
import com.shifadaily.app.data.DataManager
import com.shifadaily.app.data.MealRepository
import com.shifadaily.app.data.MedicationRepository
import com.shifadaily.app.data.db.AppDatabase
import com.shifadaily.app.data.prefs.SettingsRepository
import com.shifadaily.app.util.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** Manual dependency container: small app, no DI framework needed. */
class AppContainer(app: Application) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val db = AppDatabase.build(app)
    val settings = SettingsRepository(app)
    val scheduler = AlarmScheduler(app)
    val notifier = ReminderNotifier(app)
    val engine = ReminderEngine(db, settings, scheduler, notifier)
    val medications = MedicationRepository(db, engine)
    val meals = MealRepository(db)
    val data = DataManager(app, db, settings, notifier, scheduler)
}

class ShifaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applyStoredLanguage()
        container.appScope.launch { container.engine.reconcile("app-start") }
    }

    /**
     * The chosen language is stored in DataStore. On Android 13+ the user can also change the
     * per-app language in system settings; in that case the system choice wins and is synced back.
     */
    private fun applyStoredLanguage() {
        val stored = runBlocking { container.settings.current().language }
        val applied = LocaleHelper.appliedLanguage()
        if (Build.VERSION.SDK_INT >= 33 && applied != null) {
            if (applied != stored) runBlocking { container.settings.setLanguage(applied) }
        } else if (stored != null && stored != applied) {
            LocaleHelper.apply(stored)
        }
    }
}
