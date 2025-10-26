package com.example.fluidsim.sim;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLES31;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.fluidsim.gl.ShaderHandle;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Coordinates the compute shaders and draw passes for the fluid simulation. The implementation
 * follows the classic "stable fluids" approach with a compute-driven pressure projection step.
 */
public final class FluidSimulation {

    private static final String TAG = "FluidSimulation";

    private static final float VELOCITY_DISSIPATION = 0.995f;
    private static final float DYE_DISSIPATION = 0.999f;
    private static final float MAX_TIMESTEP = 1f / 30f;
    private static final float SPLAT_RADIUS = 0.02f;
    private static final float SPLAT_FORCE = 6f;

    private final Context context;
    private final ConcurrentLinkedQueue<TouchEvent> touchQueue = new ConcurrentLinkedQueue<>();

    private ShaderHandle fullscreenProgram;
    private int fullscreenVao;
    private int paletteUniform;
    private int densityUniform;
    private int aspectUniform;
    private int hasDensityUniform;

    private ComputeProgram advectProgram;
    private int advectDtLocation;
    private int advectDissipationLocation;

    private ComputeProgram splatProgram;
    private int splatPointLocation;
    private int splatDeltaLocation;
    private int splatColorLocation;
    private int splatRadiusLocation;
    private int splatAspectLocation;
    private int splatVelocityLocation;

    private ComputeProgram divergenceProgram;
    private int divergenceTexelLocation;

    private ComputeProgram jacobiProgram;
    private int jacobiAlphaLocation;
    private int jacobiRBetaLocation;

    private ComputeProgram projectProgram;
    private int projectTexelLocation;

    private final FloatBuffer fullscreenQuad = ByteBuffer
            .allocateDirect(4 * 4 * Float.BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();

    private final PingPongTexture velocity = new PingPongTexture();
    private final PingPongTexture dye = new PingPongTexture();
    private final PingPongTexture pressure = new PingPongTexture();
    private int divergenceTexture = 0;

    private boolean computeSupported = false;
    private boolean texturesReady = false;

    private int gridSize = 1024;
    private int pressureIterations = 24;
    private int paletteId = 0;

    private int surfaceWidth = 1;
    private int surfaceHeight = 1;

    private long lastStepTimestampNs = 0L;

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
        int program = fullscreenProgram.getProgram();
        paletteUniform = GLES20.glGetUniformLocation(program, "uPalette");
        densityUniform = GLES20.glGetUniformLocation(program, "uDensity");
        aspectUniform = GLES20.glGetUniformLocation(program, "uAspect");
        hasDensityUniform = GLES20.glGetUniformLocation(program, "uHasDensity");

        computeSupported = isComputeSupported();
        if (!computeSupported) {
            Log.w(TAG, "OpenGL ES 3.1 is unavailable; falling back to gradient rendering.");
            return;
        }

        advectProgram = ComputeProgram.create(context, "shaders/advect.comp");
        advectDtLocation = advectProgram.getUniformLocation("uDt");
        advectDissipationLocation = advectProgram.getUniformLocation("uDissipation");

        splatProgram = ComputeProgram.create(context, "shaders/splat.comp");
        splatPointLocation = splatProgram.getUniformLocation("uPoint");
        splatDeltaLocation = splatProgram.getUniformLocation("uDelta");
        splatColorLocation = splatProgram.getUniformLocation("uColor");
        splatRadiusLocation = splatProgram.getUniformLocation("uRadius");
        splatAspectLocation = splatProgram.getUniformLocation("uAspect");
        splatVelocityLocation = splatProgram.getUniformLocation("uAffectsVelocity");

        divergenceProgram = ComputeProgram.create(context, "shaders/divergence.comp");
        divergenceTexelLocation = divergenceProgram.getUniformLocation("uTexelSize");

        jacobiProgram = ComputeProgram.create(context, "shaders/jacobi.comp");
        jacobiAlphaLocation = jacobiProgram.getUniformLocation("uAlpha");
        jacobiRBetaLocation = jacobiProgram.getUniformLocation("uRBeta");

        projectProgram = ComputeProgram.create(context, "shaders/project.comp");
        projectTexelLocation = projectProgram.getUniformLocation("uTexelSize");

        rebuildTextures();
    }

    public void onSurfaceChanged(int width, int height) {
        surfaceWidth = Math.max(width, 1);
        surfaceHeight = Math.max(height, 1);
    }

    public void step() {
        drainTouches();
        if (!computeSupported || !texturesReady) {
            return;
        }
        long now = SystemClock.elapsedRealtimeNanos();
        if (lastStepTimestampNs == 0L) {
            lastStepTimestampNs = now;
            return;
        }
        float dt = (now - lastStepTimestampNs) / 1_000_000_000f;
        lastStepTimestampNs = now;
        dt = Math.min(dt, MAX_TIMESTEP);
        simulate(dt);
    }

    public void render() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(fullscreenProgram.getProgram());
        GLES30.glBindVertexArray(fullscreenVao);
        GLES20.glUniform1i(paletteUniform, paletteId);
        GLES20.glUniform1f(aspectUniform, surfaceWidth / (float) surfaceHeight);
        if (computeSupported && texturesReady) {
            GLES20.glUniform1i(hasDensityUniform, 1);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, dye.read());
            GLES20.glUniform1i(densityUniform, 0);
        } else {
            GLES20.glUniform1i(hasDensityUniform, 0);
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glBindVertexArray(0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public void setQuality(int gridSize, int pressureIterations) {
        this.gridSize = gridSize;
        this.pressureIterations = Math.max(1, pressureIterations);
        if (computeSupported) {
            rebuildTextures();
        }
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

    public void enqueueTouch(float x, float y, float dx, float dy, int colorId) {
        touchQueue.add(new TouchEvent(x, y, dx, dy, colorId));
    }

    public void reset() {
        if (!texturesReady) {
            return;
        }
        velocity.clear();
        dye.clear();
        pressure.clear();
        clearTexture(divergenceTexture);
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
        destroyTextures();
        advectProgram = closeProgram(advectProgram);
        splatProgram = closeProgram(splatProgram);
        divergenceProgram = closeProgram(divergenceProgram);
        jacobiProgram = closeProgram(jacobiProgram);
        projectProgram = closeProgram(projectProgram);
        computeSupported = false;
        texturesReady = false;
    }

    private void simulate(float dt) {
        applyPendingSplats();

        advectField(velocity, velocity, dt, VELOCITY_DISSIPATION);
        advectField(dye, dye, dt, DYE_DISSIPATION);

        computeDivergence();
        pressure.clear();
        for (int i = 0; i < pressureIterations; i++) {
            jacobiIteration();
        }
        projectVelocity();
    }

    private void applyPendingSplats() {
        TouchEvent evt;
        while ((evt = touchQueue.poll()) != null) {
            float width = Math.max(surfaceWidth, 1);
            float height = Math.max(surfaceHeight, 1);
            float px = evt.x / width;
            float py = 1f - evt.y / height;
            float aspect = width / height;
            float velocityScale = gridSize * SPLAT_FORCE;
            float vx = (evt.dx / width) * velocityScale;
            float vy = (-evt.dy / height) * velocityScale;
            float[] color = paletteColor(evt.colorId);

            runSplat(velocity, px, py, aspect, SPLAT_RADIUS, vx, vy, 0f, true);
            runSplat(dye, px, py, aspect, SPLAT_RADIUS, 0f, 0f,
                    color[0], color[1], color[2], false);
        }
    }

    private void runSplat(PingPongTexture target, float px, float py, float aspect,
                          float radius, float vx, float vy, float colorR, float colorG, float colorB,
                          boolean affectsVelocity) {
        splatProgram.use();
        GLES31.glUniform2f(splatPointLocation, px, py);
        GLES31.glUniform2f(splatDeltaLocation, vx, vy);
        GLES31.glUniform3f(splatColorLocation, colorR, colorG, colorB);
        GLES31.glUniform1f(splatRadiusLocation, radius);
        GLES31.glUniform1f(splatAspectLocation, aspect);
        GLES31.glUniform1i(splatVelocityLocation, affectsVelocity ? 1 : 0);
        bindImage(0, target.write(), GLES31.GL_WRITE_ONLY);
        bindImage(1, target.read(), GLES31.GL_READ_ONLY);
        dispatch(gridSize, gridSize);
        target.swap();
    }

    private void runSplat(PingPongTexture target, float px, float py, float aspect,
                          float radius, float vx, float vy, float color, boolean affectsVelocity) {
        runSplat(target, px, py, aspect, radius, vx, vy, color, color, color, affectsVelocity);
    }

    private void advectField(PingPongTexture target, PingPongTexture source, float dt, float dissipation) {
        advectProgram.use();
        GLES31.glUniform1f(advectDtLocation, dt);
        GLES31.glUniform1f(advectDissipationLocation, dissipation);
        bindImage(0, target.write(), GLES31.GL_WRITE_ONLY);
        bindImage(1, source.read(), GLES31.GL_READ_ONLY);
        bindImage(2, velocity.read(), GLES31.GL_READ_ONLY);
        dispatch(gridSize, gridSize);
        target.swap();
    }

    private void computeDivergence() {
        divergenceProgram.use();
        float texelX = 1f / gridSize;
        float texelY = 1f / gridSize;
        GLES31.glUniform2f(divergenceTexelLocation, texelX, texelY);
        bindImage(0, divergenceTexture, GLES31.GL_WRITE_ONLY);
        bindImage(1, velocity.read(), GLES31.GL_READ_ONLY);
        dispatch(gridSize, gridSize);
    }

    private void jacobiIteration() {
        jacobiProgram.use();
        GLES31.glUniform1f(jacobiAlphaLocation, -1f);
        GLES31.glUniform1f(jacobiRBetaLocation, 0.25f);
        bindImage(0, pressure.write(), GLES31.GL_WRITE_ONLY);
        bindImage(1, pressure.read(), GLES31.GL_READ_ONLY);
        bindImage(2, divergenceTexture, GLES31.GL_READ_ONLY);
        dispatch(gridSize, gridSize);
        pressure.swap();
    }

    private void projectVelocity() {
        projectProgram.use();
        float texelX = 1f / gridSize;
        float texelY = 1f / gridSize;
        GLES31.glUniform2f(projectTexelLocation, texelX, texelY);
        bindImage(0, velocity.write(), GLES31.GL_WRITE_ONLY);
        bindImage(1, velocity.read(), GLES31.GL_READ_ONLY);
        bindImage(2, pressure.read(), GLES31.GL_READ_ONLY);
        dispatch(gridSize, gridSize);
        velocity.swap();
    }

    private void dispatch(int width, int height) {
        int groupsX = (width + 7) / 8;
        int groupsY = (height + 7) / 8;
        GLES31.glDispatchCompute(groupsX, groupsY, 1);
        GLES31.glMemoryBarrier(GLES31.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
    }

    private void bindImage(int unit, int texture, int access) {
        GLES31.glBindImageTexture(unit, texture, 0, false, 0, access, GLES30.GL_RGBA16F);
    }

    private void drainTouches() {
        if (!computeSupported) {
            touchQueue.clear();
        }
    }

    private void rebuildTextures() {
        destroyTextures();
        velocity.allocate(gridSize, gridSize);
        dye.allocate(gridSize, gridSize);
        pressure.allocate(gridSize, gridSize);
        divergenceTexture = createTexture(gridSize, gridSize);
        lastStepTimestampNs = 0L;
        clearTexture(divergenceTexture);
        velocity.clear();
        dye.clear();
        pressure.clear();
        texturesReady = velocity.isValid() && dye.isValid() && pressure.isValid() && divergenceTexture != 0;
    }

    private void destroyTextures() {
        velocity.destroy();
        dye.destroy();
        pressure.destroy();
        if (divergenceTexture != 0) {
            int[] tex = new int[]{divergenceTexture};
            GLES20.glDeleteTextures(1, tex, 0);
            divergenceTexture = 0;
        }
    }

    private static ComputeProgram closeProgram(ComputeProgram program) {
        if (program != null) {
            program.close();
        }
        return null;
    }

    private boolean isComputeSupported() {
        String version = GLES20.glGetString(GLES20.GL_VERSION);
        if (version == null) {
            return false;
        }
        return version.contains("OpenGL ES 3.1") || version.contains("OpenGL ES 3.2") || version.contains("OpenGL ES 3.0.1");
    }

    private static int createTexture(int width, int height) {
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES30.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, width, height, 0,
                GLES20.GL_RGBA, GLES30.GL_HALF_FLOAT, null);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return tex[0];
    }

    private void clearTexture(int texture) {
        if (texture == 0) {
            return;
        }
        int[] fbo = new int[1];
        GLES20.glGenFramebuffers(1, fbo, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, texture, 0);
        GLES20.glClearColor(0f, 0f, 0f, 0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glDeleteFramebuffers(1, fbo, 0);
    }

    private float[] paletteColor(int colorId) {
        switch (paletteId) {
            case 1:
                if (colorId % 2 == 0) {
                    return new float[]{0.2f, 0.6f, 1.0f};
                } else {
                    return new float[]{1.0f, 0.4f, 0.7f};
                }
            default:
                if (colorId % 2 == 0) {
                    return new float[]{1.2f, 0.5f, 0.2f};
                } else {
                    return new float[]{0.1f, 0.3f, 0.9f};
                }
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
    }

    private final class PingPongTexture {
        private final int[] textures = new int[2];
        private int index = 0;

        void allocate(int width, int height) {
            destroy();
            textures[0] = createTexture(width, height);
            textures[1] = createTexture(width, height);
            index = 0;
        }

        void destroy() {
            if (textures[0] != 0) {
                int[] tex = new int[]{textures[0]};
                GLES20.glDeleteTextures(1, tex, 0);
                textures[0] = 0;
            }
            if (textures[1] != 0) {
                int[] tex = new int[]{textures[1]};
                GLES20.glDeleteTextures(1, tex, 0);
                textures[1] = 0;
            }
            index = 0;
        }

        void clear() {
            clearTexture(textures[0]);
            clearTexture(textures[1]);
            index = 0;
        }

        int read() {
            return textures[index];
        }

        int write() {
            return textures[1 - index];
        }

        void swap() {
            index = 1 - index;
        }

        boolean isValid() {
            return textures[0] != 0 && textures[1] != 0;
        }
    }
}
