package com.example.fluidsim.ui

data class FluidUiState(
    val style: Style = Style.Warm,
    val aiEnabled: Boolean = true,
    val aiStrength: Float = 0.6f,
    val particlesEnabled: Boolean = true,
    val bloomEnabled: Boolean = true,
    val gridVisible: Boolean = false,
    val gridResolution: Int = 512,
    val pressureIterations: Int = 30,
    val fps: Float = 0f,
    val recording: Boolean = false,
    val recordPermissionGranted: Boolean = false,
) {
    enum class Style(val displayName: String) {
        Warm("Warm"),
        Cool("Cool")
    }
}
