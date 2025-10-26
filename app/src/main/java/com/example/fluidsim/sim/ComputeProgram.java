package com.example.fluidsim.sim;

import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.GLES31;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Minimal wrapper for OpenGL ES compute shader programs.
 */
final class ComputeProgram implements AutoCloseable {

    private static final String TAG = "ComputeProgram";

    private final int program;

    private ComputeProgram(int program) {
        this.program = program;
    }

    static ComputeProgram create(@NonNull Context context, @NonNull String assetPath) {
        String source = readAsset(context, assetPath);
        int shader = GLES31.glCreateShader(GLES31.GL_COMPUTE_SHADER);
        GLES31.glShaderSource(shader, source);
        GLES31.glCompileShader(shader);
        int[] status = new int[1];
        GLES31.glGetShaderiv(shader, GLES31.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            String info = GLES31.glGetShaderInfoLog(shader);
            GLES31.glDeleteShader(shader);
            throw new IllegalStateException("Compute shader compile failed: " + info);
        }
        int program = GLES31.glCreateProgram();
        GLES31.glAttachShader(program, shader);
        GLES31.glLinkProgram(program);
        GLES31.glGetProgramiv(program, GLES31.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            String info = GLES31.glGetProgramInfoLog(program);
            Log.e(TAG, "Program link failed: " + info);
            GLES31.glDeleteProgram(program);
            GLES31.glDeleteShader(shader);
            throw new IllegalStateException("Unable to link compute program");
        }
        GLES31.glDeleteShader(shader);
        return new ComputeProgram(program);
    }

    int getProgram() {
        return program;
    }

    int getUniformLocation(@NonNull String name) {
        return GLES31.glGetUniformLocation(program, name);
    }

    void use() {
        GLES31.glUseProgram(program);
    }

    @Override
    public void close() {
        GLES31.glDeleteProgram(program);
    }

    private static String readAsset(@NonNull Context context, @NonNull String path) {
        AssetManager assets = context.getAssets();
        try (InputStream input = assets.open(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load shader asset " + path, e);
        }
    }
}
