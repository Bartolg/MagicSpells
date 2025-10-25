package com.example.fluidsim.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun FluidSurface(
    modifier: Modifier = Modifier,
    uiState: FluidUiState,
    onTouch: (Float, Float, Float, Float, Int) -> Unit,
    onFrame: (Int, Int) -> Unit,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            FluidSurfaceView(context).apply {
                setCallbacks(onTouch, onFrame)
                updateUiState(uiState)
            }
        },
        update = {
            it.setCallbacks(onTouch, onFrame)
            it.updateUiState(uiState)
        }
    )
}

private class FluidSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback, Choreographer.FrameCallback {

    private var onTouchCallback: (Float, Float, Float, Float, Int) -> Unit = { _, _, _, _, _ -> }
    private var onFrameCallback: (Int, Int) -> Unit = { _, _ -> }
    private val pointerCache = mutableMapOf<Int, Pair<Float, Float>>()
    private var uiState: FluidUiState = FluidUiState()
    private var running = false
    private val choreographer = Choreographer.getInstance()
    private var frameSeed = 0

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    fun setCallbacks(
        onTouch: (Float, Float, Float, Float, Int) -> Unit,
        onFrame: (Int, Int) -> Unit,
    ) {
        onTouchCallback = onTouch
        onFrameCallback = onFrame
    }

    fun updateUiState(state: FluidUiState) {
        uiState = state
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        choreographer.postFrameCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Reinitialize engine with new dimensions on next frame via callback.
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        choreographer.removeFrameCallback(this)
    }

    @Suppress("UNUSED_PARAMETER")
    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        onFrameCallback(width, height)
        choreographer.postFrameCallback(this)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                pointerCache[pointerId] = x to y
                emitTouch(x, y, 0f, 0f)
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)
                    val previous = pointerCache[id]
                    val dx = if (previous != null) x - previous.first else 0f
                    val dy = if (previous != null) y - previous.second else 0f
                    pointerCache[id] = x to y
                    emitTouch(x, y, dx, dy)
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_CANCEL -> {
                pointerCache.remove(pointerId)
            }
        }
        return true
    }

    private fun emitTouch(x: Float, y: Float, dx: Float, dy: Float) {
        val color = when (uiState.style) {
            FluidUiState.Style.Warm -> warmPalette[frameSeed % warmPalette.size]
            FluidUiState.Style.Cool -> coolPalette[frameSeed % coolPalette.size]
        }
        frameSeed = (frameSeed + 1) % 1024
        onTouchCallback(x, y, dx, dy, color)
    }

    companion object {
        private val warmPalette = listOf(
            Color.rgb(255, 64, 128),
            Color.rgb(255, 140, 0),
            Color.rgb(255, 215, 0)
        )
        private val coolPalette = listOf(
            Color.rgb(0, 255, 255),
            Color.rgb(64, 128, 255),
            Color.rgb(180, 64, 255)
        )
    }
}
