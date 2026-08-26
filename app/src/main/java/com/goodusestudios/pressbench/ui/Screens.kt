package com.goodusestudios.pressbench.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodusestudios.pressbench.FirstPieceOutcome
import com.goodusestudios.pressbench.PressBenchViewModel
import com.goodusestudios.pressbench.model.ActiveRun
import com.goodusestudios.pressbench.model.Peel
import com.goodusestudios.pressbench.model.PressBenchState
import com.goodusestudios.pressbench.model.RecipeStageType
import com.goodusestudios.pressbench.model.RunPhase
import com.goodusestudios.pressbench.model.RunRecord
import com.goodusestudios.pressbench.model.Setup
import com.goodusestudios.pressbench.model.SetupStatus
import com.goodusestudios.pressbench.model.productionSummary
import com.goodusestudios.pressbench.model.productionCounts
import com.goodusestudios.pressbench.ui.theme.LocalPressBenchPalette

@Composable
fun HomeScreen(
    state: PressBenchState,
    t: Translator,
    compact: Boolean,
    onStart: (Setup) -> Unit,
    onPickSetup: () -> Unit,
    onViewSetups: () -> Unit,
    onAddMachine: () -> Unit,
    onAddSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = state.preferences.locale
    val summary = state.history.productionSummary()
    val recent = state.history.firstOrNull()?.let { record -> state.setups.find { it.id == record.setupId } }
    val favorites = state.setups.sortedWith(compareByDescending<Setup> { it.favorite }.thenByDescending { it.provenRuns }).take(2)
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = if (compact) 14.dp else 22.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        if (state.setups.isEmpty()) {
            item {
                EmptyHomeState(
                    t = t,
                    hasMachine = state.machines.isNotEmpty(),
                    onAction = if (state.machines.isEmpty()) onAddMachine else onAddSetup,
                )
            }
            return@LazyColumn
        }
        item {
            if (recent != null) RepeatCard(recent, state, t, onStart = { onStart(recent) })
            else StartHero(t, onPickSetup)
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("home.recentSetups"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = onViewSetups) { Text(t("home.viewAll")) }
            }
        }
        if (!compact && favorites.size > 1) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    favorites.forEach { setup ->
                        SetupCard(
                            setup, state.preferences.temperatureUnit, locale, t,
                            onClick = { onStart(setup) }, modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        } else {
            items(favorites, key = { it.id }) { setup ->
                SetupCard(setup, state.preferences.temperatureUnit, locale, t, onClick = { onStart(setup) })
            }
        }
        item {
            MetricStrip(
                formatNumber(state.history.size, locale), t("home.metric.batches"),
                formatPercent(summary.firstPassYield, locale), t("home.metric.firstPass"),
                formatNumber(summary.waste, locale), t("home.metric.waste"),
            )
        }
        item { Spacer(Modifier.height(2.dp)) }
    }
}

@Composable
private fun EmptyHomeState(t: Translator, hasMachine: Boolean, onAction: () -> Unit) {
    NativeSurface(Modifier.fillMaxWidth(), raised = true) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PressMachineIcon(Modifier.size(88.dp))
            Spacer(Modifier.height(16.dp))
            Text(t("home.empty.title"), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            PrimaryAction(
                text = t(if (hasMachine) "home.empty.addSetup" else "home.empty.addMachine"),
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.Add,
            )
        }
    }
}

@Composable
private fun RepeatCard(setup: Setup, state: PressBenchState, t: Translator, onStart: () -> Unit) {
    val palette = LocalPressBenchPalette.current
    val narrow = LocalConfiguration.current.screenWidthDp <= 360
    Surface(
        shape = RoundedCornerShape(26.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 5.dp,
    ) {
        Column(Modifier.background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, palette.surface2))).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    StatusBadge(SetupStatus.PROVEN, t)
                    Spacer(Modifier.height(8.dp))
                    Text(localTitle(setup, t), fontSize = 21.sp, lineHeight = 25.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${localMaterial(setup, t)} · ${setup.machine}", fontSize = 11.sp, lineHeight = 16.sp, color = palette.ink2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Box(
                    Modifier.size(if (narrow) 72.dp else 86.dp).clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, palette.warmSoft)))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    SetupArtwork(setup, Modifier.size(if (narrow) 66.dp else 78.dp))
                }
            }
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RepeatFact(formatTemperature(setup.temperature, state.preferences.temperatureUnit, state.preferences.locale), t("common.temperature"), Modifier.weight(1f))
                RepeatFact("${formatNumber(setup.seconds, state.preferences.locale)}s", t("common.durationSeconds"), Modifier.weight(1f))
                RepeatFact(pressureText(setup.pressure, t), t("common.pressure"), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            PrimaryAction(t("home.startRun.title"), onStart, Modifier.fillMaxWidth(), Icons.Filled.PlayArrow)
        }
    }
}

@Composable
private fun RepeatFact(value: String, label: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.padding(horizontal = 7.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.ExtraBold, color = LocalPressBenchPalette.current.ink3, maxLines = 2, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun StartHero(t: Translator, onStart: () -> Unit) {
    val palette = LocalPressBenchPalette.current
    Box(
        Modifier.fillMaxWidth().heightIn(min = if (LocalConfiguration.current.screenWidthDp <= 390) 264.dp else 255.dp)
            .clip(LargeShape)
            .background(Brush.linearGradient(listOf(palette.dark, Color(0xFF24424F), MaterialTheme.colorScheme.primary))),
    ) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = 62.dp, y = (-76).dp).size(220.dp).clip(CircleShape).background(palette.brand3.copy(alpha = .22f)))
        Column(Modifier.fillMaxWidth(.58f).padding(start = 18.dp, top = 22.dp, bottom = 86.dp)) {
            Text("PressBench", fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Color(0xFFC9E3EB))
            Spacer(Modifier.height(11.dp))
            Text(t("home.startRun.title"), fontSize = if (LocalConfiguration.current.screenWidthDp <= 390) 25.sp else 28.sp, lineHeight = if (LocalConfiguration.current.screenWidthDp <= 390) 29.sp else 32.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
        PressArtwork(Modifier.align(Alignment.BottomEnd).offset(x = 13.dp, y = (-5).dp).size(220.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 15.dp).heightIn(min = 52.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = palette.dark),
        ) {
            Icon(Icons.Filled.PlayArrow, null, tint = palette.warm)
            Spacer(Modifier.width(8.dp))
            Text(t("home.startRun.title"), fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupsScreen(
    state: PressBenchState,
    t: Translator,
    onAdd: () -> Unit,
    onMachines: () -> Unit,
    onDetail: (Setup) -> Unit,
    onFavorite: (Setup) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    val filtered = remember(state.setups, query, filter, state.preferences.locale) {
        state.setups.filter { setup ->
            (filter == "all" || setup.status.name.equals(filter, ignoreCase = true)) &&
                (query.isBlank() || listOf(setup.titleKey?.let(t) ?: setup.title, setup.materialKey?.let(t) ?: setup.material, setup.transfer, setup.machine)
                    .joinToString(" ").contains(query, ignoreCase = true))
        }.sortedWith(compareByDescending<Setup> { it.favorite }.thenByDescending { it.provenRuns })
    }
    Column(modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t("setups.title"), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            FilledIconButton(onClick = onAdd) { Icon(Icons.Filled.Add, t("setup.add")) }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onMachines, modifier = Modifier.size(64.dp)) {
                PressMachineIcon(Modifier.size(64.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(t("setups.search")) }, leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true, shape = RoundedCornerShape(16.dp),
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("all" to "common.all", "proven" to "status.proven", "trial" to "status.trial").forEach { (value, key) ->
                AssistChip(
                    onClick = { filter = value }, label = { Text(t(key)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (filter == value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        labelColor = if (filter == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(320.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 14.dp),
        ) {
            items(filtered, key = { it.id }) { setup ->
                SetupCard(
                    setup, state.preferences.temperatureUnit, state.preferences.locale, t,
                    onClick = { onDetail(setup) }, showFavorite = true,
                    onFavorite = { onFavorite(setup) },
                )
            }
        }
    }
}

@Composable
fun RunsScreen(
    state: PressBenchState,
    t: Translator,
    onReports: () -> Unit,
    onRecord: (RunRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = state.history.productionSummary()
    val locale = state.preferences.locale
    LazyColumn(
        modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("runs.title"), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onReports) { Icon(Icons.Filled.Assessment, t("report.productionReport")) }
            }
        }
        item {
            MetricStrip(
                formatNumber(summary.processed, locale), t("report.unitsProcessed"),
                formatPercent(summary.firstPassYield, locale), t("report.firstPassYield"),
                formatNumber(summary.waste, locale), t("report.wasteUnits"),
            )
        }
        item {
            NativeSurface {
                Column {
                    state.history.forEachIndexed { index, record ->
                        RunRow(record, locale, t, onClick = { onRecord(record) })
                        if (index < state.history.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
private fun RunRow(record: RunRecord, locale: String, t: Translator, onClick: () -> Unit) {
    val palette = LocalPressBenchPalette.current
    val counts = record.productionCounts()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(if (record.issue) palette.warmSoft else palette.successSoft),
            contentAlignment = Alignment.Center,
        ) { Icon(if (record.issue) Icons.Filled.ReportProblem else Icons.Filled.Check, null, tint = if (record.issue) palette.warm else palette.success) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(record.titleKey?.let(t) ?: record.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${formatDate(record.timestamp, locale)} · ${formatNumber(counts.firstPassGood, locale)}/${formatNumber(counts.processed, locale)}", style = MaterialTheme.typography.bodySmall, color = palette.ink2, maxLines = 2)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.ink3)
    }
}

@Composable
fun MachinesScreen(state: PressBenchState, t: Translator, onBack: () -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val columns = if (LocalConfiguration.current.screenWidthDp < 600) GridCells.Fixed(2) else GridCells.Adaptive(220.dp)
    Column(modifier.fillMaxSize().padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t("machines.title"), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            FilledIconButton(onClick = onAdd) { Icon(Icons.Filled.Add, t("machines.add")) }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, t("common.back")) }
        }
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = columns,
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.machines, key = { it.name }) { machine ->
                NativeSurface {
                    Column(Modifier.padding(16.dp)) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            PressMachineIcon(Modifier.size(42.dp).padding(3.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(machine.name, style = MaterialTheme.typography.titleLarge)
                        Text(machine.detail, style = MaterialTheme.typography.bodyMedium, color = LocalPressBenchPalette.current.ink2)
                        Spacer(Modifier.height(7.dp))
                        Text("✓ ${formatShortDate(machine.verifiedAt, state.preferences.locale)}", style = MaterialTheme.typography.labelMedium, color = LocalPressBenchPalette.current.success)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsScreen(
    state: PressBenchState,
    t: Translator,
    onPdf: () -> Unit,
    onCsv: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = state.history.productionSummary()
    val locale = state.preferences.locale
    LazyColumn(
        modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("report.productionReport"), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, t("common.back")) }
            }
        }
        item {
            MetricStrip(
                formatNumber(summary.processed, locale), t("report.unitsProcessed"),
                formatPercent(summary.firstPassYield, locale), t("report.firstPassYield"),
                formatNumber(summary.waste, locale), t("report.wasteUnits"),
            )
        }
        item {
            NativeSurface {
                Column {
                    state.history.forEachIndexed { index, record ->
                        val counts = record.productionCounts()
                        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(record.titleKey?.let(t) ?: record.title, style = MaterialTheme.typography.titleMedium)
                                Text(formatDate(record.timestamp, locale), style = MaterialTheme.typography.bodySmall, color = LocalPressBenchPalette.current.ink2)
                            }
                            Text("${formatNumber(counts.firstPassGood, locale)}/${formatNumber(counts.processed, locale)}", style = MaterialTheme.typography.titleMedium)
                        }
                        if (index < state.history.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlineAction("PDF", onPdf, Modifier.weight(1f))
                OutlineAction("CSV", onCsv, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ActiveRunScreen(viewModel: PressBenchViewModel, t: Translator, modifier: Modifier = Modifier) {
    val state = viewModel.state
    val run = state.activeRun ?: return
    val setup = viewModel.setup(run.setupId) ?: return
    val locale = state.preferences.locale
    val percent = (run.processed.toFloat() / run.quantity).coerceIn(0f, 1f)
    val step = when (run.phase) {
        RunPhase.PREFLIGHT -> 0
        RunPhase.FIRST_PIECE -> 1
        RunPhase.PRODUCTION_READY, RunPhase.RUNNING, RunPhase.PAUSED -> 2
        RunPhase.RESULT_PENDING -> 3
    }
    LazyColumn(
        modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (run.recovered) item { Notice(t("runs.resume"), NoticeKind.WARNING) }
        item {
            NativeSurface {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(localTitle(setup, t), style = MaterialTheme.typography.titleLarge)
                            Text(if (run.jobReference.isBlank()) setup.machine else "${run.jobReference} · ${setup.machine}", style = MaterialTheme.typography.bodySmall, color = LocalPressBenchPalette.current.ink2)
                        }
                        if (run.phase == RunPhase.RUNNING) StatusBadge(SetupStatus.PROVEN, { if (it == "status.proven") t("runState.running") else t(it) })
                    }
                    Spacer(Modifier.height(10.dp))
                    Row { Text("${formatNumber(run.processed, locale)}/${formatNumber(run.quantity, locale)}", Modifier.weight(1f)); Text(formatPercent(percent, locale)) }
                    Spacer(Modifier.height(5.dp))
                    LinearProgressIndicator(progress = { percent }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape))
                }
            }
        }
        item {
            ProgressSteps(
                listOf("onboarding.process.setup", "onboarding.process.firstPiece", "onboarding.process.production", "onboarding.process.result").map(t),
                step,
            )
        }
        item { FactGrid(setup, state.preferences.temperatureUnit, locale, t) }
        item {
            when (run.phase) {
                RunPhase.PREFLIGHT -> Preflight(setup, t, viewModel::confirmInstructions)
                RunPhase.FIRST_PIECE -> FirstPiece(run, setup, state.timerRemaining, state.timerRunning, t, viewModel)
                RunPhase.PRODUCTION_READY -> ProductionReady(t, viewModel::startProduction)
                RunPhase.RUNNING -> ProductionRunning(run, locale, t, viewModel)
                RunPhase.PAUSED -> PausedRun(t, viewModel)
                RunPhase.RESULT_PENDING -> ResultPending(run, t, viewModel)
            }
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun Preflight(setup: Setup, t: Translator, onConfirm: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Notice(localSource(setup, t), NoticeKind.SUCCESS, t("report.instructionSource"))
        RecipeStrip(setup, t = t)
        PrimaryAction(t("run.confirmInstructions"), onConfirm, Modifier.fillMaxWidth(), Icons.Filled.Check)
    }
}

@Composable
private fun FirstPiece(run: ActiveRun, setup: Setup, remaining: Int, running: Boolean, t: Translator, viewModel: PressBenchViewModel) {
    val stages = setup.recipeStages()
    val index = run.recipeIndex.coerceIn(0, stages.lastIndex.coerceAtLeast(0))
    val current = stages.getOrNull(index)
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        if (run.recipeComplete) {
            RecipeStrip(setup, complete = true, t = t)
            Notice(t("qc.title"), NoticeKind.SUCCESS)
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    PrimaryAction(t("qc.pass"), { viewModel.firstPiece(FirstPieceOutcome.PASS) }, Modifier.weight(1f), Icons.Filled.Check)
                    TonalAction(t("run.adjustRetry"), { viewModel.firstPiece(FirstPieceOutcome.ADJUST) }, Modifier.weight(1f))
                }
                DangerAction(t("run.stopWithNote"), { viewModel.firstPiece(FirstPieceOutcome.STOP) }, Modifier.fillMaxWidth())
            }
        } else if (current?.type == RecipeStageType.PEEL) {
            RecipeStrip(setup, active = index, t = t)
            NativeSurface {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Check, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(t("peel.${setup.peel.name.lowercase()}"), style = MaterialTheme.typography.headlineMedium)
                    Text(t("stage.peel"), color = LocalPressBenchPalette.current.ink2)
                    Spacer(Modifier.height(12.dp))
                    PrimaryAction(t("common.continue"), viewModel::advanceRecipeStage, Modifier.fillMaxWidth())
                }
            }
        } else {
            RecipeStrip(setup, active = index, t = t)
            NativeSurface {
                Column(Modifier.fillMaxWidth().padding(15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { if ((current?.durationSeconds ?: 0) > 0) remaining.toFloat() / current!!.durationSeconds else 0f },
                            modifier = Modifier.size(150.dp),
                            strokeWidth = 10.dp,
                            color = LocalPressBenchPalette.current.warm,
                            trackColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(remaining.toString().padStart(2, '0'), fontSize = 45.sp, lineHeight = 48.sp, fontWeight = FontWeight.Black)
                            Text("s", fontSize = 16.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("${current?.let { if (it.type == RecipeStageType.PREPRESS) t("stage.prepress") else t("stage.press") }} · ${formatTemperature(setup.temperature, viewModel.state.preferences.temperatureUnit, viewModel.state.preferences.locale)} · ${pressureText(setup.pressure, t)}", style = MaterialTheme.typography.bodySmall, color = LocalPressBenchPalette.current.ink2, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    val timerLabel = when {
                        running -> t("run.pauseTimer")
                        run.timerExpiredDuringRecovery -> t("common.continue")
                        run.timerRecovered -> t("runs.resume")
                        else -> t("run.startTimer")
                    }
                    TonalAction(
                        timerLabel,
                        viewModel::toggleTimer,
                        Modifier.fillMaxWidth(),
                        if (running) Icons.Filled.Pause else if (run.timerExpiredDuringRecovery) Icons.Filled.Check else Icons.Filled.Timer,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductionReady(t: Translator, onStart: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Notice(t("run.firstPiecePass"), NoticeKind.SUCCESS)
        PrimaryAction(t("run.startProduction"), onStart, Modifier.fillMaxWidth(), Icons.Filled.PlayArrow)
    }
}

@Composable
private fun ProductionRunning(run: ActiveRun, locale: String, t: Translator, viewModel: PressBenchViewModel) {
    val palette = LocalPressBenchPalette.current
    val counter = "${formatNumber(run.processed, locale)} / ${formatNumber(run.quantity, locale)}"
    val counterSize = when {
        counter.length >= 13 -> 44.sp
        counter.length >= 10 -> 52.sp
        LocalConfiguration.current.screenWidthDp <= 390 -> 58.sp
        else -> 66.sp
    }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        NativeSurface(raised = true) {
            Column(Modifier.fillMaxWidth().padding(15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(counter, fontSize = counterSize, lineHeight = counterSize, fontWeight = FontWeight.Black, maxLines = 1)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 78.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(Brush.verticalGradient(listOf(palette.brand2, MaterialTheme.colorScheme.primary)))
                        .clickable(role = Role.Button, onClick = viewModel::completeCycle)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Check, null, Modifier.size(30.dp), tint = Color.White)
                    Spacer(Modifier.width(9.dp))
                    Text("+1", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineAction(t("run.undoLastCount"), viewModel::undoCycle, Modifier.weight(1f), Icons.Filled.Undo)
                    OutlineAction(t("runs.pause"), viewModel::pauseRun, Modifier.weight(1f), Icons.Filled.Pause)
                }
            }
        }
        if (run.processed >= 10 && !run.qcDone) {
            Notice(t("qc.due"), NoticeKind.WARNING)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TonalAction(t("qc.pass"), { viewModel.recordQc(true) }, Modifier.weight(1f))
                OutlineAction(t("qc.adjust"), { viewModel.recordQc(false) }, Modifier.weight(1f))
            }
        }
        OutlineAction(t("runs.end"), viewModel::endRun, Modifier.fillMaxWidth())
    }
}

@Composable
private fun PausedRun(t: Translator, viewModel: PressBenchViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Notice(t("runs.pause"), NoticeKind.WARNING)
        PrimaryAction(t("runs.resume"), viewModel::resumeRun, Modifier.fillMaxWidth(), Icons.Filled.PlayArrow)
        OutlineAction(t("runs.end"), viewModel::endRun, Modifier.fillMaxWidth())
    }
}

@Composable
private fun ResultPending(run: ActiveRun, t: Translator, viewModel: PressBenchViewModel) {
    var showIssue by rememberSaveable { mutableStateOf(run.hasIssue || run.processed != run.quantity) }
    var waste by rememberSaveable { mutableStateOf(if (run.hasIssue) minOf(1, run.processed).toString() else "0") }
    var rework by rememberSaveable { mutableStateOf("0") }
    var notes by rememberSaveable { mutableStateOf("") }
    if (!showIssue) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Notice(t("result.allGood"), NoticeKind.SUCCESS)
            PrimaryAction(t("result.saveAllGood"), { viewModel.finalizeRun(0, 0, "") }, Modifier.fillMaxWidth(), Icons.Filled.Check)
            OutlineAction(t("result.addIssue"), { showIssue = true }, Modifier.fillMaxWidth(), Icons.Filled.ReportProblem)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            NativeSurface(raised = true) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(waste, { waste = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text(t("report.wasteUnits")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                        OutlinedTextField(rework, { rework = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text(t("report.reworkedUnits")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    }
                    OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text(t("common.notes")) }, minLines = 3)
                }
            }
            val wasteInt = waste.toIntOrNull() ?: -1
            val reworkInt = rework.toIntOrNull() ?: -1
            PrimaryAction(
                t("result.saveOutcome"),
                { viewModel.finalizeRun(wasteInt, reworkInt, notes) },
                Modifier.fillMaxWidth(), Icons.Filled.Check,
                enabled = wasteInt in 0..run.processed && reworkInt in 0..(run.processed - wasteInt.coerceAtLeast(0)),
            )
        }
    }
}

private enum class NoticeKind { SUCCESS, WARNING }

@Composable
private fun Notice(text: String, kind: NoticeKind, title: String? = null) {
    val palette = LocalPressBenchPalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (kind == NoticeKind.SUCCESS) palette.successSoft else palette.warningSoft,
        contentColor = if (kind == NoticeKind.SUCCESS) palette.success else palette.warning,
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            title?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DangerAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPressBenchPalette.current
    OutlinedButton(
        onClick = onClick, modifier = modifier.heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = palette.dangerSoft, contentColor = palette.danger),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.danger.copy(alpha = .25f)),
    ) { Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center) }
}
