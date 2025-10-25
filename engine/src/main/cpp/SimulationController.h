#pragma once

#include <cstdint>
#include <memory>
#include <vector>
#include "SimTypes.h"

class GraphicsContext;

class SimulationController {
public:
    SimulationController();
    ~SimulationController();

    void initialize(uint32_t width, uint32_t height, bool useVulkan);
    void registerTouch(float x, float y, float dx, float dy, uint32_t color);
    void update(float dt);
    void setAiStrength(float value);

private:
    void ensureContext(bool useVulkan);
    void updateEmitters();

    float aiStrength_ = 1.0f;
    uint32_t width_ = 0;
    uint32_t height_ = 0;
    bool initialized_ = false;
    bool useVulkan_ = false;
    std::unique_ptr<GraphicsContext> graphics_;
    std::vector<TouchEmitter> emitters_;
};
