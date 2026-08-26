package com.goodusestudios.pressbench

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.goodusestudios.pressbench.data.PressBenchStore
import com.goodusestudios.pressbench.i18n.PressBenchStrings
import com.goodusestudios.pressbench.model.ActiveRun
import com.goodusestudios.pressbench.model.AppDestination
import com.goodusestudios.pressbench.model.AppPreferences
import com.goodusestudios.pressbench.model.LastStart
import com.goodusestudios.pressbench.model.Machine
import com.goodusestudios.pressbench.model.PressBenchState
import com.goodusestudios.pressbench.model.ProductionCounts
import com.goodusestudios.pressbench.model.RunPhase
import com.goodusestudios.pressbench.model.RunRecord
import com.goodusestudios.pressbench.model.Setup
import com.goodusestudios.pressbench.model.SetupStatus
import com.goodusestudios.pressbench.model.TemperatureUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PressBenchViewModel(application: Application) : AndroidViewModel(application) {
    val strings = PressBenchStrings.load(application)
    private val store = PressBenchStore(application)
    private val defaultLocale = PressBenchStrings.deviceLocale(strings.supportedLocales)

    var state by mutableStateOf(PressBenchState(preferences = AppPreferences(locale = defaultLocale)))
        private set
    var isLoaded by mutableStateOf(false)
        private set

    private var timerJob: Job? = null
    private var isClearing = false
    private val storeOperations = Channel<StoreOperation>(Channel.UNLIMITED)
    private val vibrator = application.getSystemService(Vibrator::class.java)
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 55)

    init {
        viewModelScope.launch {
            for (operation in storeOperations) {
                when (operation) {
                    is StoreOperation.Save -> runCatching { store.save(operation.snapshot) }
                    is StoreOperation.Clear -> runCatching { store.clear(defaultLocale) }
                        .onSuccess { operation.result.complete(it) }
                        .onFailure { operation.result.completeExceptionally(it) }
                }
            }
        }
        viewModelScope.launch {
            state = store.load(defaultLocale)
            isLoaded = true
        }
    }

    fun navigate(destination: AppDestination) = update { copy(destination = destination) }

    fun finishOnboarding() = update {
        copy(preferences = preferences.copy(onboardingStep = 2), destination = AppDestination.HOME)
    }

    fun setOnboardingStep(step: Int) = update {
        copy(preferences = preferences.copy(onboardingStep = step.coerceIn(0, 2)))
    }

    fun startRun(setupId: String, quantity: Int, jobReference: String) {
        val setup = state.setups.find { it.id == setupId } ?: return
        val safeQuantity = quantity.coerceIn(1, 999_999)
        stopTimer(saveState = false)
        update {
            copy(
                destination = AppDestination.HOME,
                activeRun = ActiveRun(
                    id = "r${System.currentTimeMillis()}",
                    setupId = setupId,
                    quantity = safeQuantity,
                    jobReference = jobReference.trim(),
                ),
                lastStart = LastStart(setupId, safeQuantity),
                timerRemaining = setup.recipeStages().firstOrNull()?.durationSeconds ?: 0,
                timerRunning = false,
                timerDeadline = null,
            )
        }
    }

    fun confirmInstructions() {
        val run = state.activeRun ?: return
        val setup = setup(run.setupId) ?: return
        stopTimer(saveState = false)
        update {
            copy(
                activeRun = run.copy(
                    phase = RunPhase.FIRST_PIECE,
                    recipeIndex = 0,
                    recipeComplete = false,
                    recovered = false,
                    timerRecovered = false,
                    timerExpiredDuringRecovery = false,
                ),
                timerRemaining = setup.recipeStages().firstOrNull()?.durationSeconds ?: 0,
                timerRunning = false,
                timerDeadline = null,
            )
        }
    }

    fun toggleTimer() {
        val run = state.activeRun ?: return
        if (run.timerExpiredDuringRecovery) {
            state = state.copy(activeRun = run.copy(timerRecovered = false, timerExpiredDuringRecovery = false, recovered = false))
            advanceRecipeStage()
        } else if (state.timerRunning) stopTimer() else startTimer()
    }

    private fun startTimer() {
        val run = state.activeRun ?: return
        val setup = setup(run.setupId) ?: return
        val stage = setup.recipeStages().getOrNull(run.recipeIndex) ?: return
        if (stage.durationSeconds <= 0) {
            advanceRecipeStage()
            return
        }
        val remaining = state.timerRemaining.takeIf { it > 0 } ?: stage.durationSeconds
        val deadline = System.currentTimeMillis() + remaining * 1000L
        update {
            copy(
                activeRun = run.copy(recovered = false, timerRecovered = false, timerExpiredDuringRecovery = false),
                timerRemaining = remaining,
                timerRunning = true,
                timerDeadline = deadline,
            )
        }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && state.timerRunning) {
                val seconds = (((state.timerDeadline ?: deadline) - System.currentTimeMillis() + 999L) / 1000L)
                    .coerceAtLeast(0L).toInt()
                if (seconds != state.timerRemaining) update { copy(timerRemaining = seconds) }
                if (seconds <= 0) {
                    signal(Feedback.TIMER)
                    state = state.copy(timerRemaining = 0, timerRunning = false, timerDeadline = null)
                    persist(state)
                    timerJob = null
                    advanceRecipeStage()
                    break
                }
                delay(250)
            }
        }
    }

    fun stopTimer(saveState: Boolean = true) {
        timerJob?.cancel()
        timerJob = null
        state = state.copy(timerRunning = false, timerDeadline = null)
        if (saveState) persist(state)
    }

    fun advanceRecipeStage() {
        val run = state.activeRun ?: return
        val setup = setup(run.setupId) ?: return
        stopTimer(saveState = false)
        val stages = setup.recipeStages()
        if (run.recipeIndex < stages.lastIndex) {
            val next = run.recipeIndex + 1
            update {
                copy(
                    activeRun = run.copy(recipeIndex = next, recovered = false, timerRecovered = false, timerExpiredDuringRecovery = false),
                    timerRemaining = stages[next].durationSeconds,
                    timerRunning = false,
                    timerDeadline = null,
                )
            }
        } else {
            update {
                copy(
                    activeRun = run.copy(recipeComplete = true, recovered = false, timerRecovered = false, timerExpiredDuringRecovery = false),
                    timerRemaining = 0,
                    timerRunning = false,
                    timerDeadline = null,
                )
            }
        }
    }

    fun firstPiece(outcome: FirstPieceOutcome) {
        val run = state.activeRun ?: return
        stopTimer(saveState = false)
        val attempts = run.firstPieceAttempts + 1
        val next = when (outcome) {
            FirstPieceOutcome.PASS -> run.copy(
                processed = maxOf(1, run.processed),
                firstPieceAttempts = attempts,
                phase = if (maxOf(1, run.processed) >= run.quantity) RunPhase.RESULT_PENDING else RunPhase.PRODUCTION_READY,
                recovered = false,
            )
            FirstPieceOutcome.ADJUST -> run.copy(
                firstPieceAttempts = attempts, hasIssue = true, phase = RunPhase.PREFLIGHT,
                recipeIndex = 0, recipeComplete = false, recovered = false,
            )
            FirstPieceOutcome.STOP -> run.copy(
                firstPieceAttempts = attempts, hasIssue = true, phase = RunPhase.RESULT_PENDING, recovered = false,
            )
        }
        update { copy(activeRun = next) }
    }

    fun startProduction() = mutateRun { run ->
        run.copy(phase = if (run.processed >= run.quantity) RunPhase.RESULT_PENDING else RunPhase.RUNNING, recovered = false)
    }

    fun completeCycle() {
        val run = state.activeRun ?: return
        val nextCount = (run.processed + 1).coerceAtMost(run.quantity)
        signal(Feedback.COUNT)
        if (run.processed < 10 && nextCount >= 10 && !run.qcDone) signal(Feedback.QC)
        mutateRun {
            it.copy(
                processed = nextCount,
                phase = if (nextCount >= it.quantity) RunPhase.RESULT_PENDING else it.phase,
                recovered = false,
            )
        }
    }

    fun undoCycle() = mutateRun {
        val floor = if (it.firstPieceAttempts > 0) 1 else 0
        it.copy(processed = (it.processed - 1).coerceAtLeast(floor), recovered = false)
    }

    fun pauseRun() = mutateRun { it.copy(phase = RunPhase.PAUSED, recovered = false) }
    fun resumeRun() = mutateRun {
        it.copy(phase = if (it.processed >= it.quantity) RunPhase.RESULT_PENDING else RunPhase.RUNNING, recovered = false)
    }

    fun recordQc(pass: Boolean) = mutateRun {
        if (pass) it.copy(qcDone = true, recovered = false)
        else it.copy(hasIssue = true, phase = RunPhase.PAUSED, recovered = false)
    }

    fun endRun() {
        stopTimer(saveState = false)
        mutateRun {
            it.copy(
                hasIssue = it.hasIssue || it.processed < it.quantity,
                phase = RunPhase.RESULT_PENDING,
                recovered = false,
            )
        }
    }

    fun cancelRun() {
        if (state.activeRun == null) return
        stopTimer(saveState = false)
        update {
            copy(
                destination = AppDestination.HOME,
                activeRun = null,
                timerRemaining = 0,
                timerRunning = false,
                timerDeadline = null,
            )
        }
    }

    fun finalizeRun(waste: Int, rework: Int, note: String) {
        val run = state.activeRun ?: return
        val setup = setup(run.setupId) ?: return
        val counts = ProductionCounts.normalized(run.processed, waste, rework)
        val issue = counts.waste > 0 || counts.rework > 0 || run.hasIssue
        val record = RunRecord(
            id = "b${System.currentTimeMillis()}", setupId = setup.id,
            title = setup.title, titleKey = setup.titleKey, timestamp = System.currentTimeMillis(),
            processed = counts.processed, good = counts.firstPassGood, waste = counts.waste, rework = counts.rework,
            note = note.trim(), issue = issue,
        )
        val updatedSetups = state.setups.map {
            if (it.id != setup.id || issue || counts.firstPassGood != counts.processed || counts.firstPassGood <= 0) it
            else {
                val runs = it.provenRuns + 1
                it.copy(
                    provenRuns = runs,
                    provenPieces = it.provenPieces + counts.firstPassGood,
                    status = if (runs >= 2) SetupStatus.PROVEN else it.status,
                )
            }
        }
        signal(Feedback.DONE)
        stopTimer(saveState = false)
        update {
            copy(
                destination = AppDestination.RUNS,
                history = listOf(record) + history,
                setups = updatedSetups,
                activeRun = null,
                timerRemaining = 0,
                timerRunning = false,
                timerDeadline = null,
            )
        }
    }

    fun toggleFavorite(id: String) = update {
        copy(setups = setups.map { if (it.id == id) it.copy(favorite = !it.favorite) else it })
    }

    fun addSetup(setup: Setup) = update {
        copy(setups = listOf(setup.copy(id = "s${System.currentTimeMillis()}")) + setups, destination = AppDestination.SETUPS)
    }

    fun addMachine(name: String, detail: String): Boolean {
        val cleanName = name.trim()
        val cleanDetail = detail.trim()
        if (cleanName.isBlank() || cleanDetail.isBlank()) return false
        if (state.machines.any { it.name.equals(cleanName, ignoreCase = true) }) return false
        update {
            copy(
                machines = listOf(Machine(cleanName, cleanDetail, System.currentTimeMillis())) + machines,
                destination = AppDestination.MACHINES,
            )
        }
        return true
    }

    fun duplicateSetup(id: String) {
        val source = setup(id) ?: return
        addSetup(source.copy(status = SetupStatus.TRIAL, favorite = false, provenRuns = 0, provenPieces = 0, checkedAt = System.currentTimeMillis()))
    }

    fun setLocale(locale: String) = updatePreferences { copy(locale = PressBenchStrings.normalizeLocale(locale, strings.supportedLocales)) }
    fun setTemperatureUnit(unit: TemperatureUnit) = updatePreferences { copy(temperatureUnit = unit) }
    fun setSound(enabled: Boolean) = updatePreferences { copy(soundEnabled = enabled) }
    fun setVibration(enabled: Boolean) = updatePreferences { copy(vibrationEnabled = enabled) }
    fun clearLocalData() {
        if (isClearing) return
        isClearing = true
        timerJob?.cancel()
        timerJob = null
        state = PressBenchState(preferences = AppPreferences(locale = defaultLocale))
        viewModelScope.launch {
            try {
                val result = CompletableDeferred<PressBenchState>()
                storeOperations.send(StoreOperation.Clear(result))
                state = result.await()
            } finally {
                isClearing = false
            }
        }
    }

    fun setup(id: String): Setup? = state.setups.find { it.id == id }

    private fun mutateRun(transform: (ActiveRun) -> ActiveRun) {
        val run = state.activeRun ?: return
        update { copy(activeRun = transform(run)) }
    }

    private fun updatePreferences(transform: AppPreferences.() -> AppPreferences) = update {
        copy(preferences = preferences.transform())
    }

    private fun update(transform: PressBenchState.() -> PressBenchState) {
        if (isClearing) return
        state = state.transform()
        persist(state)
    }

    private fun persist(snapshot: PressBenchState) {
        if (isClearing) return
        storeOperations.trySend(StoreOperation.Save(snapshot))
    }

    private fun signal(feedback: Feedback) {
        val prefs = state.preferences
        if (prefs.vibrationEnabled) {
            val pattern = when (feedback) {
                Feedback.COUNT -> longArrayOf(0, 18)
                Feedback.TIMER -> longArrayOf(0, 90, 45, 90)
                Feedback.DONE -> longArrayOf(0, 60, 35, 110)
                Feedback.QC -> longArrayOf(0, 45, 35, 45)
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                else @Suppress("DEPRECATION") vibrator.vibrate(pattern, -1)
            }
        }
        if (prefs.soundEnabled && feedback != Feedback.COUNT) {
            val tone = when (feedback) {
                Feedback.DONE -> ToneGenerator.TONE_PROP_ACK
                Feedback.QC -> ToneGenerator.TONE_PROP_BEEP2
                else -> ToneGenerator.TONE_PROP_BEEP
            }
            runCatching { toneGenerator.startTone(tone, 170) }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        storeOperations.close()
        toneGenerator.release()
        super.onCleared()
    }
}

enum class FirstPieceOutcome { PASS, ADJUST, STOP }
private enum class Feedback { COUNT, TIMER, DONE, QC }
private sealed interface StoreOperation {
    data class Save(val snapshot: PressBenchState) : StoreOperation
    data class Clear(val result: CompletableDeferred<PressBenchState>) : StoreOperation
}
