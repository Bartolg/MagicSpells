package com.example.fluidsim.ui;

import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;

import java.util.function.IntConsumer;

/**
 * Utility listener to reduce noise when wiring Spinner callbacks.
 */
class SimpleItemSelectedListener implements AdapterView.OnItemSelectedListener {

    private final IntConsumer consumer;

    SimpleItemSelectedListener(@NonNull IntConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        consumer.accept(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Ignored.
    }
}
