package com.goodusestudios.pressbench.model

enum class AppDestination { HOME, SETUPS, RUNS, MACHINES, REPORTS }

enum class SetupStatus { PROVEN, TRIAL }

enum class Pressure(val level: Int) { LIGHT(1), MEDIUM(2), FIRM(3);
    companion object { fun from(level: Int) = entries.minBy { kotlin.math.abs(it.level - level) } }
}

enum class Peel { HOT, WARM, COLD }

enum class RunPhase {
    PREFLIGHT,
    FIRST_PIECE,
    PRODUCTION_READY,
    RUNNING,
    PAUSED,
    RESULT_PENDING,
}

enum class TemperatureUnit { AUTO, C, F }

enum class RecipeStageType { PREPRESS, PRESS, PEEL, POSTPRESS }

data class RecipeStage(
    val type: RecipeStageType,
    val durationSeconds: Int,
    val value: String,
)

data class Setup(
    val id: String,
    val title: String,
    val titleKey: String? = null,
    val material: String,
    val materialKey: String? = null,
    val transfer: String,
    val machine: String,
    val temperature: String,
    val seconds: Int,
    val pressure: Pressure,
    val status: SetupStatus,
    val source: String = "",
    val sourceKey: String? = null,
    val checkedAt: Long? = null,
    val prepressSeconds: Int = 0,
    val peel: Peel = Peel.WARM,
    val postpressSeconds: Int = 0,
    val favorite: Boolean = false,
    val provenRuns: Int = 0,
    val provenPieces: Int = 0,
) {
    fun recipeStages(): List<RecipeStage> = buildList {
        if (prepressSeconds > 0) add(RecipeStage(RecipeStageType.PREPRESS, prepressSeconds, "${prepressSeconds}s"))
        add(RecipeStage(RecipeStageType.PRESS, seconds, "${seconds}s"))
        add(RecipeStage(RecipeStageType.PEEL, 0, peel.name.lowercase()))
        if (postpressSeconds > 0) add(RecipeStage(RecipeStageType.POSTPRESS, postpressSeconds, "${postpressSeconds}s"))
    }
}

data class Machine(
    val name: String,
    val detail: String,
    val verifiedAt: Long,
)

data class RunRecord(
    val id: String,
    val setupId: String,
    val title: String,
    val titleKey: String? = null,
    val timestamp: Long,
    val processed: Int,
    val good: Int,
    val waste: Int,
    val rework: Int,
    val note: String = "",
    val issue: Boolean,
)

data class ActiveRun(
    val id: String,
    val setupId: String,
    val quantity: Int,
    val processed: Int = 0,
    val jobReference: String = "",
    val phase: RunPhase = RunPhase.PREFLIGHT,
    val hasIssue: Boolean = false,
    val qcDone: Boolean = false,
    val firstPieceAttempts: Int = 0,
    val recipeIndex: Int = 0,
    val recipeComplete: Boolean = false,
    val recovered: Boolean = false,
    val timerRecovered: Boolean = false,
    val timerExpiredDuringRecovery: Boolean = false,
    val startedAt: Long = System.currentTimeMillis(),
)

data class AppPreferences(
    val locale: String = "en",
    val temperatureUnit: TemperatureUnit = TemperatureUnit.AUTO,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val onboardingStep: Int = 0,
)

data class LastStart(
    val setupId: String = "",
    val quantity: Int = 1,
)

data class PressBenchState(
    val destination: AppDestination = AppDestination.HOME,
    val setups: List<Setup> = emptyList(),
    val machines: List<Machine> = emptyList(),
    val history: List<RunRecord> = emptyList(),
    val activeRun: ActiveRun? = null,
    val preferences: AppPreferences = AppPreferences(),
    val lastStart: LastStart = LastStart(),
    val timerRemaining: Int = 0,
    val timerRunning: Boolean = false,
    val timerDeadline: Long? = null,
)
