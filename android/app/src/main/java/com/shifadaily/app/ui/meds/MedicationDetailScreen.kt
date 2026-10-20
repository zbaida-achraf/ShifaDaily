package com.shifadaily.app.ui.meds

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.shifadaily.app.R
import com.shifadaily.app.data.db.DoseEventEntity
import com.shifadaily.app.data.db.DoseRecordEntity
import com.shifadaily.app.data.db.FoodInstruction
import com.shifadaily.app.domain.dose.DoseStatus
import com.shifadaily.app.domain.dose.EventSource
import com.shifadaily.app.ui.Routes
import com.shifadaily.app.ui.components.ChipKind
import com.shifadaily.app.ui.components.SectionTitle
import com.shifadaily.app.ui.components.ShifaCard
import com.shifadaily.app.ui.components.ShifaTopBar
import com.shifadaily.app.ui.components.StatusChip
import com.shifadaily.app.ui.components.chipKind
import com.shifadaily.app.ui.components.statusLabel
import com.shifadaily.app.util.Formats
import com.shifadaily.app.util.isolate
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicationDetailScreen(container: AppContainer, medId: Long, nav: NavHostController) {
    val med by container.medications.observe(medId).collectAsState(initial = null)
    val history by container.db.doseDao().observeForMedication(medId, 200).collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var deleteDialog by remember { mutableStateOf(false) }
    var correcting by remember { mutableStateOf<DoseRecordEntity?>(null) }

    Scaffold(
        topBar = {
            ShifaTopBar(med?.name?.let(::isolate) ?: "", onBack = { nav.popBackStack() }) {
                if (med?.archivedAt == null) {
                    IconButton(onClick = { nav.navigate(Routes.medEdit(medId)) }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_medication))
                    }
                }
            }
        },
    ) { padding ->
        val m = med ?: return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ShifaCard {
                    Text(isolate(m.doseLabel), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(formLabel(m.form)) + (m.formOther?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodyMedium)
                    Text(scheduleSummary(context, m), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.start_date_value, Formats.date(context, m.startDate)), style = MaterialTheme.typography.bodySmall)
                    if (m.foodInstruction != FoodInstruction.NONE) {
                        Text(
                            if (m.foodInstruction == FoodInstruction.CUSTOM) m.foodInstructionNote.orEmpty() else stringResource(foodLabel(m.foodInstruction)),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(stringResource(R.string.alert_value, stringResource(alertLabel(m.alertStyle))), style = MaterialTheme.typography.bodySmall)
                    m.instructions?.let { Text(stringResource(R.string.field_instructions) + ": " + it, style = MaterialTheme.typography.bodySmall) }
                    m.notes?.let { Text(stringResource(R.string.field_notes) + ": " + it, style = MaterialTheme.typography.bodySmall) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when {
                            m.archivedAt != null -> StatusChip(R.string.archived, ChipKind.NEUTRAL)
                            m.paused -> StatusChip(R.string.paused, ChipKind.WARN)
                            else -> StatusChip(R.string.active, ChipKind.GOOD)
                        }
                    }
                }
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (m.archivedAt == null) {
                        FilledTonalButton(onClick = { scope.launch { container.medications.setPaused(m.id, !m.paused) } }) {
                            Text(stringResource(if (m.paused) R.string.resume else R.string.pause))
                        }
                    } else {
                        FilledTonalButton(onClick = { scope.launch { container.medications.restore(m.id) } }) {
                            Text(stringResource(R.string.restore))
                        }
                    }
                    OutlinedButton(onClick = { deleteDialog = true }) {
                        Text(stringResource(R.string.delete_or_archive), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            item { SectionTitle(stringResource(R.string.dose_history)) }
            if (history.isEmpty()) {
                item { Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyMedium) }
            }
            items(history, key = { it.id }) { r -> HistoryRow(r, showName = false) { correcting = r } }
        }
    }

    if (deleteDialog) {
        val m = med
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    if (m?.archivedAt == null) {
                        TextButton(onClick = {
                            deleteDialog = false
                            scope.launch { container.medications.archive(medId); nav.popBackStack() }
                        }) { Text(stringResource(R.string.archive_keep_history)) }
                    }
                    TextButton(onClick = {
                        deleteDialog = false
                        scope.launch { container.medications.deleteWithHistory(medId); nav.popBackStack() }
                    }) { Text(stringResource(R.string.delete_with_history), color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { deleteDialog = false }) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }

    correcting?.let { r -> CorrectionDialog(container, r, onDismiss = { correcting = null }) }
}

@Composable
fun HistoryRow(r: DoseRecordEntity, showName: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    ShifaCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (showName) Text(isolate(r.medicationName), style = MaterialTheme.typography.titleSmall)
                Text(
                    Formats.date(context, r.scheduledDate) + " · " + Formats.time(context, r.scheduledTime),
                    style = MaterialTheme.typography.bodyMedium,
                )
                r.actionAt?.let {
                    val t = it.atZone(ZoneId.systemDefault())
                    Text(
                        stringResource(R.string.recorded_at, Formats.date(context, t.toLocalDate()) + " · " + Formats.time(context, t.toLocalTime())),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            StatusChip(if (r.status == DoseStatus.MISSED && !r.notified) R.string.status_not_delivered else statusLabel(r.status), chipKind(r.status))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CorrectionDialog(container: AppContainer, record: DoseRecordEntity, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val res = androidx.compose.ui.platform.LocalResources.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(record.status) }
    var events by remember { mutableStateOf<List<DoseEventEntity>>(emptyList()) }
    val initialTaken = record.actionAt?.atZone(ZoneId.systemDefault())?.toLocalTime() ?: record.scheduledTime
    val tp = rememberTimePickerState(initialTaken.hour, initialTaken.minute, DateFormat.is24HourFormat(context))
    LaunchedEffect(record.id) { events = container.db.doseDao().eventsFor(record.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.correct_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(isolate(record.medicationName) + " · " + isolate(record.doseLabel), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.scheduled_for, Formats.date(context, record.scheduledDate) + " · " + Formats.time(context, record.scheduledTime)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(DoseStatus.TAKEN, DoseStatus.SKIPPED, DoseStatus.MISSED).forEach { s ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(stringResource(statusLabel(s))) })
                    }
                }
                if (status == DoseStatus.TAKEN) {
                    Text(stringResource(R.string.taken_at_time), style = MaterialTheme.typography.labelLarge)
                    TimeInput(tp)
                }
                if (events.isNotEmpty()) {
                    HorizontalDivider()
                    Text(stringResource(R.string.audit_trail), style = MaterialTheme.typography.labelLarge)
                    events.forEach { e ->
                        val t = e.at.atZone(ZoneId.systemDefault())
                        Text(
                            Formats.date(context, t.toLocalDate()) + " " + Formats.time(context, t.toLocalTime()) + " · " +
                                (e.fromStatus?.let { res.getString(statusLabel(it)) + " → " } ?: "") +
                                res.getString(statusLabel(e.toStatus)) + " · " + res.getString(sourceLabel(e.source, e.correction)),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    val takenAt = if (status == DoseStatus.TAKEN) {
                        record.scheduledDate.atTime(LocalTime.of(tp.hour, tp.minute)).atZone(ZoneId.systemDefault()).toInstant()
                    } else null
                    container.engine.correct(record.id, status, takenAt)
                    onDismiss()
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private fun sourceLabel(s: EventSource, correction: Boolean) = when {
    correction -> R.string.source_correction
    s == EventSource.NOTIFICATION -> R.string.source_notification
    s == EventSource.APP -> R.string.source_app
    else -> R.string.source_system
}

@Composable
fun HistoryScreen(container: AppContainer, onBack: () -> Unit) {
    val records by container.db.doseDao().observeRecent(300).collectAsState(initial = emptyList())
    var correcting by remember { mutableStateOf<DoseRecordEntity?>(null) }
    Scaffold(topBar = { ShifaTopBar(stringResource(R.string.dose_history), onBack = onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text(stringResource(R.string.history_explain), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (records.isEmpty()) item { Text(stringResource(R.string.history_empty), modifier = Modifier.fillMaxWidth()) }
            items(records, key = { it.id }) { r -> HistoryRow(r, showName = true) { correcting = r } }
        }
    }
    correcting?.let { r -> CorrectionDialog(container, r, onDismiss = { correcting = null }) }
}
