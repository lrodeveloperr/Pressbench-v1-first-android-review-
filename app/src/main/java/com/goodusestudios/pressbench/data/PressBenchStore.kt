package com.goodusestudios.pressbench.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.goodusestudios.pressbench.model.ActiveRun
import com.goodusestudios.pressbench.model.AppDestination
import com.goodusestudios.pressbench.model.AppPreferences
import com.goodusestudios.pressbench.model.LastStart
import com.goodusestudios.pressbench.model.Machine
import com.goodusestudios.pressbench.model.Peel
import com.goodusestudios.pressbench.model.PressBenchState
import com.goodusestudios.pressbench.model.Pressure
import com.goodusestudios.pressbench.model.ProductionCounts
import com.goodusestudios.pressbench.model.RunPhase
import com.goodusestudios.pressbench.model.RunRecord
import com.goodusestudios.pressbench.model.Setup
import com.goodusestudios.pressbench.model.SetupStatus
import com.goodusestudios.pressbench.model.TemperatureUnit
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.first

private val Context.pressBenchDataStore by preferencesDataStore(
    name = "pressbench_native_v14",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class PressBenchStore(context: Context) {
    private val dataStore = context.applicationContext.pressBenchDataStore
    suspend fun load(defaultLocale: String): PressBenchState {
        val empty = emptyState(defaultLocale)
        val raw = runCatching { dataStore.data.first()[STATE_KEY] }.getOrNull() ?: return empty
        return runCatching { decode(JSONObject(raw), defaultLocale) }.getOrElse { empty }
    }

    suspend fun save(state: PressBenchState) {
        dataStore.edit { it[STATE_KEY] = encode(state).toString() }
    }

    suspend fun clear(defaultLocale: String): PressBenchState {
        dataStore.edit { it.clear() }
        return emptyState(defaultLocale)
    }

    private fun encode(state: PressBenchState) = JSONObject().apply {
        put("schema", SCHEMA)
        put("destination", state.destination.name)
        put("setups", JSONArray().apply { state.setups.forEach { put(it.toJson()) } })
        put("machines", JSONArray().apply { state.machines.forEach { put(it.toJson()) } })
        put("history", JSONArray().apply { state.history.forEach { put(it.toJson()) } })
        put("activeRun", state.activeRun?.toJson() ?: JSONObject.NULL)
        put("preferences", state.preferences.toJson())
        put("lastStart", state.lastStart.toJson())
        put("timerRemaining", state.timerRemaining.coerceAtLeast(0))
        put("timerRunning", state.timerRunning)
        state.timerDeadline?.let { put("timerDeadline", it) }
    }

    private fun decode(root: JSONObject, defaultLocale: String): PressBenchState {
        val fallback = emptyState(defaultLocale)
        val storedSchema = root.optInt("schema", 0)
        val decodedRun = root.optJSONObject("activeRun")?.activeRun()
        val timerWasRunning = root.optBoolean("timerRunning")
        val deadline = root.optLongOrNull("timerDeadline")
        val remaining = if (timerWasRunning && deadline != null) {
            ((deadline - System.currentTimeMillis() + 999L) / 1000L).coerceAtLeast(0L).toInt()
        } else root.optInt("timerRemaining", 0).coerceAtLeast(0)
        val recoveredRun = decodedRun?.copy(
            phase = if (decodedRun.phase == RunPhase.RUNNING) RunPhase.PAUSED else decodedRun.phase,
            recovered = true,
            timerRecovered = timerWasRunning && decodedRun.phase == RunPhase.FIRST_PIECE,
            timerExpiredDuringRecovery = timerWasRunning && decodedRun.phase == RunPhase.FIRST_PIECE && remaining <= 0,
        )
        val decoded = PressBenchState(
            destination = root.optEnum("destination", AppDestination.HOME),
            setups = root.optJSONArray("setups")?.objects()?.map { it.setup() } ?: fallback.setups,
            machines = root.optJSONArray("machines")?.objects()?.map { it.machine() } ?: fallback.machines,
            history = root.optJSONArray("history")?.objects()?.map { it.record() } ?: fallback.history,
            activeRun = recoveredRun,
            preferences = root.optJSONObject("preferences")?.appPreferences(defaultLocale) ?: fallback.preferences,
            lastStart = root.optJSONObject("lastStart")?.lastStart() ?: fallback.lastStart,
            timerRemaining = remaining,
            timerRunning = false,
            timerDeadline = null,
        )
        return if (storedSchema < SCHEMA) decoded.withoutLegacyPresetData() else decoded
    }

    private fun Setup.toJson() = JSONObject().apply {
        put("id", id); put("title", title); putNullable("titleKey", titleKey)
        put("material", material); putNullable("materialKey", materialKey)
        put("transfer", transfer); put("machine", machine); put("temperature", temperature)
        put("seconds", seconds); put("pressure", pressure.level); put("status", status.name)
        put("source", source); putNullable("sourceKey", sourceKey); putNullable("checkedAt", checkedAt)
        put("prepressSeconds", prepressSeconds); put("peel", peel.name); put("postpressSeconds", postpressSeconds)
        put("favorite", favorite); put("provenRuns", provenRuns); put("provenPieces", provenPieces)
    }

    private fun JSONObject.setup() = Setup(
        id = optString("id", "s${System.currentTimeMillis()}"),
        title = optString("title", "Setup"), titleKey = optNullableString("titleKey"),
        material = optString("material"), materialKey = optNullableString("materialKey"),
        transfer = optString("transfer", "Transfer"), machine = optString("machine"),
        temperature = optString("temperature", "300°F"), seconds = optInt("seconds", 12).coerceAtLeast(1),
        pressure = Pressure.from(optInt("pressure", 2)), status = optEnum("status", SetupStatus.TRIAL),
        source = optString("source"), sourceKey = optNullableString("sourceKey"), checkedAt = optLongOrNull("checkedAt"),
        prepressSeconds = optInt("prepressSeconds").coerceAtLeast(0), peel = optEnum("peel", Peel.WARM),
        postpressSeconds = optInt("postpressSeconds").coerceAtLeast(0), favorite = optBoolean("favorite"),
        provenRuns = optInt("provenRuns").coerceAtLeast(0), provenPieces = optInt("provenPieces").coerceAtLeast(0),
    )

    private fun Machine.toJson() = JSONObject().apply {
        put("name", name); put("detail", detail); put("verifiedAt", verifiedAt)
    }

    private fun JSONObject.machine() = Machine(optString("name"), optString("detail"), optLong("verifiedAt"))

    private fun RunRecord.toJson() = JSONObject().apply {
        put("id", id); put("setupId", setupId); put("title", title); putNullable("titleKey", titleKey)
        put("timestamp", timestamp); put("processed", processed); put("good", good)
        put("waste", waste); put("rework", rework); put("note", note); put("issue", issue)
    }

    private fun JSONObject.record(): RunRecord {
        val counts = ProductionCounts.normalized(
            processed = optInt("processed"),
            waste = optInt("waste"),
            rework = optInt("rework"),
        )
        return RunRecord(
            id = optString("id", "b${System.currentTimeMillis()}"), setupId = optString("setupId"),
            title = optString("title"), titleKey = optNullableString("titleKey"), timestamp = optLong("timestamp"),
            processed = counts.processed, good = counts.firstPassGood,
            waste = counts.waste, rework = counts.rework,
            note = optString("note"), issue = optBoolean("issue") || counts.waste > 0 || counts.rework > 0,
        )
    }

    private fun ActiveRun.toJson() = JSONObject().apply {
        put("id", id); put("setupId", setupId); put("quantity", quantity); put("processed", processed)
        put("jobReference", jobReference); put("phase", phase.name); put("hasIssue", hasIssue)
        put("qcDone", qcDone); put("firstPieceAttempts", firstPieceAttempts); put("recipeIndex", recipeIndex)
        put("recipeComplete", recipeComplete); put("timerRecovered", timerRecovered)
        put("timerExpiredDuringRecovery", timerExpiredDuringRecovery); put("startedAt", startedAt)
    }

    private fun JSONObject.activeRun() = ActiveRun(
        id = optString("id", "r${System.currentTimeMillis()}"), setupId = optString("setupId"),
        quantity = optInt("quantity", 1).coerceAtLeast(1),
        processed = optInt("processed").coerceAtLeast(0).coerceAtMost(optInt("quantity", 1).coerceAtLeast(1)),
        jobReference = optString("jobReference"), phase = optEnum("phase", RunPhase.PREFLIGHT),
        hasIssue = optBoolean("hasIssue"), qcDone = optBoolean("qcDone"),
        firstPieceAttempts = optInt("firstPieceAttempts").coerceAtLeast(0),
        recipeIndex = optInt("recipeIndex").coerceAtLeast(0), recipeComplete = optBoolean("recipeComplete"),
        timerRecovered = optBoolean("timerRecovered"), timerExpiredDuringRecovery = optBoolean("timerExpiredDuringRecovery"),
        startedAt = optLong("startedAt", System.currentTimeMillis()),
    )

    private fun AppPreferences.toJson() = JSONObject().apply {
        put("locale", locale); put("temperatureUnit", temperatureUnit.name)
        put("soundEnabled", soundEnabled); put("vibrationEnabled", vibrationEnabled)
        put("onboardingStep", onboardingStep)
    }

    private fun JSONObject.appPreferences(defaultLocale: String) = AppPreferences(
        locale = optString("locale", defaultLocale), temperatureUnit = optEnum("temperatureUnit", TemperatureUnit.AUTO),
        soundEnabled = optBoolean("soundEnabled", true), vibrationEnabled = optBoolean("vibrationEnabled", true),
        onboardingStep = optInt("onboardingStep").coerceIn(0, 2),
    )

    private fun LastStart.toJson() = JSONObject().apply { put("setupId", setupId); put("quantity", quantity) }
    private fun JSONObject.lastStart() = LastStart(optString("setupId"), optInt("quantity", 1).coerceIn(1, 999_999))

    private fun emptyState(defaultLocale: String) =
        PressBenchState(preferences = AppPreferences(locale = defaultLocale))

    private fun PressBenchState.withoutLegacyPresetData(): PressBenchState {
        val legacySetupIds = setups.filter { it.titleKey?.startsWith("demo.setup.") == true }.mapTo(mutableSetOf()) { it.id }
        val cleanedSetups = setups.filterNot { it.id in legacySetupIds }
        val cleanedMachines = machines.filterNot {
            (it.name == "P1" && it.detail == "Hotronix · 15×15") ||
                (it.name == "P2" && it.detail == "Geo Knight · 16×20")
        }
        val cleanedHistory = history.filterNot {
            it.setupId in legacySetupIds || it.titleKey?.startsWith("demo.setup.") == true
        }
        val cleanedActiveRun = activeRun?.takeUnless { it.setupId in legacySetupIds }
        return copy(
            destination = if (destination == AppDestination.MACHINES && cleanedMachines.isEmpty()) AppDestination.HOME else destination,
            setups = cleanedSetups,
            machines = cleanedMachines,
            history = cleanedHistory,
            activeRun = cleanedActiveRun,
            lastStart = lastStart.takeUnless { it.setupId in legacySetupIds } ?: LastStart(),
            timerRemaining = if (cleanedActiveRun == null) 0 else timerRemaining,
            timerRunning = false,
            timerDeadline = null,
        )
    }

    private fun JSONObject.putNullable(key: String, value: Any?) { put(key, value ?: JSONObject.NULL) }
    private fun JSONObject.optNullableString(key: String) = if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
    private fun JSONObject.optLongOrNull(key: String) = if (isNull(key) || !has(key)) null else optLong(key)
    private inline fun <reified T : Enum<T>> JSONObject.optEnum(key: String, fallback: T): T =
        runCatching { enumValueOf<T>(optString(key, fallback.name)) }.getOrDefault(fallback)

    private fun JSONArray.objects() = buildList {
        for (index in 0 until length()) optJSONObject(index)?.let(::add)
    }

    companion object {
        private val STATE_KEY = stringPreferencesKey("state")
        private const val SCHEMA = 15
    }
}
