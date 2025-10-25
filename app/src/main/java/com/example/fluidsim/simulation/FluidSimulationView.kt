package com.example.fluidsim.simulation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.example.fluidsim.ai.TFLiteEffectGenerator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A lightweight placeholder fluid simulation rendered on the Canvas. The simulation keeps
 * track of a set of vortices whose velocities are blended over time. The TensorFlow Lite
 * effect generator can optionally modulate the colors of the rendering, allowing the template
 * to be extended with learned visual effects.
 */
class FluidSimulationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), DefaultLifecycleObserver {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val vortices = MutableList(VORTEX_COUNT) { index ->
        val ratio = index.toFloat() / VORTEX_COUNT
        Vortex(
            x = cos(ratio * Math.PI * 2).toFloat(),
            y = sin(ratio * Math.PI * 2).toFloat(),
            phase = ratio * Math.PI.toFloat()
        )
    }
    private var effectGenerator: TFLiteEffectGenerator? = null
    private var displayTime = 0f
    private var attachedLifecycle: Lifecycle? = null

    fun setLifecycle(lifecycle: Lifecycle) {
        attachedLifecycle?.removeObserver(this)
        attachedLifecycle = lifecycle
        lifecycle.addObserver(this)
    }

    fun attachEffectGenerator(generator: TFLiteEffectGenerator) {
        effectGenerator = generator
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        postInvalidateOnAnimation()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        removeCallbacks(invalidateRunnable)
    }

    private val invalidateRunnable = Runnable { invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        if (width == 0f || height == 0f) return

        displayTime += FRAME_TIME_SECONDS
        canvas.drawColor(Color.BLACK)

        val colorModifiers = effectGenerator?.generateColorModifiers(displayTime)?.let {
            ColorModifiers(it.hue, it.saturation, it.value)
        } ?: DEFAULT_COLOR

        for (vortex in vortices) {
            vortex.advance(displayTime)
            val cx = width * (0.5f + 0.45f * vortex.x)
            val cy = height * (0.5f + 0.45f * vortex.y)
            val radius = 50f + 40f * vortex.energy

            paint.color = Color.HSVToColor(
                floatArrayOf(
                    (colorModifiers.hue * 360f + vortex.phase * 180f / Math.PI).toFloat() % 360f,
                    0.7f + 0.3f * colorModifiers.saturation,
                    0.6f + 0.4f * colorModifiers.value
                )
            )
            canvas.drawCircle(cx, cy, radius, paint)
        }

        postOnAnimation(invalidateRunnable)
    }

    private data class Vortex(
        var x: Float,
        var y: Float,
        var phase: Float,
        var energy: Float = 1f
    ) {
        fun advance(time: Float) {
            val radius = sqrt(x * x + y * y)
            val angle = phase + time * 0.5f
            val rotation = time * 0.25f
            x = (cos(angle.toDouble()) * radius).toFloat()
            y = (sin(angle.toDouble()) * radius).toFloat()
            phase += rotation
            energy = 0.5f + 0.5f * sin((time + phase).toDouble()).toFloat()
        }
    }

    private data class ColorModifiers(val hue: Float, val saturation: Float, val value: Float)

    companion object {
        private const val VORTEX_COUNT = 6
        private const val FRAME_TIME_SECONDS = 1f / 60f
        private val DEFAULT_COLOR = ColorModifiers(0.6f, 0.5f, 0.9f)
    }
}
