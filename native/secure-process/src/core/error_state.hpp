#pragma once

#include <windows.h>

#include <string>
#include <vector>

namespace secure_process::core {

std::string format_windows_error(DWORD error);
void set_last_error_message(const std::vector<std::string>& failures);
std::string last_error_message();

} // namespace secure_process::core
