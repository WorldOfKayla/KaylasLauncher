#pragma once

#include <windows.h>

#include <cstdint>
#include <string>

namespace secure_process::runtime {

void set_module(HMODULE module);
HMODULE module();

void set_mitigation_state(std::uint32_t applied, std::uint32_t failed);
std::uint32_t applied_flags();
std::uint32_t failed_flags();

std::wstring current_process_path();
std::wstring secure_process_path();

} // namespace secure_process::runtime
