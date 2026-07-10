package com.pouyaApp.metoronome.logic

import androidx.compose.ui.graphics.Color

object RhythmScreenColors {

    private val Accent = Color(0xFF1B5E20)
    private val Regular = Color(0xFF15202B)
    private val SubTick = Color(0xFF121820)

    fun resolve(
        enabled: Boolean,
        isPlaying: Boolean,
        kind: BeatKind,
        isBeatDownbeat: Boolean,
        idle: Color,
    ): Color {
        if (!enabled || !isPlaying) return idle
        return when {
            kind == BeatKind.ACCENT -> Accent
            isBeatDownbeat -> Regular
            else -> SubTick
        }
    }
}
