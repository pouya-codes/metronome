package com.pouyaApp.metoronome

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pouyaApp.metoronome.ui.MetronomeScreen
import com.pouyaApp.metoronome.ui.theme.MetronomeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MetronomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            MetronomeTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                MetronomeScreen(
                    state = state,
                    onBpmChanged = viewModel::setBpm,
                    onIncrement = viewModel::incrementBpm,
                    onDecrement = viewModel::decrementBpm,
                    onBeatsChanged = viewModel::setBeatsPerMeasure,
                    onBeatNoteSelected = viewModel::setBeatNote,
                    onSubdivisionSelected = viewModel::setSubdivision,
                    onAccentToggled = viewModel::toggleAccent,
                    onPresetSelected = viewModel::applyPreset,
                    onAsymmetricPreset = viewModel::applyAsymmetricPreset,
                    onGroupSizeChanged = viewModel::changeGroupSize,
                    onAddGroup = viewModel::addGroup,
                    onSameAccentChanged = viewModel::toggleSameAccentSound,
                    onVolumeChanged = viewModel::setVolume,
                    onFlashChanged = viewModel::toggleFlash,
                    onScreenColorChanged = viewModel::toggleScreenColor,
                    onViewModeChanged = viewModel::setViewMode,
                    onTogglePlayback = viewModel::togglePlayback,
                    onCycleSound = viewModel::cycleSoundPack,
                    onOpenKookyar = { openUrl("https://cafebazaar.ir/developer/pouya") },
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (viewModel.shouldShowRatePrompt()) {
            showRateDialog()
        } else {
            super.onBackPressed()
        }
    }

    private fun showRateDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.exit_dialog_title)
            .setMessage(R.string.exit_dialog_message)
            .setPositiveButton(R.string.exit) { _, _ -> finish() }
            .setNeutralButton(R.string.rate_app) { _, _ ->
                viewModel.markWelcomeShown()
                openUrl("bazaar://details?id=com.pouyaApp.metoronome")
                finish()
            }
            .setCancelable(true)
            .show()
    }

    private fun openUrl(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}
