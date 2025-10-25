#include "SimulationController.h"
#include "VulkanContext.h"
#include "GlContext.h"
#include "GraphicsContext.h"

#include <android/log.h>
#include <chrono>

namespace {
constexpr const char* kTag = "FluidController";
}

SimulationController::SimulationController() = default;
SimulationController::~SimulationController() = default;

void SimulationController::initialize(uint32_t width, uint32_t height, bool useVulkan) {
    width_ = width;
    height_ = height;
    useVulkan_ = useVulkan;
    ensureContext(useVulkan);
    if (graphics_) {
        graphics_->resize(width_, height_);
    }
    initialized_ = true;
}

void SimulationController::registerTouch(float x, float y, float dx, float dy, uint32_t color) {
    if (!initialized_) {
        __android_log_print(ANDROID_LOG_WARN, kTag, "Touch before init");
        return;
    }
    TouchEmitter emitter {x, y, dx, dy, color};
    emitters_.push_back(emitter);
    if (graphics_) {
        graphics_->addTouchEmitter(emitter);
    }
}

void SimulationController::update(float dt) {
    if (!graphics_) {
        ensureContext(useVulkan_);
    }
    if (graphics_) {
        graphics_->updateFrame(dt, aiStrength_);
    }
    updateEmitters();
}

void SimulationController::setAiStrength(float value) {
    aiStrength_ = value;
}

void SimulationController::ensureContext(bool useVulkan) {
    if (graphics_) {
        return;
    }
    if (useVulkan) {
        graphics_ = createVulkanContext();
        if (!graphics_) {
            __android_log_print(ANDROID_LOG_WARN, kTag, "Falling back to OpenGL context");
        }
    }
    if (!graphics_) {
        graphics_ = createGlContext();
    }
    if (!graphics_) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "Unable to create any graphics context");
    }
}

void SimulationController::updateEmitters() {
    if (emitters_.empty()) {
        return;
    }
    // Lightweight lifetime management stub; the compute back-end will handle
    // sophisticated emitter decay once implemented.
    emitters_.clear();
}
