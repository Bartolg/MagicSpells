package com.example.fluidsim.sim;

import android.opengl.GLES20;
import android.opengl.GLES30;

import androidx.annotation.NonNull;

import java.nio.FloatBuffer;

/**
 * Helper for creating VAOs/VBOs used by the simulation rendering passes.
 */
final class GlObjects {

    private GlObjects() {
    }

    static int createFullscreenVao(@NonNull FloatBuffer quadBuffer) {
        int[] vao = new int[1];
        int[] vbo = new int[1];
        GLES30.glGenVertexArrays(1, vao, 0);
        GLES30.glGenBuffers(1, vbo, 0);
        GLES30.glBindVertexArray(vao[0]);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
        quadBuffer.position(0);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quadBuffer.capacity() * Float.BYTES, quadBuffer, GLES30.GL_STATIC_DRAW);
        int stride = 4 * Float.BYTES;
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, stride, 0);
        GLES30.glEnableVertexAttribArray(1);
        GLES30.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, stride, 2 * Float.BYTES);
        GLES30.glBindVertexArray(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        return vao[0];
    }
}
