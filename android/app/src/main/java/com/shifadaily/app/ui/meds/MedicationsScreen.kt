package com.shifadaily.app.ui.meds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.shifadaily.app.AppContainer
import com.shifadaily.app.R
import com.shifadaily.app.data.db.MedicationEntity
import com.shifadaily.app.ui.Routes
import com.shifadaily.app.ui.components.ChipKind
import com.shifadaily.app.ui.components.EmptyState
import com.shifadaily.app.ui.components.SectionTitle
import com.shifadaily.app.ui.components.ShifaCard
import com.shifadaily.app.ui.components.ShifaTopBar
import com.shifadaily.app.ui.components.StatusChip
import com.shifadaily.app.util.isolate

@Composable
fun MedicationsScreen(container: AppContainer, nav: NavHostController) {
    val meds by container.medications.observeCurrent().collectAsState(initial = null)
    val archived by container.medications.observeArchived().collectAsState(initial = emptyList())
    var showArchived by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            ShifaTopBar(stringResource(R.string.nav_medications)) {
                IconButton(onClick = { nav.navigate(Routes.HISTORY) }) {
                    Icon(Icons.Filled.History, contentDescription = stringResource(R.string.view_history))
                }
            }
        },
        floatingActionButton = {
            val addLabel = stringResource(R.string.add_medication)
            ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.medEdit()) },
                // Explicit label: emulator QA showed the FAB text was not exposed to accessibility services.
                modifier = Modifier.semantics { contentDescription = addLabel },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(addLabel) },
            )
        },
    ) { padding ->
        val list = meds ?: return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (list.isEmpty()) {
                item {
                    EmptyState(Icons.Filled.Medication, stringResource(R.string.meds_empty_title), stringResource(R.string.meds_empty_body)) {
                        Button(onClick = { nav.navigate(Routes.medEdit()) }) { Text(stringResource(R.string.add_medication)) }
                    }
                }
            }
            items(list, key = { it.id }) { med -> MedRow(med) { nav.navigate(Routes.medDetail(med.id)) } }
            if (archived.isNotEmpty()) {
                item {
                    TextButton(onClick = { showArchived = !showArchived }) {
                        Text(stringResource(if (showArchived) R.string.hide_archived else R.string.show_archived, archived.size))
                    }
                }
                if (showArchived) {
                    item { SectionTitle(stringResource(R.string.archived)) }
                    items(archived, key = { "a${it.id}" }) { med -> MedRow(med) { nav.navigate(Routes.medDetail(med.id)) } }
                }
            }
        }
    }
}

@Composable
private fun MedRow(med: MedicationEntity, onClick: () -> Unit) {
    val context = LocalContext.current
    ShifaCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(isolate(med.name), style = MaterialTheme.typography.titleMedium)
                Text(isolate(med.doseLabel), style = MaterialTheme.typography.bodyMedium)
                Text(scheduleSummary(context, med), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when {
                med.archivedAt != null -> StatusChip(R.string.archived, ChipKind.NEUTRAL)
                med.paused -> StatusChip(R.string.paused, ChipKind.WARN)
            }
        }
    }
}
