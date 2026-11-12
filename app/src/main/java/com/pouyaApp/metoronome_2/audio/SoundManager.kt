package com.pouyaApp.metoronome_2.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import com.pouyaApp.metoronome_2.R
import com.pouyaApp.metoronome_2.logic.BeatKind
import java.util.concurrent.atomic.AtomicInteger

class SoundManager(context: Context) {

    private val appContext = context.applicationContext
    private val soundPool: SoundPool
    private val pendingLoads = AtomicInteger(0)
    private var currentPackIndex = 0
    private var volume = 1f

    private data class LoadedPack(
        val regularId: Int,
        val accentId: Int,
    )

    private val soundPairs = listOf(
        R.raw.beep1 to R.raw.beep2,
        R.raw.cowbell1 to R.raw.cowbell2,
        R.raw.hihat1 to R.raw.hihat2,
        R.raw.ridebell1 to R.raw.ridebell2,
        R.raw.metro0 to R.raw.metro1,
        R.raw.stick1 to R.raw.stick2,
        R.raw.tick to R.raw.tock,
    )

    private val loadedPacks = mutableListOf<LoadedPack>()

    val isReady: Boolean
        get() = pendingLoads.get() == 0 && loadedPacks.isNotEmpty()

    init {
        val attributesBuilder = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            attributesBuilder.setFlags(AudioAttributes.FLAG_LOW_LATENCY)
        }
        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attributesBuilder.build())
            .build()
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                pendingLoads.decrementAndGet()
            }
        }
        preloadAllPacks()
    }

    fun setVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
    }

    fun cycleSoundPack(): String {
        currentPackIndex = (currentPackIndex + 1) % soundPairs.size
        return currentPackName(appContext)
    }

    fun currentPackIndex(): Int = currentPackIndex

    fun currentPackName(context: Context): String {
        val names = context.resources.getStringArray(R.array.sound_pack_names)
        return names[currentPackIndex]
    }

    fun play(kind: BeatKind, isSubdivision: Boolean) {
        if (!isReady) return
        val pack = loadedPacks.getOrNull(currentPackIndex) ?: return
        val soundId = when (kind) {
            BeatKind.ACCENT -> pack.accentId
            BeatKind.REGULAR -> pack.regularId
            BeatKind.SILENT -> return
        }
        val level = if (isSubdivision) volume * SUBDIVISION_VOLUME_SCALE else volume
        if (level <= 0f) return
        soundPool.play(soundId, level, level, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }

    private fun preloadAllPacks() {
        pendingLoads.set(soundPairs.size * 2)
        loadedPacks.clear()
        soundPairs.forEach { (regular, accent) ->
            val regularId = soundPool.load(appContext, regular, 1)
            val accentId = soundPool.load(appContext, accent, 1)
            loadedPacks.add(LoadedPack(regularId, accentId))
        }
    }

    companion object {
        private const val SUBDIVISION_VOLUME_SCALE = 0.72f
    }
}
