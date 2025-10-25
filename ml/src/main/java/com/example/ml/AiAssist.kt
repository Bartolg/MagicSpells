package com.example.ml

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.tensorflow.lite.Delegate
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * Handles loading the TensorFlow Lite model and dispatching asynchronous
 * inference jobs. The actual buffer wiring to the GPU engine is delegated to
 * the caller via the [Callbacks] interface. This keeps the module self-contained
 * and ready for the future compute integration.
 */
class AiAssist(
    context: Context,
    private val callbacks: Callbacks,
    modelAssetPath: String = DEFAULT_MODEL_PATH,
) {
    interface Callbacks {
        fun provideInput(buffer: ByteBuffer)
        fun consumeOutput(buffer: ByteBuffer)
    }

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "tflite-worker").apply { isDaemon = true }
    }
    private val dispatcher = executor.asCoroutineDispatcher()
    private val scope = CoroutineScope(dispatcher + Job())

    private val delegates: List<Delegate>
    private val interpreter: Interpreter?

    val inputBuffer: ByteBuffer
    val outputBuffer: ByteBuffer

    init {
        val options = Interpreter.Options().apply {
            setUseNNAPI(true)
            setCancellable(true)
        }

        val nnapi = try {
            NnApiDelegate()
        } catch (t: Throwable) {
            null
        }
        val gpu = try {
            GpuDelegate()
        } catch (t: Throwable) {
            null
        }
        delegates = listOfNotNull(nnapi, gpu)
        delegates.forEach { options.addDelegate(it) }

        val modelBuffer = try {
            context.assets.open(modelAssetPath).use { stream ->
                val byteArray = stream.readBytes()
                ByteBuffer.allocateDirect(byteArray.size).apply {
                    order(java.nio.ByteOrder.nativeOrder())
                    put(byteArray)
                    rewind()
                }
            }
        } catch (_: Throwable) {
            null
        }
        inputBuffer = ByteBuffer.allocateDirect(INPUT_BYTES).apply {
            order(java.nio.ByteOrder.nativeOrder())
        }
        outputBuffer = ByteBuffer.allocateDirect(OUTPUT_BYTES).apply {
            order(java.nio.ByteOrder.nativeOrder())
        }
        interpreter = if (modelBuffer != null) {
            try {
                Interpreter(modelBuffer, options)
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun requestInference(styleId: Float) {
        val runner = interpreter ?: return
        scope.launch {
            callbacks.provideInput(inputBuffer)
            runner.run(inputBuffer, outputBuffer)
            callbacks.consumeOutput(outputBuffer)
        }
    }

    fun dispose() {
        interpreter?.close()
        delegates.forEach { delegate ->
            try {
                delegate.close()
            } catch (_: Throwable) {
                // Ignored
            }
        }
        executor.shutdownNow()
    }

    companion object {
        private const val CHANNELS_IN = 7
        private const val CHANNELS_OUT = 5
        private const val SIZE = 256
        private const val BATCH = 1
        private const val DEFAULT_MODEL_PATH = "effects_unet_int8.tflite"
        private const val INPUT_BYTES = BATCH * CHANNELS_IN * SIZE * SIZE
        private const val OUTPUT_BYTES = BATCH * CHANNELS_OUT * SIZE * SIZE
    }
}
