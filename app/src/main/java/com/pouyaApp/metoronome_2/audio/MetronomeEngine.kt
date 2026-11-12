package com.pouyaApp.metoronome_2.audio

import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.SystemClock
import com.pouyaApp.metoronome_2.logic.BeatKind
import com.pouyaApp.metoronome_2.logic.MetronomeSettings
import com.pouyaApp.metoronome_2.logic.TickSoundPolicy

/**
 * Lightweight native scheduler — no Pure Data / libpd.
 * Uses a dedicated high-priority thread and [Handler.postAtTime] for stable timing.
 */
class MetronomeEngine(
    private val onTick: (tickIndex: Int, beatIndex: Int, kind: BeatKind) -> Unit,
) {
    private val thread = HandlerThread("MetronomeEngine", Process.THREAD_PRIORITY_URGENT_AUDIO).apply { start() }
    private val handler = Handler(thread.looper)

    @Volatile
    private var running = false

    @Volatile
    private var settings = MetronomeSettings()

    private var tickIndex = 0
    private var nextTickUptimeMs = 0L

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!running) return

            val snapshot = settings
            val intervalMs = snapshot.tickIntervalMs().toLong().coerceAtLeast(1L)

            val beatIndex = snapshot.beatIndexForTick(tickIndex)
            val kind = TickSoundPolicy.resolve(snapshot, tickIndex)
            onTick(tickIndex, beatIndex, kind)

            tickIndex = (tickIndex + 1) % snapshot.ticksPerMeasure.coerceAtLeast(1)
            nextTickUptimeMs += intervalMs

            val now = SystemClock.uptimeMillis()
            if (nextTickUptimeMs <= now) {
                nextTickUptimeMs = now + intervalMs
            }
            handler.postAtTime(this, nextTickUptimeMs)
        }
    }

    fun updateSettings(newSettings: MetronomeSettings) {
        settings = newSettings
    }

    fun start() {
        if (running) return
        running = true
        tickIndex = 0
        nextTickUptimeMs = SystemClock.uptimeMillis()
        handler.post(tickRunnable)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tickRunnable)
        tickIndex = 0
    }

    fun release() {
        stop()
        thread.quitSafely()
    }

    val isRunning: Boolean
        get() = running
}
