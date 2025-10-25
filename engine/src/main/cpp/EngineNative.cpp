#include <jni.h>
#include <android/log.h>
#include <mutex>
#include <memory>
#include "SimulationController.h"

namespace {
constexpr const char* kTag = "FluidEngine";

std::mutex gMutex;
std::unique_ptr<SimulationController> gController;

SimulationController* controller() {
    std::scoped_lock lock(gMutex);
    if (!gController) {
        gController = std::make_unique<SimulationController>();
    }
    return gController.get();
}
} // namespace

extern "C" JNIEXPORT void JNICALL
Java_Engine_init(JNIEnv* env, jobject /*thiz*/, jint width, jint height, jboolean useVulkan) {
    (void)env;
    __android_log_print(ANDROID_LOG_INFO, kTag, "Init request: %dx%d Vulkan=%d", width, height, useVulkan);
    controller()->initialize(static_cast<uint32_t>(width), static_cast<uint32_t>(height), useVulkan);
}

extern "C" JNIEXPORT void JNICALL
Java_Engine_touch(JNIEnv* env, jobject /*thiz*/, jfloat x, jfloat y, jfloat dx, jfloat dy, jint color) {
    (void)env;
    controller()->registerTouch(x, y, dx, dy, color);
}

extern "C" JNIEXPORT void JNICALL
Java_Engine_frame(JNIEnv* env, jobject /*thiz*/, jfloat dt) {
    (void)env;
    controller()->update(dt);
}

extern "C" JNIEXPORT void JNICALL
Java_Engine_setAiStrength(JNIEnv* env, jobject /*thiz*/, jfloat strength) {
    (void)env;
    controller()->setAiStrength(strength);
}
