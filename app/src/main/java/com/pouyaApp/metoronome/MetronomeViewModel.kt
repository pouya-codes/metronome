package com.pouyaApp.metoronome

import android.app.Application
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import com.pouyaApp.metoronome.audio.FlashManager
import com.pouyaApp.metoronome.audio.MetronomeEngine
import com.pouyaApp.metoronome.audio.SoundManager
import com.pouyaApp.metoronome.logic.AsymmetricPresets
import com.pouyaApp.metoronome.logic.BeatKind
import com.pouyaApp.metoronome.logic.BeatNote
import com.pouyaApp.metoronome.logic.MeterPresets
import com.pouyaApp.metoronome.logic.MetronomeSettings
import com.pouyaApp.metoronome.logic.MetronomeViewMode
import com.pouyaApp.metoronome.logic.Subdivision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MetronomeUiState(
    val bpm: Int = 120,
    val beatsPerMeasure: Int = 6,
    val beatNote: BeatNote = BeatNote.EIGHTH,
    val subdivision: Subdivision = Subdivision.NONE,
    val accents: List<Boolean> = MetronomeSettings.defaultAccents(6, compoundSixEight = true),
    val sameAccentSound: Boolean = false,
    val volume: Float = 1f,
    val isPlaying: Boolean = false,
    val flashEnabled: Boolean = false,
    val flashAvailable: Boolean = false,
    val screenColorEnabled: Boolean = false,
    val viewMode: MetronomeViewMode = MetronomeViewMode.PHYSICAL,
    val lastBeatKind: BeatKind = BeatKind.REGULAR,
    val beatIndex: Int = 0,
    val beatCounter: Long = 0L,
    val isBeatDownbeat: Boolean = false,
    val soundPackIndex: Int = 0,
    val soundPackName: String = "",
) {
    val timeSignatureLabel: String
        get() = "$beatsPerMeasure/${beatNote.symbol}"

    /** Additive grouping derived from the accent pattern, e.g. 7/8 accented on 1,3,5 → (2,2,3). */
    val groups: List<Int>
        get() = MetronomeSettings.groupsFromAccents(accents)

    val isAsymmetric: Boolean
        get() = groups.distinct().size > 1

    fun toSettings(): MetronomeSettings = MetronomeSettings(
        bpm = bpm,
        beatsPerMeasure = beatsPerMeasure,
        beatNote = beatNote,
        subdivision = subdivision,
        accents = accents,
        sameAccentSound = sameAccentSound,
    )
}

class MetronomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)

    private val soundManager = SoundManager(application)
    private val flashManager = FlashManager(application)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<MetronomeUiState> = _uiState.asStateFlow()

    private val engine = MetronomeEngine { tickIndex, beatIndex, kind ->
        val settings = _uiState.value.toSettings()
        val isSubdivision = !settings.isBeatDownbeat(tickIndex)
        soundManager.play(kind, isSubdivision)
        if (_uiState.value.flashEnabled && settings.isBeatDownbeat(tickIndex)) {
            flashManager.pulse()
        }
        val isBeat = settings.isBeatDownbeat(tickIndex)
        mainHandler.post {
            _uiState.update {
                it.copy(
                    beatIndex = beatIndex,
                    isBeatDownbeat = isBeat,
                    beatCounter = if (isBeat) it.beatCounter + 1 else it.beatCounter,
                    lastBeatKind = kind,
                )
            }
        }
    }

    init {
        soundManager.setVolume(_uiState.value.volume)
        updateEngineSettings()
    }

    fun setBpm(value: Int) {
        val bpm = value.coerceIn(MIN_BPM, MAX_BPM)
        _uiState.update { it.copy(bpm = bpm) }
        updateEngineSettings()
    }

    fun incrementBpm(step: Int = 1) = setBpm(_uiState.value.bpm + step)

    fun decrementBpm(step: Int = 1) = setBpm(_uiState.value.bpm - step)

    fun setVolume(value: Float) {
        val volume = value.coerceIn(0f, 1f)
        soundManager.setVolume(volume)
        _uiState.update { it.copy(volume = volume) }
    }

    fun setBeatsPerMeasure(beats: Int) {
        val clamped = beats.coerceIn(MIN_BEATS, MAX_BEATS)
        _uiState.update { state ->
            val accents = resizeAccents(state.accents, clamped)
            state.copy(beatsPerMeasure = clamped, accents = accents)
        }
        updateEngineSettings()
    }

    fun setBeatNote(note: BeatNote) {
        _uiState.update { it.copy(beatNote = note) }
        updateEngineSettings()
    }

    fun setSubdivision(subdivision: Subdivision) {
        _uiState.update { it.copy(subdivision = subdivision) }
        updateEngineSettings()
    }

    fun toggleAccent(beatIndex: Int) {
        _uiState.update { state ->
            if (beatIndex !in state.accents.indices) return@update state
            val updated = state.accents.toMutableList()
            updated[beatIndex] = !updated[beatIndex]
            state.copy(accents = updated)
        }
        updateEngineSettings()
    }

    fun applyPreset(preset: MeterPresets.Preset) {
        _uiState.update {
            it.copy(
                beatsPerMeasure = preset.beats,
                beatNote = preset.note,
                bpm = preset.bpm,
                accents = preset.accents,
            )
        }
        updateEngineSettings()
    }

    fun applyGrouping(groups: List<Int>, note: BeatNote? = null) {
        val sanitized = groups.filter { it > 0 }
        if (sanitized.isEmpty()) return
        val total = sanitized.sum().coerceAtMost(MAX_BEATS)
        _uiState.update {
            it.copy(
                beatsPerMeasure = total,
                beatNote = note ?: it.beatNote,
                accents = MetronomeSettings.accentsFromGroups(sanitized),
            )
        }
        updateEngineSettings()
    }

    fun applyAsymmetricPreset(preset: AsymmetricPresets.Grouping) {
        applyGrouping(preset.groups, preset.note)
    }

    fun changeGroupSize(index: Int, delta: Int) {
        val groups = _uiState.value.groups.toMutableList()
        if (index !in groups.indices) return
        val newSize = groups[index] + delta
        if (newSize < 1) {
            if (groups.size <= 1) return
            groups.removeAt(index)
        } else {
            groups[index] = newSize.coerceAtMost(MAX_GROUP_SIZE)
        }
        applyGrouping(groups)
    }

    fun addGroup() {
        val groups = _uiState.value.groups.toMutableList()
        if (groups.sum() + DEFAULT_GROUP_SIZE > MAX_BEATS) return
        groups.add(DEFAULT_GROUP_SIZE)
        applyGrouping(groups)
    }

    fun toggleSameAccentSound(enabled: Boolean) {
        _uiState.update { it.copy(sameAccentSound = enabled) }
        updateEngineSettings()
    }

    fun toggleFlash(enabled: Boolean) {
        if (enabled && !flashManager.isAvailable) return
        _uiState.update { it.copy(flashEnabled = enabled) }
    }

    fun toggleScreenColor(enabled: Boolean) {
        _uiState.update { it.copy(screenColorEnabled = enabled) }
    }

    fun setViewMode(mode: MetronomeViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun cycleSoundPack() {
        val name = soundManager.cycleSoundPack()
        _uiState.update {
            it.copy(soundPackIndex = soundManager.currentPackIndex(), soundPackName = name)
        }
    }

    fun togglePlayback() {
        if (_uiState.value.isPlaying) {
            engine.stop()
            _uiState.update {
                it.copy(
                    isPlaying = false,
                    beatIndex = 0,
                    beatCounter = 0L,
                    isBeatDownbeat = false,
                    lastBeatKind = BeatKind.REGULAR,
                )
            }
        } else {
            updateEngineSettings()
            engine.start()
            _uiState.update { it.copy(isPlaying = true, beatCounter = 0L) }
        }
    }

    fun markWelcomeShown() {
        prefs.edit().putBoolean(KEY_WELCOME_SHOWN, true).apply()
    }

    fun shouldShowRatePrompt(): Boolean {
        return !prefs.getBoolean(KEY_WELCOME_SHOWN, false)
    }

    override fun onCleared() {
        engine.release()
        soundManager.release()
        flashManager.release()
        super.onCleared()
    }

    private fun createInitialState(): MetronomeUiState {
        val context = getApplication<Application>()
        return MetronomeUiState(
            flashAvailable = flashManager.isAvailable,
            soundPackName = soundManager.currentPackName(context),
        )
    }

    private fun updateEngineSettings() {
        engine.updateSettings(_uiState.value.toSettings())
    }

    private fun resizeAccents(current: List<Boolean>, newSize: Int): List<Boolean> {
        if (newSize == current.size) return current
        return List(newSize) { index ->
            current.getOrElse(index) { index == 0 }
        }
    }

    companion object {
        private const val PREFS_NAME = "metronome_prefs"
        private const val KEY_WELCOME_SHOWN = "welcomeScreenShown"
        const val MIN_BPM = 30
        const val MAX_BPM = 300
        const val MIN_BEATS = 1
        const val MAX_BEATS = 16
        const val MAX_GROUP_SIZE = 7
        private const val DEFAULT_GROUP_SIZE = 2
    }
}
