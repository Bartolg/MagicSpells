package com.example.fluidsim

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import com.example.fluidsim.ui.FluidUiState
import com.example.fluidsim.ui.FluidSurface
import com.example.fluidsim.ui.FluidViewModel
import com.example.fluidsim.ui.theme.FluidTheme

class MainActivity : ComponentActivity() {
    private val viewModel: FluidViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onRecordPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FluidTheme {
                val uiState by viewModel.uiState.collectAsState()
                Box(modifier = Modifier.fillMaxSize()) {
                    FluidSurface(
                        modifier = Modifier.fillMaxSize(),
                        uiState = uiState,
                        onTouch = viewModel::onTouch,
                        onFrame = viewModel::onFrame,
                    )
                    ControlPanel(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        uiState = uiState,
                        onToggleParticles = viewModel::toggleParticles,
                        onToggleBloom = viewModel::toggleBloom,
                        onToggleGrid = viewModel::toggleGrid,
                        onToggleAi = viewModel::toggleAi,
                        onStyleChange = viewModel::changeStyle,
                        onIterationChange = viewModel::changeIterations,
                        onResolutionChange = viewModel::changeResolution,
                        onReset = viewModel::reset,
                        onRecord = {
                            if (checkRecordPermission()) {
                                viewModel.toggleRecording()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onAiStrengthChanged = viewModel::setAiStrength,
                    )
                }
            }
        }
    }

    private fun checkRecordPermission(): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.onRecordPermissionResult(true)
        }
        return granted
    }
}

@Composable
private fun ControlPanel(
    modifier: Modifier,
    uiState: FluidUiState,
    onToggleParticles: () -> Unit,
    onToggleBloom: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleAi: () -> Unit,
    onStyleChange: (FluidUiState.Style) -> Unit,
    onIterationChange: (Int) -> Unit,
    onResolutionChange: (Int) -> Unit,
    onReset: () -> Unit,
    onRecord: () -> Unit,
    onAiStrengthChanged: (Float) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = context.getString(
                R.string.fps_label,
                uiState.fps
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = uiState.style.displayName)
            Spacer(modifier = Modifier.height(0.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { onStyleChange(FluidUiState.Style.Warm) }) {
                Text(text = context.getString(R.string.style_warm))
            }
            Spacer(modifier = Modifier.height(0.dp))
            Button(onClick = { onStyleChange(FluidUiState.Style.Cool) }) {
                Text(text = context.getString(R.string.style_cool))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = context.getString(R.string.toggle_particles))
            Switch(checked = uiState.particlesEnabled, onCheckedChange = { _ -> onToggleParticles() })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = context.getString(R.string.toggle_bloom))
            Switch(checked = uiState.bloomEnabled, onCheckedChange = { _ -> onToggleBloom() })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = context.getString(R.string.toggle_ai))
            Switch(checked = uiState.aiEnabled, onCheckedChange = { _ -> onToggleAi() })
        }
        Slider(
            value = uiState.aiStrength,
            onValueChange = onAiStrengthChanged,
            valueRange = 0f..1f
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = context.getString(R.string.toggle_grid))
            Switch(checked = uiState.gridVisible, onCheckedChange = { _ -> onToggleGrid() })
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = context.getString(R.string.toggle_iterations))
            Slider(
                value = uiState.pressureIterations.toFloat(),
                onValueChange = { onIterationChange(it.roundToInt()) },
                valueRange = 10f..60f
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = context.getString(R.string.toggle_resolution))
            Slider(
                value = uiState.gridResolution.toFloat(),
                onValueChange = { onResolutionChange(it.roundToInt()) },
                valueRange = 256f..1024f
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onReset) {
            Text(text = context.getString(R.string.toggle_reset))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRecord) {
            Text(text = context.getString(R.string.toggle_record))
        }
    }
}
