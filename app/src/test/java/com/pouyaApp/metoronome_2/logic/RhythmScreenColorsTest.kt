package com.pouyaApp.metoronome_2.logic

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class RhythmScreenColorsTest {

    private val idle = Color(0xFF000000)

    @Test
    fun disabledReturnsIdleColor() {
        val color = RhythmScreenColors.resolve(
            enabled = false,
            isPlaying = true,
            kind = BeatKind.ACCENT,
            isBeatDownbeat = true,
            idle = idle,
        )
        assertEquals(idle, color)
    }

    @Test
    fun accentUsesGreenTone() {
        val color = RhythmScreenColors.resolve(
            enabled = true,
            isPlaying = true,
            kind = BeatKind.ACCENT,
            isBeatDownbeat = true,
            idle = idle,
        )
        assertEquals(Color(0xFF1B5E20), color)
    }

    @Test
    fun regularDownbeatUsesRegularTone() {
        val color = RhythmScreenColors.resolve(
            enabled = true,
            isPlaying = true,
            kind = BeatKind.REGULAR,
            isBeatDownbeat = true,
            idle = idle,
        )
        assertEquals(Color(0xFF15202B), color)
    }
}
