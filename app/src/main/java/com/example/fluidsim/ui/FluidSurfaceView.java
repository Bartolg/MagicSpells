package com.example.fluidsim.ui;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fluidsim.gl.FluidRenderer;

/**
 * GLSurfaceView configured for OpenGL ES 3.1 rendering of the fluid simulation.
 */
public class FluidSurfaceView extends GLSurfaceView {

    private final FluidRenderer renderer;

    public FluidSurfaceView(@NonNull Context context) {
        this(context, null);
    }

    public FluidSurfaceView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(3);
        setPreserveEGLContextOnPause(true);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        renderer = new FluidRenderer(context.getApplicationContext());
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Touch handling is delegated to the activity to control color palette decisions.
        return super.onTouchEvent(event);
    }

    public FluidRenderer getRenderer() {
        return renderer;
    }
}
