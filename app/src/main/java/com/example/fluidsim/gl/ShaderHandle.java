package com.example.fluidsim.gl;

import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.GLES20;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utility wrapper around an OpenGL shader program.
 */
public final class ShaderHandle implements AutoCloseable {

    private static final String TAG = "ShaderHandle";

    private final int program;

    private ShaderHandle(int program) {
        this.program = program;
    }

    public static ShaderHandle createFullscreenProgram(@NonNull Context context) {
        String vertex = readAsset(context, "shaders/fullscreen.vert");
        String fragment = readAsset(context, "shaders/fullscreen.frag");
        int program = linkProgram(vertex, fragment);
        return new ShaderHandle(program);
    }

    public int getProgram() {
        return program;
    }

    @Override
    public void close() {
        GLES20.glDeleteProgram(program);
    }

    private static int linkProgram(@NonNull String vertexSource, @NonNull String fragmentSource) {
        int vertexShader = compile(GLES20.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compile(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glBindAttribLocation(program, 0, "aPosition");
        GLES20.glBindAttribLocation(program, 1, "aTexCoord");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Program link failed: " + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            throw new IllegalStateException("Unable to link program");
        }
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);
        return program;
    }

    private static int compile(int type, @NonNull String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            String info = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new IllegalStateException("Shader compile failed: " + info);
        }
        return shader;
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
