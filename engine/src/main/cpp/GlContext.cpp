#include "GlContext.h"

#include <android/log.h>

namespace {
constexpr const char* kTag = "GlContext";

class GlGraphicsContext final : public GraphicsContext {
public:
    void resize(uint32_t width, uint32_t height) override {
        __android_log_print(ANDROID_LOG_INFO, kTag, "Resize %ux%u", width, height);
    }

    void addTouchEmitter(const TouchEmitter& emitter) override {
        __android_log_print(ANDROID_LOG_INFO, kTag, "Emitter at (%f,%f)", emitter.x, emitter.y);
    }

    void updateFrame(float dt, float aiStrength) override {
        __android_log_print(ANDROID_LOG_VERBOSE, kTag, "Frame dt=%f ai=%f", dt, aiStrength);
        // Placeholder for OpenGL ES compute fallback.
    }
};
} // namespace

std::unique_ptr<GraphicsContext> createGlContext() {
    return std::make_unique<GlGraphicsContext>();
}
