#pragma once

#include <memory>
#include "GraphicsContext.h"

std::unique_ptr<GraphicsContext> createVulkanContext();
