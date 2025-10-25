package com.example.fluidsim.sim;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fluidsim.gl.ShaderHandle;
import com.example.fluidsim.ml.AiAssist;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Coordinates the compute shaders and draw passes for the fluid simulation. This implementation
 * keeps the data structures ready and renders a placeholder gradient; it can be extended with the
 * full stable-fluids pipeline without modifying the renderer contract.
 */
public final class FluidSimulation {

    private static final String TAG = "FluidSimulation";

    private final Context context;
    private final ConcurrentLinkedQueue<TouchEvent> touchQueue = new ConcurrentLinkedQueue<>();

    private int gridSize = 1024;
    private int pressureIterations = 24;
    private boolean bloomEnabled = true;
    private boolean particlesEnabled = true;
    private boolean aiEnabled = true;
    private float aiStrength = 0.5f;
    private int paletteId = 0;

    private ShaderHandle fullscreenProgram;
    private int fullscreenVao;

    private AiAssist aiAssist;
    private ByteBuffer aiInput;
    private ByteBuffer aiOutput;
    private long lastAiDispatch = 0L;

    private final FloatBuffer fullscreenQuad = ByteBuffer
            .allocateDirect(4 * 4 * Float.BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();

    public FluidSimulation(@NonNull Context context) {
        this.context = context;
        fullscreenQuad.put(new float[]{
                -1f, -1f, 0f, 0f,
                1f, -1f, 1f, 0f,
                -1f, 1f, 0f, 1f,
                1f, 1f, 1f, 1f
        });
        fullscreenQuad.position(0);
    }

    public void onSurfaceCreated() {
        fullscreenProgram = ShaderHandle.createFullscreenProgram(context);
        fullscreenVao = GlObjects.createFullscreenVao(fullscreenQuad);
    }

    public void onSurfaceChanged(int width, int height) {
        // In a full implementation we would rebuild framebuffers and textures here.
    }

    public void step() {
        drainTouches();
        maybeRunAi();
        // Placeholder integration point for fluid compute dispatches.
    }

    public void render() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(fullscreenProgram.getProgram());
        GLES30.glBindVertexArray(fullscreenVao);
        int paletteLocation = GLES20.glGetUniformLocation(fullscreenProgram.getProgram(), "uPalette");
        GLES20.glUniform1i(paletteLocation, paletteId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glBindVertexArray(0);
    }

    public void setQuality(int gridSize, int pressureIterations) {
        this.gridSize = gridSize;
        this.pressureIterations = pressureIterations;
    }

    public int getGridSize() {
        return gridSize;
    }

    public int getPressureIterations() {
        return pressureIterations;
    }

    public void setPalette(int paletteId) {
        this.paletteId = paletteId;
    }

    public void setBloomEnabled(boolean enabled) {
        this.bloomEnabled = enabled;
    }

    public void setParticlesEnabled(boolean enabled) {
        this.particlesEnabled = enabled;
    }

    public void setAiEnabled(boolean enabled, float strength) {
        this.aiEnabled = enabled;
        this.aiStrength = strength;
    }

    public void enqueueTouch(float x, float y, float dx, float dy, int colorId) {
        touchQueue.add(new TouchEvent(x, y, dx, dy, colorId));
    }

    public void reset() {
        // Clear dye/velocity textures in a complete implementation.
    }

    public void attachAi(@NonNull AiAssist assist, @Nullable ByteBuffer input, @Nullable ByteBuffer output) {
        this.aiAssist = assist;
        this.aiInput = input;
        this.aiOutput = output;
    }

    public int getThermalLevel() {
        // Placeholder value that could be mapped to actual thermal feedback.
        return 0;
    }

    private void drainTouches() {
        TouchEvent evt;
        while ((evt = touchQueue.poll()) != null) {
            // The compute splat pass would be issued here. For now we just log.
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Touch" + evt);
            }
        }
    }

    private void maybeRunAi() {
        if (!aiEnabled || aiAssist == null || aiInput == null || aiOutput == null) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastAiDispatch < 33_000_000L * 2) {
            return;
        }
        lastAiDispatch = now;
        aiAssist.enqueue(aiInput, aiOutput);
    }

    public void destroy() {
        if (fullscreenProgram != null) {
            fullscreenProgram.close();
            fullscreenProgram = null;
        }
        if (fullscreenVao != 0) {
            int[] arrays = new int[]{fullscreenVao};
            GLES30.glDeleteVertexArrays(1, arrays, 0);
            fullscreenVao = 0;
        }
    }

    private static final class TouchEvent {
        final float x;
        final float y;
        final float dx;
        final float dy;
        final int colorId;

        TouchEvent(float x, float y, float dx, float dy, int colorId) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.colorId = colorId;
        }

        @Override
        public String toString() {
            return "TouchEvent{" +
                    "x=" + x +
                    ", y=" + y +
                    ", dx=" + dx +
                    ", dy=" + dy +
                    ", colorId=" + colorId +
                    '}';
        }
    }
}
