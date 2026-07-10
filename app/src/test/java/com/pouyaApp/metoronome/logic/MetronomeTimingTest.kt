package com.pouyaApp.metoronome.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetronomeTimingTest {

    @Test
    fun sixEightAt120BpmHas500msBetweenEighthNotes() {
        val settings = MetronomeSettings(bpm = 120, beatsPerMeasure = 6, beatNote = BeatNote.EIGHTH)
        assertEquals(500.0, settings.tickIntervalMs(), 0.01)
    }

    @Test
    fun fourFourWithEighthSubdivisionsHalvesTickInterval() {
        val settings = MetronomeSettings(
            bpm = 120,
            beatsPerMeasure = 4,
            beatNote = BeatNote.QUARTER,
            subdivision = Subdivision.EIGHTH,
        )
        assertEquals(250.0, settings.tickIntervalMs(), 0.01)
        assertEquals(8, settings.ticksPerMeasure)
    }

    @Test
    fun sixEightDefaultAccentsBeatOneAndFour() {
        val settings = MetronomeSettings(
            bpm = 120,
            beatsPerMeasure = 6,
            beatNote = BeatNote.EIGHTH,
            accents = MetronomeSettings.defaultAccents(6, compoundSixEight = true),
        )
        assertTrue(settings.isAccentedBeat(0))
        assertFalse(settings.isAccentedBeat(1))
        assertFalse(settings.isAccentedBeat(2))
        assertTrue(settings.isAccentedBeat(3))
    }

    @Test
    fun accentSoundOnlyOnAccentedBeatStarts() {
        val settings = MetronomeSettings(
            bpm = 120,
            beatsPerMeasure = 6,
            beatNote = BeatNote.EIGHTH,
            accents = MetronomeSettings.defaultAccents(6, compoundSixEight = true),
        )
        assertEquals(BeatKind.ACCENT, TickSoundPolicy.resolve(settings, tickIndex = 0))
        assertEquals(BeatKind.REGULAR, TickSoundPolicy.resolve(settings, tickIndex = 1))
        assertEquals(BeatKind.ACCENT, TickSoundPolicy.resolve(settings, tickIndex = 3))
    }

    @Test
    fun sameAccentSoundUsesRegularClickForAllBeats() {
        val settings = MetronomeSettings(
            bpm = 120,
            beatsPerMeasure = 4,
            beatNote = BeatNote.QUARTER,
            sameAccentSound = true,
        )
        assertEquals(BeatKind.REGULAR, TickSoundPolicy.resolve(settings, tickIndex = 0))
    }

    @Test
    fun sevenEightGroupingAccentsStartOfEachGroup() {
        val accents = MetronomeSettings.accentsFromGroups(listOf(2, 2, 3))
        assertEquals(
            listOf(true, false, true, false, true, false, false),
            accents,
        )
    }

    @Test
    fun fourteenSixteenAdditiveGroupingMatchesScore() {
        val accents = MetronomeSettings.accentsFromGroups(listOf(3, 3, 4, 4))
        assertEquals(14, accents.size)
        // Group starts at indices 0, 3, 6, 10.
        assertTrue(accents[0])
        assertTrue(accents[3])
        assertTrue(accents[6])
        assertTrue(accents[10])
        assertFalse(accents[1])
        assertFalse(accents[9])
    }

    @Test
    fun groupingRoundTripsThroughAccents() {
        val groups = listOf(3, 2, 2)
        val accents = MetronomeSettings.accentsFromGroups(groups)
        assertEquals(groups, MetronomeSettings.groupsFromAccents(accents))
    }

    @Test
    fun asymmetricPresetProducesExpectedTotal() {
        val sevenEight = AsymmetricPresets.ALL.first { it.groups == listOf(2, 2, 3) }
        assertEquals(7, sevenEight.total)
        assertEquals("7/8 (2+2+3)", sevenEight.label)
    }
}
