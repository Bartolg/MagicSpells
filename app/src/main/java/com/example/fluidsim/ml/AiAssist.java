package com.example.fluidsim.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;

import org.tensorflow.lite.Delegate;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thin Java wrapper that manages a TensorFlow Lite interpreter on a worker thread.
 */
public final class AiAssist implements AutoCloseable {

    private static final String TAG = "AiAssist";

    private final Interpreter interpreter;
    private final Delegate delegate;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AiAssist");
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final AtomicBoolean ready = new AtomicBoolean(false);

    public AiAssist(@NonNull Context context, @NonNull String assetModelPath) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.setUseNNAPI(true);
        delegate = maybeCreateGpuDelegate();
        if (delegate != null) {
            options.addDelegate(delegate);
        }
        MappedByteBuffer modelBuffer = loadModelFile(context, assetModelPath);
        interpreter = new Interpreter(modelBuffer, options);
        warmup();
        ready.set(true);
    }

    private void warmup() {
        try {
            ByteBuffer dummyInput = ByteBuffer.allocateDirect(256 * 256 * 7)
                    .order(java.nio.ByteOrder.nativeOrder());
            ByteBuffer dummyOutput = ByteBuffer.allocateDirect(256 * 256 * 5)
                    .order(java.nio.ByteOrder.nativeOrder());
            interpreter.run(dummyInput, dummyOutput);
        } catch (IllegalArgumentException iae) {
            Log.w(TAG, "Warmup skipped", iae);
        }
    }

    private static Delegate maybeCreateGpuDelegate() {
        try {
            return new GpuDelegate();
        } catch (Exception ex) {
            Log.w(TAG, "Unable to create GPU delegate", ex);
            return null;
        }
    }

    private static MappedByteBuffer loadModelFile(Context context, String path) throws IOException {
        AssetFileDescriptor afd = context.getAssets().openFd(path);
        try (FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
             FileChannel channel = fis.getChannel()) {
            long startOffset = afd.getStartOffset();
            long declaredLength = afd.getDeclaredLength();
            return channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    public boolean isReady() {
        return ready.get();
    }

    public void setEnabled(boolean on) {
        enabled.set(on);
    }

    public void enqueue(@NonNull ByteBuffer input, @NonNull ByteBuffer output) {
        if (!ready.get() || !enabled.get()) {
            return;
        }
        final ByteBuffer in = input.duplicate();
        final ByteBuffer out = output.duplicate();
        executor.execute(() -> interpreter.run(in, out));
    }

    @Override
    public void close() {
        executor.shutdownNow();
        interpreter.close();
        if (delegate != null) {
            delegate.close();
        }
    }
}
