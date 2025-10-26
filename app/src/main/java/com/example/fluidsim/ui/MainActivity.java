package com.example.fluidsim.ui;

import static com.example.fluidsim.R.id.fluidSurfaceView;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fluidsim.R;
import com.example.fluidsim.gl.FluidRenderer;

/**
 * The main activity wires UI controls to the OpenGL renderer. It exposes a simple panel of
 * toggles that control the compute-based fluid simulation.
 */
public class MainActivity extends AppCompatActivity {

    private FluidSurfaceView surfaceView;
    private FluidRenderer renderer;
    private TextView statusText;
    private int currentPalette = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(fluidSurfaceView);
        statusText = findViewById(R.id.statusText);
        Spinner paletteSpinner = findViewById(R.id.paletteSpinner);
        Spinner gridSpinner = findViewById(R.id.gridSpinner);
        SeekBar pressureSeek = findViewById(R.id.pressureSeek);
        Button resetButton = findViewById(R.id.resetButton);

        renderer = surfaceView.getRenderer();
        renderer.setOnFrameListener(this::updateUiStatus);

        ArrayAdapter<CharSequence> paletteAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.palette_entries,
                android.R.layout.simple_spinner_item);
        paletteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        paletteSpinner.setAdapter(paletteAdapter);
        paletteSpinner.setSelection(0);
        paletteSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener(pos -> {
            currentPalette = pos;
            surfaceView.queueEvent(() -> renderer.setPalette(pos));
        }));
        surfaceView.queueEvent(() -> renderer.setPalette(currentPalette));

        ArrayAdapter<CharSequence> gridAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.grid_entries,
                android.R.layout.simple_spinner_item);
        gridAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gridSpinner.setAdapter(gridAdapter);
        gridSpinner.setSelection(1);
        gridSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener(pos -> {
            int gridSize = pos == 0 ? 512 : 1024;
            int iterations = renderer.getPressureIterations();
            surfaceView.queueEvent(() -> renderer.setQuality(gridSize, iterations));
        }));

        pressureSeek.setMax(40);
        pressureSeek.setProgress(renderer.getPressureIterations());
        pressureSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                if (fromUser) {
                    int gridSize = renderer.getGridSize();
                    surfaceView.queueEvent(() -> renderer.setQuality(gridSize, value));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        resetButton.setOnClickListener(v -> surfaceView.queueEvent(renderer::reset));

        surfaceView.setOnTouchListener(this::handleSurfaceTouch);
        statusText.setText(R.string.status_initializing);
    }

    private void updateUiStatus(@NonNull FluidRenderer.RendererStats stats) {
        runOnUiThread(() -> {
            String fps = getString(R.string.status_template,
                    stats.fps,
                    stats.gridSize,
                    stats.pressureIterations);
            statusText.setText(fps);
        });
    }

    private boolean handleSurfaceTouch(View v, MotionEvent event) {
        final int action = event.getActionMasked();
        final int pointerIndex = event.getActionIndex();
        final float x = event.getX(pointerIndex);
        final float y = event.getY(pointerIndex);
        final float dx;
        final float dy;
        if (action == MotionEvent.ACTION_MOVE && event.getHistorySize() > 0) {
            dx = x - event.getHistoricalX(pointerIndex, event.getHistorySize() - 1);
            dy = y - event.getHistoricalY(pointerIndex, event.getHistorySize() - 1);
        } else {
            dx = 0f;
            dy = 0f;
        }
        int colorId = currentPalette;
        surfaceView.queueEvent(() -> renderer.onTouch(x, y, dx, dy, colorId));
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        surfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        surfaceView.onPause();
    }
}
