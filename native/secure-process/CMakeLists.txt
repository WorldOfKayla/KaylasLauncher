cmake_minimum_required(VERSION 3.24)
project(SecureProcess VERSION 0.4.0 LANGUAGES CXX)

if (NOT WIN32)
    message(FATAL_ERROR "SecureProcess currently supports Windows only")
endif()

find_package(JNI REQUIRED)


set(SECURE_PROCESS_DOWNGRADE_MARKERS
    "SOFTWARE_FALLBACK"
    "fallbackAllowed"
    "fallbackReasonCode"
    "allowSoftwareFallback"
    "softwareFallbackProtocol"
)
file(GLOB_RECURSE SECURE_PROCESS_NATIVE_SOURCE_FILES
    "${CMAKE_CURRENT_SOURCE_DIR}/src/*.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/src/*.hpp"
)
foreach(SECURE_PROCESS_SOURCE_FILE IN LISTS SECURE_PROCESS_NATIVE_SOURCE_FILES)
    get_filename_component(SECURE_PROCESS_SOURCE_NAME "${SECURE_PROCESS_SOURCE_FILE}" NAME)
    string(TOLOWER "${SECURE_PROCESS_SOURCE_NAME}" SECURE_PROCESS_SOURCE_NAME_LOWER)
    if(SECURE_PROCESS_SOURCE_NAME_LOWER MATCHES "fallback")
        message(FATAL_ERROR "Fail-closed policy rejected downgrade source file: ${SECURE_PROCESS_SOURCE_FILE}")
    endif()
    file(READ "${SECURE_PROCESS_SOURCE_FILE}" SECURE_PROCESS_SOURCE_CONTENT)
    foreach(SECURE_PROCESS_MARKER IN LISTS SECURE_PROCESS_DOWNGRADE_MARKERS)
        string(FIND "${SECURE_PROCESS_SOURCE_CONTENT}" "${SECURE_PROCESS_MARKER}" SECURE_PROCESS_MARKER_OFFSET)
        if(NOT SECURE_PROCESS_MARKER_OFFSET EQUAL -1)
            message(FATAL_ERROR "Fail-closed policy rejected downgrade marker in: ${SECURE_PROCESS_SOURCE_FILE}")
        endif()
    endforeach()
endforeach()

add_library(secure_process SHARED
    src/secure_process_jni.cpp
    src/secure_process_tpm_unavailable_jni.cpp
    src/core/text.cpp
    src/core/error_state.cpp
    src/runtime/process_state.cpp
    src/mitigation/process_mitigation.cpp
    src/audit/authenticode.cpp
    src/audit/module_audit.cpp
    src/attestation/hashing.cpp
    src/attestation/cng_key.cpp
    src/attestation/tpm_key.cpp
    src/attestation/attestation.cpp
    src/attestation/hardware_attestation.cpp
    src/attestation/tpm_unavailable_attestation.cpp
    src/bootstrap/bootstrap_evidence.cpp
)

target_compile_features(secure_process PRIVATE cxx_std_20)
target_include_directories(secure_process PRIVATE
    ${JNI_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/src
)
target_compile_definitions(secure_process PRIVATE
    WIN32_LEAN_AND_MEAN
    _WIN32_WINNT=0x0A00
    NTDDI_VERSION=NTDDI_WIN10_RS5
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
    bcrypt
    ncrypt
)

if (MSVC)
    target_compile_options(secure_process PRIVATE /W4 /WX /permissive- /guard:cf)
    target_link_options(secure_process PRIVATE /DYNAMICBASE /HIGHENTROPYVA /NXCOMPAT /GUARD:CF)
else()
    target_compile_options(secure_process PRIVATE -Wall -Wextra -Wpedantic -Werror)
endif()


set(SECURE_PROCESS_PINNED_LAUNCHER_SHA256 "" CACHE STRING
    "SHA-256 of the only launcher JAR accepted by secure_process_bootstrap")

add_executable(secure_process_bootstrap
    src/bootstrap/bootstrap_main.cpp
    src/core/text.cpp
    src/core/error_state.cpp
    src/runtime/process_state.cpp
    src/mitigation/process_mitigation.cpp
    src/audit/authenticode.cpp
    src/attestation/hashing.cpp
)
target_compile_features(secure_process_bootstrap PRIVATE cxx_std_20)
target_include_directories(secure_process_bootstrap PRIVATE
    ${JNI_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/src
)
target_compile_definitions(secure_process_bootstrap PRIVATE
    WIN32_LEAN_AND_MEAN
    _WIN32_WINNT=0x0A00
    NTDDI_VERSION=NTDDI_WIN10_RS5
    NOMINMAX
    UNICODE
    _UNICODE
    SECURE_PROCESS_VERSION="${PROJECT_VERSION}"
    SECURE_PROCESS_PINNED_LAUNCHER_SHA256="${SECURE_PROCESS_PINNED_LAUNCHER_SHA256}"
)
target_link_libraries(secure_process_bootstrap PRIVATE
    ${JNI_LIBRARIES}
    wintrust
    crypt32
    bcrypt
    ncrypt
)
if (MSVC)
    target_compile_options(secure_process_bootstrap PRIVATE /W4 /WX /permissive- /guard:cf)
    target_link_options(secure_process_bootstrap PRIVATE
        /DYNAMICBASE /HIGHENTROPYVA /NXCOMPAT /GUARD:CF
    )
endif()
set_target_properties(secure_process_bootstrap PROPERTIES
    OUTPUT_NAME "secure_process_bootstrap"
)

option(SECURE_PROCESS_BUILD_TOOLS "Build SecureProcess diagnostic tools" ON)
if (SECURE_PROCESS_BUILD_TOOLS)
    add_executable(secure_process_tpm_probe
        tools/tpm_probe.cpp
        src/core/text.cpp
        src/core/error_state.cpp
        src/attestation/hashing.cpp
        src/attestation/tpm_key.cpp
    )
    target_compile_features(secure_process_tpm_probe PRIVATE cxx_std_20)
    target_include_directories(secure_process_tpm_probe PRIVATE
        ${JNI_INCLUDE_DIRS}
        ${CMAKE_CURRENT_SOURCE_DIR}/src
    )
    target_compile_definitions(secure_process_tpm_probe PRIVATE
        WIN32_LEAN_AND_MEAN
        _WIN32_WINNT=0x0A00
        NTDDI_VERSION=NTDDI_WIN10_RS5
        NOMINMAX
        UNICODE
        _UNICODE
    )
    target_link_libraries(secure_process_tpm_probe PRIVATE
        ${JNI_LIBRARIES}
        crypt32
        bcrypt
        ncrypt
    )
    if (MSVC)
        target_compile_options(secure_process_tpm_probe PRIVATE /W4 /WX /permissive- /guard:cf)
        target_link_options(secure_process_tpm_probe PRIVATE /DYNAMICBASE /HIGHENTROPYVA /NXCOMPAT /GUARD:CF)
    endif()
endif()

set_target_properties(secure_process PROPERTIES
    OUTPUT_NAME "secure_process"
    PREFIX ""
)

install(TARGETS secure_process secure_process_bootstrap
    RUNTIME DESTINATION bin
    LIBRARY DESTINATION bin
    ARCHIVE DESTINATION lib
)
