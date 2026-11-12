package com.pouyaApp.metoronome_2.logic

/**
 * Standard metronome timing model (same idea as metronome-online.org):
 *
 * - BPM = how many beat-note values occur per minute (the denominator of the time signature).
 * - Each measure has [beatsPerMeasure] beats (the numerator).
 * - [subdivision] adds extra clicks between beats (2 = eighth-note feel on quarter beats, etc.).
 * - [accents] marks which beats in the bar get a strong click.
 *
 * Example 6/8 at 120 BPM: 6 eighth-note clicks per bar, accents on 1 and 4 →
 * "ONE-two-three, FOUR-five-six".
 */
enum class MetronomeViewMode {
    PHYSICAL,
    DIGITAL,
}

enum class BeatNote(val symbol: Int) {
    HALF(2),
    QUARTER(4),
    EIGHTH(8),
    SIXTEENTH(16),
}

enum class Subdivision(val ticksPerBeat: Int) {
    NONE(1),
    EIGHTH(2),
    TRIPLET(3),
    SIXTEENTH(4),
}

data class MetronomeSettings(
    val bpm: Int = 120,
    val beatsPerMeasure: Int = 6,
    val beatNote: BeatNote = BeatNote.EIGHTH,
    val subdivision: Subdivision = Subdivision.NONE,
    val accents: List<Boolean> = defaultAccents(6, compoundSixEight = true),
    val sameAccentSound: Boolean = false,
) {
    val ticksPerMeasure: Int
        get() = beatsPerMeasure * subdivision.ticksPerBeat

    fun tickIntervalMs(): Double {
        return 60_000.0 / bpm.coerceAtLeast(1) / subdivision.ticksPerBeat
    }

    fun beatIndexForTick(tickIndex: Int): Int {
        return (tickIndex / subdivision.ticksPerBeat) % beatsPerMeasure
    }

    fun isBeatDownbeat(tickIndex: Int): Boolean {
        return tickIndex % subdivision.ticksPerBeat == 0
    }

    fun isAccentedBeat(beatIndex: Int): Boolean {
        return accents.getOrElse(beatIndex) { false }
    }

    companion object {
        fun defaultAccents(beats: Int, compoundSixEight: Boolean = false): List<Boolean> {
            return when {
                compoundSixEight && beats == 6 ->
                    listOf(true, false, false, true, false, false)
                beats == 4 -> listOf(true, false, false, false)
                beats == 3 -> listOf(true, false, false)
                beats == 2 -> listOf(true, false)
                else -> List(beats) { it == 0 }
            }
        }

        /**
         * Builds an accent pattern for an additive/asymmetric meter. The first beat of every
         * group is accented, e.g. groups (2,2,3) → 7 beats accented as [T,F,T,F,T,F,F].
         */
        fun accentsFromGroups(groups: List<Int>): List<Boolean> {
            val total = groups.sum()
            if (total <= 0) return listOf(true)
            val starts = HashSet<Int>()
            var index = 0
            for (size in groups) {
                if (size <= 0) continue
                starts.add(index)
                index += size
            }
            return List(total) { it in starts }
        }

        /**
         * Reads the additive grouping back out of an accent pattern: each accented beat starts a
         * new group, e.g. [T,F,T,F,T,F,F] → (2,2,3). Beat 0 always starts a group.
         */
        fun groupsFromAccents(accents: List<Boolean>): List<Int> {
            if (accents.isEmpty()) return emptyList()
            val starts = buildList {
                add(0)
                for (i in 1 until accents.size) {
                    if (accents[i]) add(i)
                }
            }.distinct()
            return buildList {
                for (i in starts.indices) {
                    val start = starts[i]
                    val end = if (i + 1 < starts.size) starts[i + 1] else accents.size
                    add(end - start)
                }
            }
        }
    }
}

object MeterPresets {
    data class Preset(
        val label: String,
        val beats: Int,
        val note: BeatNote,
        val bpm: Int = 120,
        val accents: List<Boolean>,
    )

    val ALL = listOf(
        Preset("2/4", 2, BeatNote.QUARTER, 120, MetronomeSettings.defaultAccents(2)),
        Preset("3/4", 3, BeatNote.QUARTER, 120, MetronomeSettings.defaultAccents(3)),
        Preset("4/4", 4, BeatNote.QUARTER, 120, MetronomeSettings.defaultAccents(4)),
        Preset("6/8", 6, BeatNote.EIGHTH, 120, MetronomeSettings.defaultAccents(6, compoundSixEight = true)),
        Preset("9/8", 9, BeatNote.EIGHTH, 120, listOf(true, false, false, true, false, false, true, false, false)),
        Preset("12/8", 12, BeatNote.EIGHTH, 120, listOf(true, false, false, true, false, false, true, false, false, true, false, false)),
    )
}

/** Common additive / asymmetric-meter groupings (7/8, 5/8, 14/16, ...). */
object AsymmetricPresets {
    data class Grouping(
        val groups: List<Int>,
        val note: BeatNote,
    ) {
        val total: Int get() = groups.sum()
        val label: String get() = "$total/${note.symbol} (${groups.joinToString("+")})"
    }

    val ALL = listOf(
        Grouping(listOf(2, 3), BeatNote.EIGHTH),
        Grouping(listOf(3, 2), BeatNote.EIGHTH),
        Grouping(listOf(2, 2, 3), BeatNote.EIGHTH),
        Grouping(listOf(3, 2, 2), BeatNote.EIGHTH),
        Grouping(listOf(2, 3, 2), BeatNote.EIGHTH),
        Grouping(listOf(3, 3, 2), BeatNote.EIGHTH),
        Grouping(listOf(2, 2, 2, 3), BeatNote.EIGHTH),
        Grouping(listOf(3, 3, 3, 2), BeatNote.EIGHTH),
        Grouping(listOf(3, 3, 4, 4), BeatNote.SIXTEENTH),
    )
}

/** Italian tempo markings, matching the labels shown on metronome-online.org. */
object TempoMarkings {
    fun forBpm(bpm: Int): String = when {
        bpm < 60 -> "Largo"
        bpm < 66 -> "Larghetto"
        bpm < 76 -> "Adagio"
        bpm < 108 -> "Andante"
        bpm < 120 -> "Moderato"
        bpm < 168 -> "Allegro"
        bpm < 200 -> "Presto"
        else -> "Prestissimo"
    }
}

object TickSoundPolicy {
    fun resolve(settings: MetronomeSettings, tickIndex: Int): BeatKind {
        if (!settings.isBeatDownbeat(tickIndex)) {
            return BeatKind.REGULAR
        }
        val beatIndex = settings.beatIndexForTick(tickIndex)
        val accented = settings.isAccentedBeat(beatIndex)
        return when {
            settings.sameAccentSound -> BeatKind.REGULAR
            accented -> BeatKind.ACCENT
            else -> BeatKind.REGULAR
        }
    }
}
