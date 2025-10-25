package com.example.fluidsim.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.engine.Engine
import com.example.ml.AiAssist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FluidViewModel(application: Application) : AndroidViewModel(application), AiAssist.Callbacks {
    private val engine = Engine(application)
    private val aiAssist = AiAssist(application, this)

    private val _uiState = MutableStateFlow(FluidUiState())
    val uiState: StateFlow<FluidUiState> = _uiState.asStateFlow()

    private var lastFrameTime = System.nanoTime()
    private var frameCounter = 0

    override fun onCleared() {
        super.onCleared()
        aiAssist.dispose()
        _initialized = false
    }

    fun onTouch(x: Float, y: Float, dx: Float, dy: Float, color: Int) {
        engine.onTouch(x, y, dx, dy, color)
    }

    fun onFrame(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        if (!_initialized) {
            initializeEngine(width, height)
        }
        val now = System.nanoTime()
        val deltaSeconds = (now - lastFrameTime) / 1_000_000_000f
        lastFrameTime = now
        engine.onFrame(deltaSeconds)
        updateFps(deltaSeconds)
        frameCounter++
        if (frameCounter % 3 == 0) {
            triggerAiAssist(_uiState.value.style.ordinal.toFloat())
        }
    }

    fun toggleParticles() = updateState { copy(particlesEnabled = !particlesEnabled) }
    fun toggleBloom() = updateState { copy(bloomEnabled = !bloomEnabled) }
    fun toggleGrid() = updateState { copy(gridVisible = !gridVisible) }
    fun toggleAi() = updateState { copy(aiEnabled = !aiEnabled) }

    fun changeStyle(style: FluidUiState.Style) = updateState { copy(style = style) }

    fun changeIterations(value: Int) = updateState {
        copy(pressureIterations = value.coerceIn(10, 60))
    }

    fun changeResolution(value: Int) = updateState {
        copy(gridResolution = value.coerceIn(256, 1024))
    }

    fun reset() {
        // Placeholder – the native engine will expose a reset hook later.
    }

    fun toggleRecording() = updateState { copy(recording = !recording) }

    fun setAiStrength(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        updateState { copy(aiStrength = clamped) }
        engine.setAiStrength(clamped)
    }

    fun onRecordPermissionResult(granted: Boolean) = updateState {
        copy(recordPermissionGranted = granted)
    }

    private fun updateFps(dt: Float) {
        val fps = if (dt > 0f) 1f / dt else 0f
        updateState { copy(fps = fps) }
    }

    private fun initializeEngine(width: Int, height: Int) {
        engine.initialize(width, height, useVulkan = true)
        _initialized = true
    }

    private fun updateState(block: FluidUiState.() -> FluidUiState) {
        _uiState.value = _uiState.value.block()
    }

    override fun provideInput(buffer: java.nio.ByteBuffer) {
        // TODO hook GPU readback when ready. For now we clear the buffer to keep
        // deterministic inference results during development.
        buffer.clear()
        repeat(buffer.capacity()) { buffer.put(0.toByte()) }
        buffer.flip()
    }

    override fun consumeOutput(buffer: java.nio.ByteBuffer) {
        buffer.rewind()
        // Mapping output to engine textures will be implemented later.
    }

    fun triggerAiAssist(styleId: Float) {
        if (_uiState.value.aiEnabled) {
            aiAssist.requestInference(styleId)
        }
    }

    companion object {
        private var _initialized = false
    }
}
