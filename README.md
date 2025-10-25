# FluidSimAI Android Template

This repository contains a minimal Android project that demonstrates how to scaffold a fluid simulation playground that can be augmented with on-device AI effects using TensorFlow Lite. The project is intentionally lightweight so you can open it in Android Studio, drop in your model, and start iterating.

## Features

- Kotlin-based Android app targeting API 34 with a minimum of API 24.
- Canvas-based fluid effect placeholder (`FluidSimulationView`) that renders animated vortices at 60 FPS.
- TensorFlow Lite integration via `TFLiteEffectGenerator`, which loads `assets/effect_model.tflite` and exposes a simple `generateColorModifiers` API.
- Graceful fallback behavior when a TensorFlow Lite model is not present so the template runs out of the box.

## Project structure

```
.
├── build.gradle.kts              # Root Gradle configuration
├── settings.gradle.kts           # Module include list
└── app/
    ├── build.gradle.kts          # Android application module configuration
    ├── proguard-rules.pro        # Keeps TensorFlow Lite classes when minifying
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/fluidsim/
        │   ├── MainActivity.kt
        │   ├── ai/TFLiteEffectGenerator.kt
        │   └── simulation/FluidSimulationView.kt
        ├── assets/README_PLACE_MODEL.txt
        └── res/
            ├── layout/activity_main.xml
            └── values/*.xml
```

## Getting started

1. **Open the project** in Android Studio (Giraffe or newer).
2. **Sync Gradle**. If you do not have the Gradle wrapper installed locally, let Android Studio create it or run `gradle wrapper` from the project root.
3. **Run on a device or emulator**. The app launches in landscape orientation and renders the placeholder fluid visualization.
4. **Add your TensorFlow Lite model**. Copy your model file to `app/src/main/assets/effect_model.tflite`. Update the input/output shapes inside `TFLiteEffectGenerator` if your model signature differs from the default `(1) -> (3)` floats.

## Extending the template

- Replace the placeholder simulation in `FluidSimulationView` with your real-time solver. The class is lifecycle-aware and schedules redraws automatically.
- Use `TFLiteEffectGenerator.generateColorModifiers` inside the simulation loop to modulate particle colors, forces, or other parameters.
- Hook up Android sensors or touch input to drive the solver and feed contextual features into the TensorFlow Lite model.

## Requirements

- Android Studio with the Android SDK (API 34) installed.
- Android device or emulator running Android 7.0 (API 24) or later.
- TensorFlow Lite model compatible with the provided API for advanced effects (optional).

## License

This template is provided as-is. Feel free to adapt it to your project's needs.
