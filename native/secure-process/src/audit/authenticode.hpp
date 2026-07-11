#pragma once

#include <windows.h>

#include <string>

namespace secure_process::audit {

LONG verify_authenticode(const std::wstring& path);

} // namespace secure_process::audit
