package com.example.fluidsim.gl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fluidsim.ml.AiAssist;
import com.example.fluidsim.sim.FluidSimulation;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Renderer hook connecting GLSurfaceView with the simulation.
 */
public class FluidRenderer implements GLSurfaceView.Renderer {

    public interface FrameListener {
        void onStats(@NonNull RendererStats stats);
    }

    public static final class RendererStats {
        public final float fps;
        public final int gridSize;
        public final int pressureIterations;
        public final int temperatureLevel;

        public RendererStats(float fps, int gridSize, int pressureIterations, int temperatureLevel) {
            this.fps = fps;
            this.gridSize = gridSize;
            this.pressureIterations = pressureIterations;
            this.temperatureLevel = temperatureLevel;
        }
    }

    private final FluidSimulation simulation;
    private final AtomicReference<FrameListener> frameListener = new AtomicReference<>();

    private long frameCounter = 0;
    private long lastTimestampNs = 0;
    private float fpsAverage = 60f;

    public FluidRenderer(@NonNull Context context) {
        this.simulation = new FluidSimulation(context);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        simulation.onSurfaceCreated();
        lastTimestampNs = SystemClock.elapsedRealtimeNanos();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        simulation.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        simulation.step();
        simulation.render();
        publishStats();
    }

    public void setQuality(int gridSize, int pressureIterations) {
        simulation.setQuality(gridSize, pressureIterations);
    }

    public int getGridSize() {
        return simulation.getGridSize();
    }

    public void setPalette(int paletteId) {
        simulation.setPalette(paletteId);
    }

    public void setBloomEnabled(boolean enabled) {
        simulation.setBloomEnabled(enabled);
    }

    public void setParticlesEnabled(boolean enabled) {
        simulation.setParticlesEnabled(enabled);
    }

    public void setAiEnabled(boolean enabled, float strength) {
        simulation.setAiEnabled(enabled, strength);
    }

    public void onTouch(float x, float y, float dx, float dy, int colorId) {
        simulation.enqueueTouch(x, y, dx, dy, colorId);
    }

    public void reset() {
        simulation.reset();
    }

    public void setAiAssist(@NonNull AiAssist assist, @Nullable ByteBuffer input, @Nullable ByteBuffer output) {
        simulation.attachAi(assist, input, output);
    }

    public void setOnFrameListener(@Nullable FrameListener listener) {
        frameListener.set(listener);
    }

    private void publishStats() {
        long now = SystemClock.elapsedRealtimeNanos();
        if (lastTimestampNs == 0) {
            lastTimestampNs = now;
            return;
        }
        frameCounter++;
        float dt = (now - lastTimestampNs) / 1_000_000_000f;
        if (dt >= 1.0f) {
            fpsAverage = frameCounter / dt;
            frameCounter = 0;
            lastTimestampNs = now;
            FrameListener listener = frameListener.get();
            if (listener != null) {
                listener.onStats(new RendererStats(
                        fpsAverage,
                        simulation.getGridSize(),
                        simulation.getPressureIterations(),
                        simulation.getThermalLevel()));
            }
        }
    }
}
