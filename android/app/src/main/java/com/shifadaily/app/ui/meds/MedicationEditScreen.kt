package com.shifadaily.app.ui.meds

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shifadaily.app.AppContainer
import com.shifadaily.app.R
import com.shifadaily.app.data.db.AlertStyle
import com.shifadaily.app.data.db.FoodInstruction
import com.shifadaily.app.data.db.MedicationEntity
import com.shifadaily.app.data.db.MedicationForm
import com.shifadaily.app.domain.schedule.MedicationError
import com.shifadaily.app.domain.schedule.ScheduleSpec
import com.shifadaily.app.domain.schedule.ScheduleType
import com.shifadaily.app.domain.schedule.ScheduleValidator
import com.shifadaily.app.ui.components.NoticeCard
import com.shifadaily.app.ui.components.SectionTitle
import com.shifadaily.app.ui.components.ShifaTopBar
import com.shifadaily.app.util.Formats
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Form state kept in a ViewModel so it survives rotation and language changes. */
class MedEditState : ViewModel() {
    var loadedId: Long? = null
    var original: MedicationEntity? = null
    var name by mutableStateOf("")
    var doseAmount by mutableStateOf("")
    var doseUnit by mutableStateOf("")
    var form by mutableStateOf(MedicationForm.TABLET)
    var formOther by mutableStateOf("")
    var notes by mutableStateOf("")
    var instructions by mutableStateOf("")
    var food by mutableStateOf(FoodInstruction.NONE)
    var foodNote by mutableStateOf("")
    var type by mutableStateOf(ScheduleType.DAILY)
    var startDate by mutableStateOf(LocalDate.now())
    var endDate by mutableStateOf<LocalDate?>(null)
    val times = mutableStateListOf(LocalTime.of(8, 0))
    val weekdays = mutableStateListOf<DayOfWeek>()
    var interval by androidx.compose.runtime.mutableIntStateOf(2)
    var alert by mutableStateOf(AlertStyle.SOUND)
    var errors by mutableStateOf<List<MedicationError>>(emptyList())

    fun load(m: MedicationEntity) {
        loadedId = m.id; original = m
        name = m.name; doseAmount = m.doseAmount; doseUnit = m.doseUnit
        form = m.form; formOther = m.formOther.orEmpty(); notes = m.notes.orEmpty(); instructions = m.instructions.orEmpty()
        food = m.foodInstruction; foodNote = m.foodInstructionNote.orEmpty()
        type = m.scheduleType; startDate = m.startDate; endDate = m.endDate
        times.clear(); times.addAll(m.times)
        weekdays.clear(); weekdays.addAll(m.weekdays)
        interval = m.intervalDays.coerceAtLeast(2); alert = m.alertStyle
    }

    fun spec() = ScheduleSpec(
        type, startDate, endDate, times.toList(),
        if (type == ScheduleType.WEEKDAYS) weekdays.toSet() else emptySet(),
        if (type == ScheduleType.INTERVAL) interval else 1,
    )

    fun toEntity(): MedicationEntity {
        val now = Instant.now()
        val base = original
        return MedicationEntity(
            id = base?.id ?: 0,
            name = name.trim(),
            doseAmount = doseAmount.trim(),
            doseUnit = doseUnit.trim(),
            form = form,
            formOther = formOther.trim().takeIf { form == MedicationForm.OTHER && it.isNotEmpty() },
            notes = notes.trim().ifEmpty { null },
            instructions = instructions.trim().ifEmpty { null },
            foodInstruction = food,
            foodInstructionNote = foodNote.trim().takeIf { food == FoodInstruction.CUSTOM && it.isNotEmpty() },
            scheduleType = type,
            startDate = startDate,
            endDate = endDate,
            times = times.sorted(),
            weekdays = if (type == ScheduleType.WEEKDAYS) weekdays.toSet() else emptySet(),
            intervalDays = if (type == ScheduleType.INTERVAL) interval else 1,
            paused = base?.paused ?: false,
            alertStyle = alert,
            effectiveFrom = base?.effectiveFrom ?: now,
            createdAt = base?.createdAt ?: now,
            updatedAt = now,
            archivedAt = base?.archivedAt,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MedicationEditScreen(container: AppContainer, medId: Long?, onDone: () -> Unit) {
    val st: MedEditState = viewModel(key = "med-edit-${medId ?: "new"}") { MedEditState() }
    val context = LocalContext.current
    val res = androidx.compose.ui.platform.LocalResources.current
    val scope = rememberCoroutineScope()
    var showTimePicker by remember { mutableStateOf<Int?>(null) } // index, -1 = new
    var datePicker by remember { mutableStateOf<String?>(null) } // "start" / "end"
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(medId) {
        if (medId != null && st.loadedId != medId) container.medications.get(medId)?.let { st.load(it) }
    }

    Scaffold(
        topBar = { ShifaTopBar(stringResource(if (medId == null) R.string.add_medication else R.string.edit_medication), onBack = onDone) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NoticeCard(stringResource(R.string.dose_as_prescribed_hint))

            OutlinedTextField(
                value = st.name, onValueChange = { st.name = it.take(ScheduleValidator.MAX_NAME + 5) },
                label = { Text(stringResource(R.string.field_name)) },
                isError = MedicationError.NAME_REQUIRED in st.errors || MedicationError.NAME_TOO_LONG in st.errors,
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = st.doseAmount, onValueChange = { st.doseAmount = it.take(20) },
                    label = { Text(stringResource(R.string.field_dose)) },
                    placeholder = { Text(stringResource(R.string.field_dose_placeholder)) },
                    isError = MedicationError.DOSE_REQUIRED in st.errors,
                    singleLine = true, modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = st.doseUnit, onValueChange = { st.doseUnit = it.take(20) },
                    label = { Text(stringResource(R.string.field_unit)) },
                    placeholder = { Text(stringResource(R.string.field_unit_placeholder)) },
                    singleLine = true, modifier = Modifier.weight(1f),
                )
            }

            SectionTitle(stringResource(R.string.field_form))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MedicationForm.entries.forEach { f ->
                    FilterChip(selected = st.form == f, onClick = { st.form = f }, label = { Text(stringResource(formLabel(f))) })
                }
            }
            if (st.form == MedicationForm.OTHER) {
                OutlinedTextField(value = st.formOther, onValueChange = { st.formOther = it.take(40) },
                    label = { Text(stringResource(R.string.field_form_other)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }

            SectionTitle(stringResource(R.string.field_schedule))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScheduleType.entries.forEach { t ->
                    FilterChip(selected = st.type == t, onClick = { st.type = t }, label = { Text(stringResource(scheduleTypeLabel(t))) })
                }
            }
            when (st.type) {
                ScheduleType.WEEKDAYS -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    orderedWeekdays(context).forEach { d ->
                        val sel = d in st.weekdays
                        FilterChip(
                            selected = sel,
                            onClick = { if (sel) st.weekdays.remove(d) else st.weekdays.add(d) },
                            label = { Text(Formats.shortDay(context, d)) },
                        )
                    }
                }
                ScheduleType.INTERVAL -> Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { st.interval = (st.interval - 1).coerceAtLeast(2) }) {
                        Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.cd_decrease))
                    }
                    Text(
                        res.getQuantityString(R.plurals.every_n_days, st.interval, st.interval),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(onClick = { st.interval = (st.interval + 1).coerceAtMost(ScheduleValidator.MAX_INTERVAL) }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_increase))
                    }
                }
                ScheduleType.DAILY -> Unit
            }

            SectionTitle(stringResource(R.string.field_times))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                st.times.sorted().forEach { t ->
                    val label = Formats.time(context, t)
                    InputChip(
                        selected = false,
                        onClick = { showTimePicker = st.times.indexOf(t) },
                        label = { Text(label) },
                        trailingIcon = {
                            IconButton(onClick = { st.times.remove(t) }, modifier = Modifier.semantics {
                                contentDescription = res.getString(R.string.cd_remove_time, label)
                            }) { Icon(Icons.Filled.Close, contentDescription = null) }
                        },
                    )
                }
                OutlinedButton(onClick = { showTimePicker = -1 }) {
                    Icon(Icons.Filled.Add, contentDescription = null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.add_time))
                }
            }

            SectionTitle(stringResource(R.string.field_dates))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { datePicker = "start" }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.start_date_value, Formats.date(context, st.startDate)))
                }
                OutlinedButton(onClick = { datePicker = "end" }, modifier = Modifier.weight(1f)) {
                    Text(st.endDate?.let { stringResource(R.string.end_date_value, Formats.date(context, it)) } ?: stringResource(R.string.no_end_date))
                }
            }
            if (st.endDate != null) TextButton(onClick = { st.endDate = null }) { Text(stringResource(R.string.remove_end_date)) }

            SectionTitle(stringResource(R.string.field_food))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FoodInstruction.entries.forEach { f ->
                    FilterChip(selected = st.food == f, onClick = { st.food = f }, label = { Text(stringResource(foodLabel(f))) })
                }
            }
            if (st.food == FoodInstruction.CUSTOM) {
                OutlinedTextField(value = st.foodNote, onValueChange = { st.foodNote = it.take(120) },
                    label = { Text(stringResource(R.string.field_food_custom)) }, modifier = Modifier.fillMaxWidth())
            }

            SectionTitle(stringResource(R.string.field_alert))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AlertStyle.entries.forEach { a ->
                    FilterChip(selected = st.alert == a, onClick = { st.alert = a }, label = { Text(stringResource(alertLabel(a))) })
                }
            }
            Text(stringResource(R.string.alert_style_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(value = st.instructions, onValueChange = { st.instructions = it.take(500) },
                label = { Text(stringResource(R.string.field_instructions)) }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = st.notes, onValueChange = { st.notes = it.take(500) },
                label = { Text(stringResource(R.string.field_notes)) }, minLines = 2, modifier = Modifier.fillMaxWidth())

            if (st.errors.isNotEmpty()) {
                NoticeCard(st.errors.joinToString("\n") { res.getString(errorLabel(it)) }, warning = true)
            }

            Button(
                enabled = !saving,
                onClick = {
                    val errors = ScheduleValidator.validate(st.name, st.doseAmount, st.spec())
                    st.errors = errors
                    if (errors.isEmpty()) {
                        saving = true
                        scope.launch {
                            val e = st.toEntity()
                            if (e.id == 0L) container.medications.create(e) else container.medications.update(e)
                            onDone()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save)) }
        }
    }

    showTimePicker?.let { idx ->
        val initial = if (idx >= 0) st.times.getOrNull(idx) ?: LocalTime.of(8, 0) else LocalTime.of(8, 0)
        val tp = rememberTimePickerState(initial.hour, initial.minute, DateFormat.is24HourFormat(context))
        AlertDialog(
            onDismissRequest = { showTimePicker = null },
            title = { Text(stringResource(R.string.pick_time)) },
            text = { TimePicker(tp) },
            confirmButton = {
                TextButton(onClick = {
                    val t = LocalTime.of(tp.hour, tp.minute)
                    if (idx >= 0 && idx < st.times.size) st.times[idx] = t else st.times.add(t)
                    // Duplicates are reported by validation rather than silently merged.
                    showTimePicker = null
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    datePicker?.let { which ->
        val initial = if (which == "start") st.startDate else st.endDate ?: st.startDate
        val dp = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { datePicker = null },
            confirmButton = {
                TextButton(onClick = {
                    dp.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        if (which == "start") st.startDate = d else st.endDate = d
                    }
                    datePicker = null
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { datePicker = null }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(dp) }
    }
}
