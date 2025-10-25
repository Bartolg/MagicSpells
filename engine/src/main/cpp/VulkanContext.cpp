#include "VulkanContext.h"

#include <android/log.h>

namespace {
constexpr const char* kTag = "VulkanContext";

class VulkanGraphicsContext final : public GraphicsContext {
public:
    void resize(uint32_t width, uint32_t height) override {
        __android_log_print(ANDROID_LOG_INFO, kTag, "Resize %ux%u", width, height);
        (void)width;
        (void)height;
    }

    void addTouchEmitter(const TouchEmitter& emitter) override {
        __android_log_print(ANDROID_LOG_INFO, kTag, "Emitter at (%f,%f)", emitter.x, emitter.y);
        (void)emitter;
    }

    void updateFrame(float dt, float aiStrength) override {
        __android_log_print(ANDROID_LOG_VERBOSE, kTag, "Frame dt=%f ai=%f", dt, aiStrength);
        // Stub update. Real implementation will dispatch compute passes via Vulkan.
    }
};
} // namespace

std::unique_ptr<GraphicsContext> createVulkanContext() {
    // In environments without Vulkan support the loader may fail. We return
    // nullptr in that case and allow SimulationController to fall back.
#if defined(__ANDROID__)
    return std::make_unique<VulkanGraphicsContext>();
#else
    return nullptr;
#endif
}
