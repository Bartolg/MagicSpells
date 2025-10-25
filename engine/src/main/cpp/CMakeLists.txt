cmake_minimum_required(VERSION 3.22.1)
project(fluid_engine)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(fluid_engine SHARED
    EngineNative.cpp
    VulkanContext.cpp
    GlContext.cpp
    SimulationController.cpp
)

find_library(log-lib log)

# We keep the graphics back ends stubbed out for now. The real implementation can
# link against Vulkan and OpenGL ES when available on device builds.

if(ANDROID)
    find_library(android-lib android)
    target_link_libraries(fluid_engine PRIVATE ${log-lib} ${android-lib})
else()
    target_link_libraries(fluid_engine PRIVATE ${log-lib})
endif()

# Enable hidden visibility by default to keep the exported JNI surface small.
set_target_properties(fluid_engine PROPERTIES
    CXX_VISIBILITY_PRESET hidden
    VISIBILITY_INLINES_HIDDEN YES
)
