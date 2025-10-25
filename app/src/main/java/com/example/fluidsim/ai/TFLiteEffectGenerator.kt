package com.example.fluidsim.ai

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.Closeable
import java.nio.MappedByteBuffer

/**
 * Loads a TensorFlow Lite model from assets/effect_model.tflite and exposes a convenience API
 * for generating color modifiers based on the current simulation time. The default
 * implementation falls back to a deterministic pattern if the model is missing so that the
 * sample remains runnable without additional assets.
 */
class TFLiteEffectGenerator private constructor(
    private val interpreter: Interpreter?,
    private val modelAvailable: Boolean
) : Closeable {

    data class ColorModifiers(val hue: Float, val saturation: Float, val value: Float)

    fun generateColorModifiers(timeSeconds: Float): ColorModifiers {
        if (interpreter == null || !modelAvailable) {
            // Deterministic fallback pattern
            val hue = (0.5f + 0.5f * kotlin.math.sin(timeSeconds.toDouble())).toFloat()
            val saturation = 0.8f
            val value = 0.9f
            return ColorModifiers(hue, saturation, value)
        }

        val inputBuffer = floatArrayOf(timeSeconds)
        val outputBuffer = FloatArray(3)
        interpreter.run(inputBuffer, outputBuffer)
        return ColorModifiers(
            hue = outputBuffer.getOrElse(0) { 0.5f }.coerceIn(0f, 1f),
            saturation = outputBuffer.getOrElse(1) { 0.8f }.coerceIn(0f, 1f),
            value = outputBuffer.getOrElse(2) { 0.9f }.coerceIn(0f, 1f)
        )
    }

    override fun close() {
        interpreter?.close()
    }

    companion object {
        private const val MODEL_PATH = "effect_model.tflite"
        private const val TAG = "TFLiteEffectGenerator"

        fun create(context: Context): TFLiteEffectGenerator? {
            return try {
                val file = FileUtil.loadMappedFile(context, MODEL_PATH)

                if (!file.hasValidTFLiteMagic()) {
                    Log.w(TAG, "Model placeholder detected, falling back to procedural colors")
                    return TFLiteEffectGenerator(null, modelAvailable = false)
                }

                val interpreter = Interpreter(file)
                TFLiteEffectGenerator(interpreter, modelAvailable = true)
            } catch (missing: IllegalArgumentException) {
                Log.w(TAG, "Model not found, falling back to procedural colors", missing)
                TFLiteEffectGenerator(null, modelAvailable = false)
            } catch (error: Throwable) {
                Log.e(TAG, "Unable to initialize TensorFlow Lite", error)
                null
            }
        }

        private fun MappedByteBuffer.hasValidTFLiteMagic(): Boolean {
            if (remaining() < 4) return false

            val magic = ByteArray(4)
            duplicate().apply { position(0) }.get(magic)
            return magic[0] == 'T'.code.toByte() &&
                magic[1] == 'F'.code.toByte() &&
                magic[2] == 'L'.code.toByte() &&
                magic[3] == '3'.code.toByte()
        }
    }
}
