package com.example.engine

import android.content.Context
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Thin Kotlin wrapper around the native simulation controller. The heavy lifting
 * happens inside the native Vulkan/OpenGL compute pipelines. For now the
 * implementation is stubbed out but keeps the contract intact.
 */
class Engine(private val context: Context) {
    private val initialized = AtomicBoolean(false)

    init {
        try {
            System.loadLibrary("fluid_engine")
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to load native library", t)
        }
    }

    fun initialize(width: Int, height: Int, useVulkan: Boolean) {
        if (initialized.compareAndSet(false, true)) {
            init(width, height, useVulkan)
        }
    }

    fun onTouch(x: Float, y: Float, dx: Float, dy: Float, color: Int) {
        if (!initialized.get()) return
        touch(x, y, dx, dy, color)
    }

    fun onFrame(dt: Float) {
        if (!initialized.get()) return
        frame(dt)
    }

    fun setAiStrength(strength: Float) {
        if (!initialized.get()) return
        setAiStrengthNative(strength)
    }

    private external fun init(width: Int, height: Int, useVulkan: Boolean)
    private external fun touch(x: Float, y: Float, dx: Float, dy: Float, color: Int)
    private external fun frame(dt: Float)
    private external fun setAiStrengthNative(strength: Float)

    companion object {
        private const val TAG = "Engine"
    }
}
