package com.shifadaily.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.shifadaily.app.data.prefs.ThemeMode
import com.shifadaily.app.ui.ShifaNavHost
import com.shifadaily.app.ui.theme.ShifaTheme
import kotlinx.coroutines.launch

// AppCompatActivity is required for AppCompat per-app language switching on API < 33.
class MainActivity : AppCompatActivity() {

    /** Dose to open from a tapped reminder notification. */
    private val pendingDoseId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        readDose(intent)
        val container = (application as ShifaApp).container
        setContent {
            val settings by container.settings.settings.collectAsState(initial = null)
            ShifaTheme(settings?.theme ?: ThemeMode.SYSTEM) {
                ShifaNavHost(
                    container = container,
                    settings = settings,
                    pendingDoseId = pendingDoseId.value,
                    onDoseHandled = { pendingDoseId.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readDose(intent)
    }

    override fun onResume() {
        super.onResume()
        // Permissions may have changed in system settings; re-check and re-arm.
        val c = (application as ShifaApp).container
        c.appScope.launch { c.engine.reconcile("resume") }
    }

    private fun readDose(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_DOSE_ID, -1L) ?: -1L
        if (id > 0) pendingDoseId.value = id
    }

    companion object {
        const val EXTRA_DOSE_ID = "dose_id"
    }
}
