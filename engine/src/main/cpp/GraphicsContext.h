#pragma once

#include <cstdint>
#include <memory>
#include "SimTypes.h"

class GraphicsContext {
public:
    virtual ~GraphicsContext() = default;
    virtual void resize(uint32_t width, uint32_t height) = 0;
    virtual void addTouchEmitter(const TouchEmitter& emitter) = 0;
    virtual void updateFrame(float dt, float aiStrength) = 0;
};

