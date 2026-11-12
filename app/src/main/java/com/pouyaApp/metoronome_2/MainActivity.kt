package com.pouyaApp.metoronome_2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pouyaApp.metoronome_2.ui.MetronomeScreen
import com.pouyaApp.metoronome_2.ui.theme.MetronomeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MetronomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is always dark, so keep light status/navigation bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
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
                    onOpenKookyar = { openUrl(developerPageUrl()) },
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
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.exit_dialog_title)
            .setMessage(R.string.exit_dialog_message)
            .setPositiveButton(R.string.exit) { _, _ -> finish() }
            .setNeutralButton(R.string.rate_app) { _, _ ->
                viewModel.markWelcomeShown()
                openUrl(storeListingUri())
                finish()
            }
            .setCancelable(true)
            .show()
    }

    // The bazaar and play flavors (kookyar-store-flavors.gradle) set kookyar_store.
    private val isPlayBuild: Boolean
        get() = getString(R.string.kookyar_store) == "play"

    private fun storeListingUri(): String =
        if (isPlayBuild) "market://details?id=$packageName"
        else "bazaar://details?id=$packageName"

    private fun developerPageUrl(): String =
        if (isPlayBuild) "https://play.google.com/store/apps/developer?id=TuningApps"
        else "https://cafebazaar.ir/developer/pouya"

    private fun openUrl(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}
