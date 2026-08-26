package com.goodusestudios.pressbench.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SafetyCheck
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goodusestudios.pressbench.PressBenchViewModel
import com.goodusestudios.pressbench.model.Peel
import com.goodusestudios.pressbench.model.Pressure
import com.goodusestudios.pressbench.model.RunRecord
import com.goodusestudios.pressbench.model.productionCounts
import com.goodusestudios.pressbench.model.Setup
import com.goodusestudios.pressbench.model.SetupStatus
import com.goodusestudios.pressbench.model.TemperatureUnit
import com.goodusestudios.pressbench.ui.theme.LocalPressBenchPalette

sealed interface PressBenchSheet {
    data object SetupPicker : PressBenchSheet
    data class StartRun(val setupId: String) : PressBenchSheet
    data class SetupDetail(val setupId: String) : PressBenchSheet
    data object NewSetup : PressBenchSheet
    data object NewMachine : PressBenchSheet
    data object Settings : PressBenchSheet
    data object Language : PressBenchSheet
    data object DeleteLocal : PressBenchSheet
    data class History(val recordId: String) : PressBenchSheet
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PressBenchBottomSheet(
    sheet: PressBenchSheet,
    viewModel: PressBenchViewModel,
    t: Translator,
    onReplace: (PressBenchSheet) -> Unit,
    onDismiss: () -> Unit,
    onOpenLegal: (String) -> Unit,
    onSupport: () -> Unit,
    onMessage: (String) -> Unit,
    onPrivacyChoices: () -> Unit,
    privacyOptionsRequired: Boolean,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            val base = Modifier.fillMaxWidth().widthIn(max = 720.dp).imePadding().padding(horizontal = 16.dp, vertical = 4.dp)
            val bodyModifier = if (sheet == PressBenchSheet.SetupPicker || sheet == PressBenchSheet.Language) base
            else base.verticalScroll(rememberScrollState())
            Column(bodyModifier) {
                when (sheet) {
                    PressBenchSheet.SetupPicker -> SetupPickerSheet(viewModel, t) { onReplace(PressBenchSheet.StartRun(it.id)) }
                    is PressBenchSheet.StartRun -> viewModel.setup(sheet.setupId)?.let { StartRunSheet(it, viewModel, t, onDismiss) }
                    is PressBenchSheet.SetupDetail -> viewModel.setup(sheet.setupId)?.let {
                        SetupDetailSheet(it, viewModel, t, onStart = { onReplace(PressBenchSheet.StartRun(it.id)) }, onDismiss)
                    }
                    PressBenchSheet.NewSetup -> NewSetupSheet(viewModel, t, onDismiss)
                    PressBenchSheet.NewMachine -> NewMachineSheet(viewModel, t, onDismiss)
                    PressBenchSheet.Settings -> SettingsSheet(viewModel, t, onReplace, onOpenLegal, onSupport, onPrivacyChoices, privacyOptionsRequired)
                    PressBenchSheet.Language -> LanguageSheet(viewModel, t, onDismiss)
                    PressBenchSheet.DeleteLocal -> DeleteLocalSheet(viewModel, t, onDismiss)
                    is PressBenchSheet.History -> viewModel.state.history.find { it.id == sheet.recordId }?.let { HistorySheet(it, viewModel, t, onMessage, onDismiss) }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun NewMachineSheet(viewModel: PressBenchViewModel, t: Translator, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var detail by rememberSaveable { mutableStateOf("") }
    val duplicate = viewModel.state.machines.any { it.name.equals(name.trim(), ignoreCase = true) }
    SheetTitle(t("machines.add"))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PressMachineIcon(Modifier.size(88.dp).align(Alignment.CenterHorizontally))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(t("machines.name")) },
            singleLine = true,
            isError = duplicate,
        )
        OutlinedTextField(
            value = detail,
            onValueChange = { detail = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(t("machines.details")) },
            singleLine = true,
        )
        PrimaryAction(
            text = t("common.save"),
            onClick = {
                if (viewModel.addMachine(name, detail)) onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank() && detail.isNotBlank() && !duplicate,
        )
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(t("common.cancel")) }
    }
}

@Composable
private fun SheetTitle(title: String, subtitle: String? = null) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = LocalPressBenchPalette.current.ink2) }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SetupPickerSheet(viewModel: PressBenchViewModel, t: Translator, onSetup: (Setup) -> Unit) {
    SheetTitle(t("setups.title"))
    LazyColumn(Modifier.heightIn(max = 600.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(viewModel.state.setups, key = { it.id }) { setup ->
            SetupCard(setup, viewModel.state.preferences.temperatureUnit, viewModel.state.preferences.locale, t, onClick = { onSetup(setup) })
        }
    }
}

@Composable
private fun StartRunSheet(setup: Setup, viewModel: PressBenchViewModel, t: Translator, onDismiss: () -> Unit) {
    val defaultQuantity = if (viewModel.state.lastStart.setupId == setup.id) viewModel.state.lastStart.quantity
    else viewModel.state.history.firstOrNull { it.setupId == setup.id }?.processed ?: 24
    var job by rememberSaveable(setup.id) { mutableStateOf("") }
    var quantity by rememberSaveable(setup.id) { mutableStateOf(defaultQuantity.toString()) }
    SheetTitle(t("home.startRun.title"), "${localTitle(setup, t)} · ${setup.machine}")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FactGrid(setup, viewModel.state.preferences.temperatureUnit, viewModel.state.preferences.locale, t)
        RecipeStrip(setup, t = t)
        OutlinedTextField(job, { job = it }, Modifier.fillMaxWidth(), label = { Text(t("run.jobReference")) }, singleLine = true)
        OutlinedTextField(
            quantity, { quantity = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(),
            label = { Text(t("run.plannedQuantity")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
        )
        val value = quantity.toIntOrNull()
        PrimaryAction(
            t("common.continue"),
            { viewModel.startRun(setup.id, value ?: 1, job); onDismiss() },
            Modifier.fillMaxWidth(), Icons.Filled.PlayArrow,
            enabled = value != null && value in 1..999_999,
        )
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(t("common.cancel")) }
    }
}

@Composable
private fun SetupDetailSheet(
    setup: Setup,
    viewModel: PressBenchViewModel,
    t: Translator,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    SheetTitle(localTitle(setup, t))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { SetupArtwork(setup, Modifier.size(120.dp)) }
        FactGrid(setup, viewModel.state.preferences.temperatureUnit, viewModel.state.preferences.locale, t)
        RecipeStrip(setup, t = t)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProofCard(t("status.${setup.status.name.lowercase()}"), setup.machine, Modifier.weight(1f))
            ProofCard(formatNumber(setup.provenRuns, viewModel.state.preferences.locale), t("home.metric.batches"), Modifier.weight(1f))
            ProofCard(formatNumber(setup.provenPieces, viewModel.state.preferences.locale), t("report.unitsProcessed"), Modifier.weight(1f))
        }
        if (setup.checkedAt != null && localSource(setup, t).isNotBlank()) {
            NativeSurface {
                Column(Modifier.padding(12.dp)) {
                    Text(t("report.instructionSource"), style = MaterialTheme.typography.labelLarge, color = LocalPressBenchPalette.current.success)
                    Text("${localSource(setup, t)} · ${formatShortDate(setup.checkedAt, viewModel.state.preferences.locale)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        PrimaryAction(t("home.startRun.title"), onStart, Modifier.fillMaxWidth(), Icons.Filled.PlayArrow)
        OutlineAction(t("setup.duplicate"), { viewModel.duplicateSetup(setup.id); onDismiss() }, Modifier.fillMaxWidth(), Icons.Filled.ContentCopy)
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(t("common.cancel")) }
    }
}

@Composable
private fun ProofCard(value: String, label: String, modifier: Modifier) {
    val palette = LocalPressBenchPalette.current
    NativeSurface(modifier) {
        Column(Modifier.heightIn(min = 72.dp).padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelSmall, color = palette.ink2, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun NewSetupSheet(viewModel: PressBenchViewModel, t: Translator, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var material by rememberSaveable { mutableStateOf("") }
    var temperature by rememberSaveable { mutableStateOf(if (viewModel.state.preferences.temperatureUnit == TemperatureUnit.C) "149°C" else "300°F") }
    var seconds by rememberSaveable { mutableStateOf("12") }
    var pressure by rememberSaveable { mutableStateOf(Pressure.MEDIUM) }
    var prepress by rememberSaveable { mutableStateOf("3") }
    var peel by rememberSaveable { mutableStateOf(Peel.WARM) }
    var postpress by rememberSaveable { mutableStateOf("5") }
    var machine by rememberSaveable { mutableStateOf("") }
    SheetTitle(t("setup.title"))
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(t("common.name")) }, singleLine = true)
        OutlinedTextField(material, { material = it }, Modifier.fillMaxWidth(), label = { Text(t("common.material")) }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(temperature, { temperature = it }, Modifier.weight(1f), label = { Text(t("common.temperature")) }, singleLine = true)
            OutlinedTextField(seconds, { seconds = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text(t("common.durationSeconds")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        }
        EnumDropdown(t("common.pressure"), Pressure.entries, pressure, { pressure = it }) { t("pressure.${it.name.lowercase()}") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(prepress, { prepress = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text(t("stage.prepress")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            EnumDropdown(t("stage.peel"), Peel.entries, peel, { peel = it }, Modifier.weight(1f)) { t("peel.${it.name.lowercase()}") }
        }
        OutlinedTextField(postpress, { postpress = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("${t("stage.press")} 2") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        OutlinedTextField(machine, { machine = it }, Modifier.fillMaxWidth(), label = { Text(t("run.machine")) }, singleLine = true)
        val duration = seconds.toIntOrNull()
        val selectedMachine = machine.trim().ifBlank { viewModel.state.machines.firstOrNull()?.name.orEmpty() }
        PrimaryAction(
            t("common.save"),
            {
                viewModel.addSetup(
                    Setup(
                        id = "", title = name.ifBlank { t("setup.title") }, material = material.trim(), transfer = "Transfer",
                        machine = selectedMachine, temperature = temperature,
                        seconds = duration ?: 12, pressure = pressure, status = SetupStatus.TRIAL,
                        prepressSeconds = prepress.toIntOrNull() ?: 0, peel = peel, postpressSeconds = postpress.toIntOrNull() ?: 0,
                    ),
                )
                onDismiss()
            },
            Modifier.fillMaxWidth(), enabled = material.isNotBlank() && selectedMachine.isNotBlank() && duration != null && duration > 0,
        )
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(t("common.cancel")) }
    }
}

@Composable
private fun <T> SelectorRow(
    values: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { value ->
            Button(
                onClick = { onSelect(value) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(13.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 5.dp, vertical = 5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (value == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (value == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) { Text(label(value), style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center) }
        }
    }
}

@Composable
private fun <T> EnumDropdown(
    fieldLabel: String,
    values: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(13.dp)) {
            Column(Modifier.weight(1f)) {
                Text(fieldLabel, style = MaterialTheme.typography.labelSmall, color = LocalPressBenchPalette.current.ink2)
                Text(label(selected), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.ArrowDropDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { value ->
                DropdownMenuItem(
                    text = { Text(label(value)) },
                    onClick = { onSelect(value); expanded = false },
                    trailingIcon = { if (value == selected) Icon(Icons.Filled.Check, null) },
                )
            }
        }
    }
}

@Composable
private fun SettingsSheet(
    viewModel: PressBenchViewModel,
    t: Translator,
    onReplace: (PressBenchSheet) -> Unit,
    onOpenLegal: (String) -> Unit,
    onSupport: () -> Unit,
    onPrivacyChoices: () -> Unit,
    privacyOptionsRequired: Boolean,
) {
    val state = viewModel.state
    SheetTitle(t("settings.title"))
    SettingsGroup(t("settings.title")) {
        ClickableRow(t("common.language"), viewModel.strings.displayLanguage(state.preferences.locale, state.preferences.locale), Icons.Filled.Language, { onReplace(PressBenchSheet.Language) })
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        ClickableRow(
            t("settings.sound"), icon = Icons.Filled.VolumeUp,
            onClick = { viewModel.setSound(!state.preferences.soundEnabled) },
            trailing = { Switch(state.preferences.soundEnabled, onCheckedChange = null) },
            toggleValue = state.preferences.soundEnabled,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        ClickableRow(
            t("settings.vibration"), icon = Icons.Filled.Vibration,
            onClick = { viewModel.setVibration(!state.preferences.vibrationEnabled) },
            trailing = { Switch(state.preferences.vibrationEnabled, onCheckedChange = null) },
            toggleValue = state.preferences.vibrationEnabled,
        )
    }
    Spacer(Modifier.height(12.dp))
    Text(t("settings.temperatureUnit"), style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(7.dp))
    SelectorRow(TemperatureUnit.entries, state.preferences.temperatureUnit, viewModel::setTemperatureUnit) {
        when (it) { TemperatureUnit.AUTO -> t("settings.auto"); else -> "°${it.name}" }
    }
    Spacer(Modifier.height(14.dp))
    SettingsGroup(t("settings.group.privacyLegal")) {
        if (privacyOptionsRequired) {
            ClickableRow(t("settings.privacyChoices"), "Google UMP", Icons.Filled.PrivacyTip, onPrivacyChoices)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
        ClickableRow(t("common.privacyPolicy"), icon = Icons.Filled.Policy, onClick = { onOpenLegal("privacy") })
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        ClickableRow(t("common.termsOfUse"), icon = Icons.Filled.Policy, onClick = { onOpenLegal("terms") })
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        ClickableRow(t("common.safetyNotice"), icon = Icons.Filled.SafetyCheck, onClick = { onOpenLegal("safety") })
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        ClickableRow(t("settings.dataDeletion"), icon = Icons.Filled.Policy, onClick = { onOpenLegal("data-choices") })
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        ClickableRow(t("settings.thirdParty"), icon = Icons.Filled.Policy, onClick = { onOpenLegal("third-party-notices") })
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        ClickableRow(t("settings.helpFeedback"), icon = Icons.Filled.Feedback, onClick = onSupport)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        ClickableRow(t("settings.deleteLocal"), icon = Icons.Filled.DeleteOutline, onClick = { onReplace(PressBenchSheet.DeleteLocal) }, danger = true)
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = LocalPressBenchPalette.current.ink2)
    Spacer(Modifier.height(6.dp))
    NativeSurface { Column { content() } }
}

@Composable
private fun LanguageSheet(viewModel: PressBenchViewModel, t: Translator, onDismiss: () -> Unit) {
    SheetTitle(t("common.language"))
    val locales = viewModel.strings.supportedLocales
    val columns = if (LocalConfiguration.current.screenWidthDp <= 390) 1 else 2
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.heightIn(max = 640.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        locales.forEach { code ->
            item(key = code) {
            val selected = code == viewModel.state.preferences.locale
            OutlinedButton(
                onClick = { viewModel.setLocale(code); onDismiss() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(viewModel.strings.displayLanguage(code, viewModel.state.preferences.locale), modifier = Modifier.weight(1f), textAlign = TextAlign.Start, maxLines = 2)
                if (selected) Icon(Icons.Filled.Check, t("common.selected"), tint = LocalPressBenchPalette.current.success)
            }
            }
        }
    }
}

@Composable
private fun DeleteLocalSheet(viewModel: PressBenchViewModel, t: Translator, onDismiss: () -> Unit) {
    SheetTitle(t("delete.title"))
    Text(t("delete.body"), style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(14.dp))
    Button(
        onClick = { viewModel.clearLocalData(); onDismiss() }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LocalPressBenchPalette.current.danger), shape = RoundedCornerShape(16.dp),
    ) { Icon(Icons.Filled.DeleteOutline, null); Spacer(Modifier.width(7.dp)); Text(t("settings.deleteLocal")) }
    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(t("common.cancel")) }
}

@Composable
private fun HistorySheet(record: RunRecord, viewModel: PressBenchViewModel, t: Translator, onMessage: (String) -> Unit, onDismiss: () -> Unit) {
    val locale = viewModel.state.preferences.locale
    val counts = record.productionCounts()
    SheetTitle(record.titleKey?.let(t) ?: record.title, formatDate(record.timestamp, locale))
    MetricStrip(
        formatNumber(counts.processed, locale), t("report.processed"),
        formatPercent(counts.firstPassYield, locale), t("report.firstPassYield"),
        formatNumber(counts.waste, locale), t("report.wasteUnits"),
    )
    Spacer(Modifier.height(12.dp))
    OutlineAction("PDF", { onMessage("PDF") }, Modifier.fillMaxWidth())
    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(t("common.cancel")) }
}
