package com.pouyaApp.metoronome.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pouyaApp.metoronome.MetronomeUiState
import com.pouyaApp.metoronome.MetronomeViewModel
import com.pouyaApp.metoronome.R
import com.pouyaApp.metoronome.logic.AsymmetricPresets
import com.pouyaApp.metoronome.logic.BeatKind
import com.pouyaApp.metoronome.logic.BeatNote
import com.pouyaApp.metoronome.logic.MeterPresets
import com.pouyaApp.metoronome.logic.MetronomeViewMode
import com.pouyaApp.metoronome.logic.RhythmScreenColors
import com.pouyaApp.metoronome.logic.Subdivision
import com.pouyaApp.metoronome.logic.TempoMarkings
import kotlin.math.PI
import kotlin.math.cos

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MetronomeScreen(
    state: MetronomeUiState,
    onBpmChanged: (Int) -> Unit,
    onIncrement: (Int) -> Unit,
    onDecrement: (Int) -> Unit,
    onBeatsChanged: (Int) -> Unit,
    onBeatNoteSelected: (BeatNote) -> Unit,
    onSubdivisionSelected: (Subdivision) -> Unit,
    onAccentToggled: (Int) -> Unit,
    onPresetSelected: (MeterPresets.Preset) -> Unit,
    onAsymmetricPreset: (AsymmetricPresets.Grouping) -> Unit,
    onGroupSizeChanged: (Int, Int) -> Unit,
    onAddGroup: () -> Unit,
    onSameAccentChanged: (Boolean) -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onFlashChanged: (Boolean) -> Unit,
    onScreenColorChanged: (Boolean) -> Unit,
    onViewModeChanged: (MetronomeViewMode) -> Unit,
    onTogglePlayback: () -> Unit,
    onCycleSound: () -> Unit,
    onOpenKookyar: () -> Unit,
) {
    val beatNoteLabels = stringArrayResource(R.array.beat_notes).toList()
    val subdivisionLabels = stringArrayResource(R.array.subdivisions).toList()

    val beatScale by animateFloatAsState(
        targetValue = if (state.isPlaying && state.isBeatDownbeat) 1.12f else 1f,
        label = "beatScale",
    )
    val beatColor = when (state.lastBeatKind) {
        BeatKind.ACCENT -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.secondary
    }
    val rhythmBackground by animateColorAsState(
        targetValue = RhythmScreenColors.resolve(
            enabled = state.screenColorEnabled,
            isPlaying = state.isPlaying,
            kind = state.lastBeatKind,
            isBeatDownbeat = state.isBeatDownbeat,
            idle = MaterialTheme.colorScheme.background,
        ),
        animationSpec = tween(durationMillis = 100),
        label = "rhythmBackground",
    )
    val topBarColor by animateColorAsState(
        targetValue = if (state.screenColorEnabled && state.isPlaying) {
            rhythmBackground.copy(alpha = 0.92f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 100),
        label = "topBarColor",
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarColor),
            )
        },
        containerColor = rhythmBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BpmCard(
                bpm = state.bpm,
                timeSignature = state.timeSignatureLabel,
                tempoMarking = TempoMarkings.forBpm(state.bpm),
                onIncrement = { onIncrement(1) },
                onDecrement = { onDecrement(1) },
                onBpmChanged = onBpmChanged,
            )

            Button(
                onClick = onTogglePlayback,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isPlaying) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                ),
            ) {
                Text(
                    text = if (state.isPlaying) {
                        stringResource(R.string.stop)
                    } else {
                        stringResource(R.string.start)
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            ModeToggle(
                mode = state.viewMode,
                onModeChanged = onViewModeChanged,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.viewMode == MetronomeViewMode.PHYSICAL) {
                        PhysicalMetronomeView(
                            isPlaying = state.isPlaying,
                            bpm = state.bpm,
                            beatCounter = state.beatCounter,
                            accentNow = state.lastBeatKind == BeatKind.ACCENT,
                        )
                    } else {
                        DigitalBeatView(
                            isPlaying = state.isPlaying,
                            beatIndex = state.beatIndex,
                            beatScale = beatScale,
                            beatColor = beatColor,
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.meter_presets),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MeterPresets.ALL.forEach { preset ->
                            val selected = state.beatsPerMeasure == preset.beats &&
                                state.beatNote == preset.note
                            FilledTonalButton(
                                onClick = { onPresetSelected(preset) },
                                colors = presetButtonColors(selected),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text(preset.label)
                            }
                        }
                    }
                }
            }

            TimeSignatureCard(
                beats = state.beatsPerMeasure,
                beatNote = state.beatNote,
                beatNoteLabels = beatNoteLabels,
                onBeatsChanged = onBeatsChanged,
                onBeatNoteSelected = onBeatNoteSelected,
            )

            ChipSelectorCard(
                label = stringResource(R.string.subdivision_label),
                options = subdivisionLabels,
                selectedIndex = Subdivision.entries.indexOf(state.subdivision),
                onSelected = { onSubdivisionSelected(Subdivision.entries[it]) },
            )

            AccentPatternCard(
                accents = state.accents,
                currentBeat = if (state.isPlaying) state.beatIndex else -1,
                onToggle = onAccentToggled,
            )

            AsymmetricRhythmCard(
                groups = state.groups,
                onPresetSelected = onAsymmetricPreset,
                onGroupSizeChanged = onGroupSizeChanged,
                onAddGroup = onAddGroup,
            )

            SettingRow(
                label = stringResource(R.string.same_accent_sound),
                checked = state.sameAccentSound,
                onCheckedChange = onSameAccentChanged,
            )

            FilledTonalButton(
                onClick = onCycleSound,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("${stringResource(R.string.change_sound)} · ${state.soundPackName}")
            }

            VolumeCard(
                volume = state.volume,
                onVolumeChanged = onVolumeChanged,
            )

            SettingRow(
                label = stringResource(R.string.flash_on_beat),
                checked = state.flashEnabled,
                enabled = state.flashAvailable,
                onCheckedChange = onFlashChanged,
            )

            SettingRow(
                label = stringResource(R.string.screen_color_on_beat),
                checked = state.screenColorEnabled,
                onCheckedChange = onScreenColorChanged,
            )

            Image(
                painter = painterResource(R.drawable.kookyar),
                contentDescription = stringResource(R.string.kookyar_website),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenKookyar() },
                contentScale = ContentScale.FillWidth,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    }
}

private val PendulumEasing = Easing { t -> 0.5f - 0.5f * cos(PI.toFloat() * t) }

@Composable
private fun ModeToggle(
    mode: MetronomeViewMode,
    onModeChanged: (MetronomeViewMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ModeToggleButton(
            label = stringResource(R.string.view_mode_physical),
            selected = mode == MetronomeViewMode.PHYSICAL,
            modifier = Modifier.weight(1f),
            onClick = { onModeChanged(MetronomeViewMode.PHYSICAL) },
        )
        ModeToggleButton(
            label = stringResource(R.string.view_mode_digital),
            selected = mode == MetronomeViewMode.DIGITAL,
            modifier = Modifier.weight(1f),
            onClick = { onModeChanged(MetronomeViewMode.DIGITAL) },
        )
    }
}

@Composable
private fun ModeToggleButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "modeToggleBg",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            },
        )
    }
}

@Composable
private fun DigitalBeatView(
    isPlaying: Boolean,
    beatIndex: Int,
    beatScale: Float,
    beatColor: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.beat_counter),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(beatScale)
                .clip(CircleShape)
                .background(beatColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isPlaying) "${beatIndex + 1}" else "–",
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = beatColor,
            )
        }
    }
}

@Composable
private fun PhysicalMetronomeView(
    isPlaying: Boolean,
    bpm: Int,
    beatCounter: Long,
    accentNow: Boolean,
) {
    val maxAngle = 26f
    val swingRight = beatCounter % 2L == 0L
    val beatPeriodMs = (60_000.0 / bpm.coerceAtLeast(1)).toInt()
    val targetAngle = when {
        !isPlaying -> 0f
        swingRight -> maxAngle
        else -> -maxAngle
    }
    val angle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = tween(
            durationMillis = if (isPlaying) beatPeriodMs else 350,
            easing = PendulumEasing,
        ),
        label = "pendulumAngle",
    )

    val bodyColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val bodyOutline = MaterialTheme.colorScheme.primary
    val slotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val rodColor = MaterialTheme.colorScheme.onSurface
    val bobColor = if (accentNow && isPlaying) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.secondary
    }
    val pivotColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        val bodyTop = h * 0.16f
        val bodyBottom = h * 0.96f
        val topHalf = w * 0.13f
        val bottomHalf = w * 0.33f

        val bodyPath = Path().apply {
            moveTo(cx - topHalf, bodyTop)
            lineTo(cx + topHalf, bodyTop)
            lineTo(cx + bottomHalf, bodyBottom)
            lineTo(cx - bottomHalf, bodyBottom)
            close()
        }
        drawPath(bodyPath, bodyColor)
        drawPath(bodyPath, bodyOutline, style = Stroke(width = h * 0.012f))

        drawLine(
            color = slotColor,
            start = Offset(cx, bodyTop + h * 0.06f),
            end = Offset(cx, bodyBottom - h * 0.10f),
            strokeWidth = h * 0.02f,
            cap = StrokeCap.Round,
        )

        val pivotY = bodyBottom - h * 0.14f
        val rodLength = (pivotY - bodyTop) + h * 0.14f

        rotate(degrees = angle, pivot = Offset(cx, pivotY)) {
            drawLine(
                color = rodColor,
                start = Offset(cx, pivotY),
                end = Offset(cx, pivotY - rodLength),
                strokeWidth = h * 0.022f,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = bobColor,
                radius = h * 0.05f,
                center = Offset(cx, pivotY - rodLength * 0.60f),
            )
        }

        drawCircle(
            color = pivotColor,
            radius = h * 0.035f,
            center = Offset(cx, pivotY),
        )
    }
}

@Composable
private fun BpmCard(
    bpm: Int,
    timeSignature: String,
    tempoMarking: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onBpmChanged: (Int) -> Unit,
) {
    var sliderBpm by remember(bpm) { mutableIntStateOf(bpm) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = null)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = timeSignature,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Text(
                        text = "$bpm",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.bpm_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Text(
                        text = tempoMarking,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                IconButton(onClick = onIncrement, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }

            Slider(
                value = sliderBpm.toFloat(),
                onValueChange = {
                    sliderBpm = it.toInt()
                    onBpmChanged(it.toInt())
                },
                valueRange = MetronomeViewModel.MIN_BPM.toFloat()..MetronomeViewModel.MAX_BPM.toFloat(),
                steps = MetronomeViewModel.MAX_BPM - MetronomeViewModel.MIN_BPM - 1,
            )
        }
    }
}

@Composable
private fun VolumeCard(
    volume: Float,
    onVolumeChanged: (Float) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.volume_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Slider(
                value = volume,
                onValueChange = onVolumeChanged,
                valueRange = 0f..1f,
            )
        }
    }
}

@Composable
private fun TimeSignatureCard(
    beats: Int,
    beatNote: BeatNote,
    beatNoteLabels: List<String>,
    onBeatsChanged: (Int) -> Unit,
    onBeatNoteSelected: (BeatNote) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.time_signature_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(onClick = { onBeatsChanged(beats - 1) }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = null)
                }
                Text(
                    text = "$beats",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(56.dp),
                )
                IconButton(onClick = { onBeatsChanged(beats + 1) }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
                Text(
                    text = "/",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Text(
                    text = "${beatNote.symbol}",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            WrapChipSelector(
                options = beatNoteLabels,
                selectedIndex = BeatNote.entries.indexOf(beatNote),
                onSelected = { onBeatNoteSelected(BeatNote.entries[it]) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentPatternCard(
    accents: List<Boolean>,
    currentBeat: Int,
    onToggle: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.accent_pattern_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                accents.forEachIndexed { index, accented ->
                    val active = index == currentBeat
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { onToggle(index) }
                            .background(
                                when {
                                    active -> MaterialTheme.colorScheme.primary
                                    accented -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                            )
                            .border(
                                width = if (accented) 2.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AsymmetricRhythmCard(
    groups: List<Int>,
    onPresetSelected: (AsymmetricPresets.Grouping) -> Unit,
    onGroupSizeChanged: (Int, Int) -> Unit,
    onAddGroup: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.asymmetric_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )

            Text(
                text = stringResource(R.string.asymmetric_presets),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AsymmetricPresets.ALL.forEach { preset ->
                    val selected = preset.groups == groups
                    FilledTonalButton(
                        onClick = { onPresetSelected(preset) },
                        colors = presetButtonColors(selected),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(preset.label, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }

            Text(
                text = stringResource(R.string.asymmetric_custom),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                groups.forEachIndexed { index, size ->
                    GroupStepper(
                        size = size,
                        onDecrease = { onGroupSizeChanged(index, -1) },
                        onIncrease = { onGroupSizeChanged(index, 1) },
                    )
                }
                FilledTonalButton(
                    onClick = onAddGroup,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.add_group), fontSize = 13.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun GroupStepper(
    size: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDecrease, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Text(
            text = "$size",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(24.dp),
        )
        IconButton(onClick = onIncrease, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ChipSelectorCard(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            WrapChipSelector(options = options, selectedIndex = selectedIndex, onSelected = onSelected)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WrapChipSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEachIndexed { index, option ->
            FilledTonalButton(
                onClick = { onSelected(index) },
                colors = presetButtonColors(index == selectedIndex),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(option, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@Composable
private fun presetButtonColors(selected: Boolean) = ButtonDefaults.filledTonalButtonColors(
    containerColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    },
    contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    },
)

@Composable
private fun SettingRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            },
        )
    }
}
