package com.goodusestudios.pressbench

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.goodusestudios.pressbench.export.ReportExporter
import com.goodusestudios.pressbench.model.AppDestination
import com.goodusestudios.pressbench.ui.ActiveRunScreen
import com.goodusestudios.pressbench.ui.HomeScreen
import com.goodusestudios.pressbench.ui.MachinesScreen
import com.goodusestudios.pressbench.ui.OnboardingScreen
import com.goodusestudios.pressbench.ui.PressBenchBottomSheet
import com.goodusestudios.pressbench.ui.PressBenchAdsController
import com.goodusestudios.pressbench.ui.PressBenchBannerAd
import com.goodusestudios.pressbench.ui.PressBenchSheet
import com.goodusestudios.pressbench.ui.ReportsScreen
import com.goodusestudios.pressbench.ui.RunsScreen
import com.goodusestudios.pressbench.ui.SetupsScreen
import com.goodusestudios.pressbench.ui.Translator
import com.goodusestudios.pressbench.ui.theme.LocalPressBenchPalette
import com.goodusestudios.pressbench.ui.theme.PressBenchTheme

class MainActivity : ComponentActivity() {
    private val viewModel: PressBenchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.WHITE, AndroidColor.WHITE),
        )
        setContent { PressBenchApp(viewModel) }
    }
}

@Composable
private fun PressBenchApp(viewModel: PressBenchViewModel) {
    val state = viewModel.state
    val locale = state.preferences.locale
    val t: Translator = remember(locale) { { key -> viewModel.strings.text(key, locale) } }
    val rtl = viewModel.strings.isRtl(locale)
    val context = LocalContext.current
    val activity = LocalActivity.current ?: return
    val adsController = remember(activity) { PressBenchAdsController(activity) }

    KeepScreenAwake(state.activeRun != null)
    PressBenchTheme {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        ) {
            if (!viewModel.isLoaded) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            } else if (state.preferences.onboardingStep < 2) {
                OnboardingScreen(viewModel, t, onOpenLegal = { openLegal(context, it) })
            } else {
                MainAppScaffold(viewModel, t, adsController)
            }
        }
    }
}

@Composable
private fun MainAppScaffold(viewModel: PressBenchViewModel, t: Translator, adsController: PressBenchAdsController) {
    val state = viewModel.state
    val context = LocalContext.current
    val shortHeight = LocalConfiguration.current.screenHeightDp < 500
    var sheet by rememberSaveable(stateSaver = PressBenchSheetStateSaver) { mutableStateOf<PressBenchSheet?>(null) }
    var showCancelRun by rememberSaveable { mutableStateOf(false) }

    if (sheet != null) {
        BackHandler { sheet = null }
        PressBenchBottomSheet(
            sheet = sheet!!,
            viewModel = viewModel,
            t = t,
            onReplace = { sheet = it },
            onDismiss = { sheet = null },
            onOpenLegal = { openLegal(context, it) },
            onSupport = { openExternal(context, Uri.parse("mailto:lrodeveloperr@gmail.com?subject=PressBench%20Feedback")) },
            onMessage = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
            onPrivacyChoices = adsController::showPrivacyOptions,
            privacyOptionsRequired = adsController.privacyOptionsRequired,
        )
    }

    BackHandler(enabled = sheet == null && state.activeRun == null && state.destination in setOf(AppDestination.MACHINES, AppDestination.REPORTS)) {
        viewModel.navigate(if (state.destination == AppDestination.MACHINES) AppDestination.SETUPS else AppDestination.RUNS)
    }
    BackHandler(enabled = sheet == null && state.activeRun != null) { showCancelRun = true }

    if (showCancelRun && state.activeRun != null) {
        AlertDialog(
            onDismissRequest = { showCancelRun = false },
            title = { Text(t("run.cancel.title")) },
            text = { Text(t("run.cancel.body")) },
            confirmButton = {
                TextButton(onClick = { showCancelRun = false; viewModel.cancelRun() }) {
                    Text(t("run.cancelAndHome"), color = LocalPressBenchPalette.current.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelRun = false }) { Text(t("run.keepRunning")) }
            },
        )
    }

    val palette = LocalPressBenchPalette.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                destination = state.destination,
                activeRun = state.activeRun != null,
                shortHeight = shortHeight,
                t = t,
                onSettings = { sheet = PressBenchSheet.Settings },
                onCancelRun = { showCancelRun = true },
            )
        },
        bottomBar = {
            Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                PressBenchBannerAd(adsController, t("ad.space"))
                if (state.activeRun == null) AppNavigation(state.destination, t, shortHeight, viewModel::navigate)
            }
        },
    ) { padding ->
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(palette.backgroundTop, MaterialTheme.colorScheme.background))),
        ) {
            val compact = maxWidth < 600.dp
            val contentModifier = Modifier
                .widthIn(max = if (state.activeRun != null) 760.dp else 1200.dp)
                .fillMaxSize()
                .align(Alignment.TopCenter)
            if (state.activeRun != null) {
                ActiveRunScreen(viewModel, t, contentModifier)
            } else {
                when (state.destination) {
                    AppDestination.HOME -> HomeScreen(
                        state, t, compact,
                        onStart = { sheet = PressBenchSheet.StartRun(it.id) },
                        onPickSetup = { sheet = PressBenchSheet.SetupPicker },
                        onViewSetups = { viewModel.navigate(AppDestination.SETUPS) },
                        onAddMachine = { sheet = PressBenchSheet.NewMachine },
                        onAddSetup = { sheet = PressBenchSheet.NewSetup },
                        modifier = contentModifier,
                    )
                    AppDestination.SETUPS -> SetupsScreen(
                        state, t,
                        onAdd = { sheet = PressBenchSheet.NewSetup },
                        onMachines = { viewModel.navigate(AppDestination.MACHINES) },
                        onDetail = { sheet = PressBenchSheet.SetupDetail(it.id) },
                        onFavorite = { viewModel.toggleFavorite(it.id) },
                        modifier = contentModifier,
                    )
                    AppDestination.RUNS -> RunsScreen(
                        state, t,
                        onReports = { viewModel.navigate(AppDestination.REPORTS) },
                        onRecord = { sheet = PressBenchSheet.History(it.id) },
                        modifier = contentModifier,
                    )
                    AppDestination.MACHINES -> MachinesScreen(
                        state, t,
                        onBack = { viewModel.navigate(AppDestination.SETUPS) },
                        onAdd = { sheet = PressBenchSheet.NewMachine },
                        modifier = contentModifier,
                    )
                    AppDestination.REPORTS -> ReportsScreen(
                        state, t,
                        onPdf = { runCatching { ReportExporter.sharePdf(context, state.history, state.preferences.locale, viewModel.strings) }.onFailure { Toast.makeText(context, it.message ?: "PDF", Toast.LENGTH_LONG).show() } },
                        onCsv = { runCatching { ReportExporter.shareCsv(context, state.history, state.preferences.locale, viewModel.strings) }.onFailure { Toast.makeText(context, it.message ?: "CSV", Toast.LENGTH_LONG).show() } },
                        onBack = { viewModel.navigate(AppDestination.RUNS) },
                        modifier = contentModifier,
                    )
                }
            }
        }
    }
}

private val PressBenchSheetStateSaver = Saver<PressBenchSheet?, String>(
    save = { sheet ->
        when (sheet) {
            null -> "none"
            PressBenchSheet.SetupPicker -> "setup-picker"
            is PressBenchSheet.StartRun -> "start|${sheet.setupId}"
            is PressBenchSheet.SetupDetail -> "detail|${sheet.setupId}"
            PressBenchSheet.NewSetup -> "new-setup"
            PressBenchSheet.NewMachine -> "new-machine"
            PressBenchSheet.Settings -> "settings"
            PressBenchSheet.Language -> "language"
            PressBenchSheet.DeleteLocal -> "delete-local"
            is PressBenchSheet.History -> "history|${sheet.recordId}"
        }
    },
    restore = { token ->
        val value = token.substringAfter('|', "")
        when {
            token == "setup-picker" -> PressBenchSheet.SetupPicker
            token.startsWith("start|") -> PressBenchSheet.StartRun(value)
            token.startsWith("detail|") -> PressBenchSheet.SetupDetail(value)
            token == "new-setup" -> PressBenchSheet.NewSetup
            token == "new-machine" -> PressBenchSheet.NewMachine
            token == "settings" -> PressBenchSheet.Settings
            token == "language" -> PressBenchSheet.Language
            token == "delete-local" -> PressBenchSheet.DeleteLocal
            token.startsWith("history|") -> PressBenchSheet.History(value)
            else -> null
        }
    },
)

@Composable
private fun AppTopBar(
    destination: AppDestination,
    activeRun: Boolean,
    shortHeight: Boolean,
    t: Translator,
    onSettings: () -> Unit,
    onCancelRun: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
        Row(
            Modifier.windowInsetsPadding(WindowInsets.statusBars).heightIn(min = if (activeRun || shortHeight) 58.dp else 60.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("PressBench", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 1)
                if (!activeRun) {
                    Text(
                        t(
                            when (destination) {
                                AppDestination.HOME -> "tab.home"
                                AppDestination.SETUPS -> "tab.setups"
                                AppDestination.RUNS -> "tab.runs"
                                AppDestination.MACHINES -> "machines.title"
                                AppDestination.REPORTS -> "report.productionReport"
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalPressBenchPalette.current.ink2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (activeRun) {
                IconButton(onClick = onCancelRun) { Icon(Icons.Filled.Home, t("run.cancelAndHome")) }
            } else {
                IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, t("settings.title")) }
            }
        }
    }
}

@Composable
private fun AppNavigation(selected: AppDestination, t: Translator, shortHeight: Boolean, onSelect: (AppDestination) -> Unit) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth().height(if (shortHeight) 56.dp else 64.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        listOf(
            Triple(AppDestination.HOME, "tab.home", Icons.Filled.Home),
            Triple(AppDestination.SETUPS, "tab.setups", Icons.Filled.Tune),
            Triple(AppDestination.RUNS, "tab.runs", Icons.Filled.History),
        ).forEach { (destination, key, icon) ->
            NavigationBarItem(
                selected = selected == destination || (destination == AppDestination.SETUPS && selected == AppDestination.MACHINES) || (destination == AppDestination.RUNS && selected == AppDestination.REPORTS),
                onClick = { onSelect(destination) },
                icon = { Icon(icon, null) },
                label = { Text(t(key), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

@Composable
private fun KeepScreenAwake(keepAwake: Boolean) {
    val activity = LocalActivity.current ?: return
    DisposableEffect(keepAwake) {
        if (keepAwake) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { if (keepAwake) activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

private const val LEGAL_BASE_URL = "https://lrodeveloperr.github.io/pressbench-legal"

private fun openLegal(context: Context, path: String) = openExternal(context, Uri.parse("$LEGAL_BASE_URL/$path/"))

private fun openExternal(context: Context, uri: Uri) {
    val scheme = uri.scheme?.lowercase().orEmpty()
    if (scheme !in setOf("https", "http", "mailto", "tel")) return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
