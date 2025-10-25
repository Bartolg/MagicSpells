package com.example.fluidsim.ui;

import static com.example.fluidsim.R.id.fluidSurfaceView;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fluidsim.R;
import com.example.fluidsim.gl.FluidRenderer;
import com.example.fluidsim.ml.AiAssist;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The main activity wires UI controls to the OpenGL renderer. It exposes a minimal panel of
 * toggles that can be expanded to match the creative brief. The view hierarchy is intentionally
 * simple so it stays friendly to GLSurfaceView threading constraints.
 */
public class MainActivity extends AppCompatActivity {

    private FluidSurfaceView surfaceView;
    private FluidRenderer renderer;
    private AiAssist aiAssist;
    private Switch aiSwitch;
    private SeekBar aiStrength;
    private TextView statusText;

    private final ByteBuffer aiInput = ByteBuffer.allocateDirect(256 * 256 * 7)
            .order(ByteOrder.nativeOrder());
    private final ByteBuffer aiOutput = ByteBuffer.allocateDirect(256 * 256 * 5)
            .order(ByteOrder.nativeOrder());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(fluidSurfaceView);
        statusText = findViewById(R.id.statusText);
        aiSwitch = findViewById(R.id.aiToggle);
        aiStrength = findViewById(R.id.aiStrength);
        Spinner paletteSpinner = findViewById(R.id.paletteSpinner);
        Spinner gridSpinner = findViewById(R.id.gridSpinner);
        SeekBar pressureSeek = findViewById(R.id.pressureSeek);
        Switch bloomToggle = findViewById(R.id.bloomToggle);
        Switch particlesToggle = findViewById(R.id.particlesToggle);
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
        paletteSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener(pos ->
                surfaceView.queueEvent(() -> renderer.setPalette(pos))));

        ArrayAdapter<CharSequence> gridAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.grid_entries,
                android.R.layout.simple_spinner_item);
        gridAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gridSpinner.setAdapter(gridAdapter);
        gridSpinner.setSelection(1);
        gridSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener(pos ->
                surfaceView.queueEvent(() -> renderer.setQuality(pos == 0 ? 512 : 1024, 24))));

        pressureSeek.setMax(40);
        pressureSeek.setProgress(24);
        pressureSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                if (fromUser) {
                    surfaceView.queueEvent(() -> renderer.setQuality(renderer.getGridSize(), value));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        bloomToggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                surfaceView.queueEvent(() -> renderer.setBloomEnabled(isChecked)));
        particlesToggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                surfaceView.queueEvent(() -> renderer.setParticlesEnabled(isChecked)));

        resetButton.setOnClickListener(v -> surfaceView.queueEvent(renderer::reset));

        aiStrength.setMax(100);
        aiStrength.setProgress(50);
        aiStrength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float strength = progress / 100f;
                    surfaceView.queueEvent(() -> renderer.setAiEnabled(aiSwitch.isChecked(), strength));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        surfaceView.setOnTouchListener(this::handleSurfaceTouch);
        initAi();
    }

    private void initAi() {
        try {
            aiAssist = new AiAssist(this, "models/effects_unet_int8.tflite");
            renderer.setAiAssist(aiAssist, aiInput, aiOutput);
            aiSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                    surfaceView.queueEvent(() -> renderer.setAiEnabled(isChecked, aiStrength.getProgress() / 100f)));
            statusText.setText(R.string.status_ready);
        } catch (Exception ex) {
            statusText.setText(getString(R.string.status_ai_error, ex.getMessage()));
            aiSwitch.setEnabled(false);
        }
    }

    private void updateUiStatus(@NonNull FluidRenderer.RendererStats stats) {
        runOnUiThread(() -> {
            String fps = getString(R.string.status_template,
                    String.format("%.1f", stats.fps),
                    stats.gridSize,
                    stats.pressureIterations,
                    stats.temperatureLevel);
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
        int colorId = aiSwitch.isChecked() ? 1 : 0;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aiAssist != null) {
            aiAssist.close();
        }
    }
}
