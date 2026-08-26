package com.goodusestudios.pressbench.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.goodusestudios.pressbench.i18n.PressBenchStrings
import com.goodusestudios.pressbench.model.AppPreferences
import com.goodusestudios.pressbench.model.PressBenchState
import com.goodusestudios.pressbench.ui.theme.PressBenchTheme

@Composable
private fun AdaptiveHomePreview(compact: Boolean) {
    val context = LocalContext.current
    val strings = remember(context) { PressBenchStrings.load(context) }
    val state = remember { PressBenchState(preferences = AppPreferences(locale = "en")) }
    PressBenchTheme {
        HomeScreen(
            state = state,
            t = { strings.text(it, "en") },
            compact = compact,
            onStart = {},
            onPickSetup = {},
            onViewSetups = {},
            onAddMachine = {},
            onAddSetup = {},
            modifier = Modifier,
        )
    }
}

@Preview(name = "Phone portrait", widthDp = 412, heightDp = 892, showBackground = true)
@Composable
private fun PhonePortraitPreview() = AdaptiveHomePreview(compact = true)

@Preview(name = "Phone landscape", widthDp = 892, heightDp = 412, showBackground = true)
@Composable
private fun PhoneLandscapePreview() = AdaptiveHomePreview(compact = false)

@Preview(name = "Tablet portrait", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
private fun TabletPortraitPreview() = AdaptiveHomePreview(compact = false)

@Preview(name = "Tablet landscape", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun TabletLandscapePreview() = AdaptiveHomePreview(compact = false)

@Preview(name = "Small phone · large type", widthDp = 320, heightDp = 720, fontScale = 1.3f, showBackground = true)
@Composable
private fun SmallPhoneLargeTypePreview() {
    val context = LocalContext.current
    val strings = remember(context) { PressBenchStrings.load(context) }
    val state = remember { PressBenchState(preferences = AppPreferences(locale = "en")) }
    PressBenchTheme {
        SetupsScreen(state, { strings.text(it, "en") }, onAdd = {}, onMachines = {}, onDetail = {}, onFavorite = {})
    }
}

@Preview(name = "RTL phone", locale = "ar", widthDp = 412, heightDp = 892, showBackground = true)
@Composable
private fun RtlRunsPreview() {
    val context = LocalContext.current
    val strings = remember(context) { PressBenchStrings.load(context) }
    val state = remember { PressBenchState(preferences = AppPreferences(locale = "ar")) }
    PressBenchTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            RunsScreen(state, { strings.text(it, "ar") }, onReports = {}, onRecord = {})
        }
    }
}

@Preview(name = "System dark · forced light", widthDp = 412, heightDp = 892, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun DarkHomePreview() = AdaptiveHomePreview(compact = true)

@Preview(name = "Machine grid · tablet", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
private fun MachineGridPreview() {
    val context = LocalContext.current
    val strings = remember(context) { PressBenchStrings.load(context) }
    val state = remember { PressBenchState(preferences = AppPreferences(locale = "en")) }
    PressBenchTheme {
        MachinesScreen(state, { strings.text(it, "en") }, onBack = {}, onAdd = {})
    }
}
