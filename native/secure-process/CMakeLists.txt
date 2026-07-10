cmake_minimum_required(VERSION 3.24)
project(SecureProcess VERSION 0.2.0 LANGUAGES CXX)

if (NOT WIN32)
    message(FATAL_ERROR "SecureProcess currently supports Windows only")
endif()

find_package(JNI REQUIRED)

add_library(secure_process SHARED
    src/secure_process.cpp
)

target_compile_features(secure_process PRIVATE cxx_std_20)
target_include_directories(secure_process PRIVATE ${JNI_INCLUDE_DIRS})
target_compile_definitions(secure_process PRIVATE
    WIN32_LEAN_AND_MEAN
    _WIN32_WINNT=0x0A00
    NOMINMAX
    UNICODE
    _UNICODE
    SECURE_PROCESS_VERSION="${PROJECT_VERSION}"
)

target_link_libraries(secure_process PRIVATE
    ${JNI_LIBRARIES}
    wintrust
    crypt32
    psapi
)

if (MSVC)
    target_compile_options(secure_process PRIVATE /W4 /WX /permissive- /guard:cf)
    target_link_options(secure_process PRIVATE /DYNAMICBASE /HIGHENTROPYVA /NXCOMPAT /GUARD:CF)
else()
    target_compile_options(secure_process PRIVATE -Wall -Wextra -Wpedantic -Werror)
endif()

set_target_properties(secure_process PROPERTIES
    OUTPUT_NAME "secure_process"
    PREFIX ""
)

install(TARGETS secure_process
    RUNTIME DESTINATION bin
    LIBRARY DESTINATION bin
    ARCHIVE DESTINATION lib
)
