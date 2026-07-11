#pragma once

#include <cstdint>

namespace secure_process::mitigation {

inline constexpr std::uint32_t safe_dll_search = 1u << 0;
inline constexpr std::uint32_t dep = 1u << 1;
inline constexpr std::uint32_t aslr = 1u << 2;
inline constexpr std::uint32_t disable_extension_points = 1u << 3;
inline constexpr std::uint32_t block_remote_images = 1u << 4;
inline constexpr std::uint32_t block_low_integrity_images = 1u << 5;
inline constexpr std::uint32_t strict_handle_checks = 1u << 6;
inline constexpr std::uint32_t prefer_system32_images = 1u << 7;

inline constexpr std::uint32_t image_load_flags = block_remote_images
        | block_low_integrity_images
        | prefer_system32_images;

} // namespace secure_process::mitigation
