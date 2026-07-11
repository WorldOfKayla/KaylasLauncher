#include "core/error_state.hpp"

#include "core/text.hpp"

#include <mutex>
#include <sstream>

namespace secure_process::core {
namespace {

std::mutex error_mutex;
std::string last_error;

} // namespace

std::string format_windows_error(const DWORD error) {
    if (error == ERROR_SUCCESS) {
        return "success";
    }

    LPWSTR buffer = nullptr;
    const DWORD flags = FORMAT_MESSAGE_ALLOCATE_BUFFER
            | FORMAT_MESSAGE_FROM_SYSTEM
            | FORMAT_MESSAGE_IGNORE_INSERTS;
    const DWORD length = FormatMessageW(
            flags,
            nullptr,
            error,
            MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
            reinterpret_cast<LPWSTR>(&buffer),
            0,
            nullptr
    );

    std::string result = "Win32 error " + std::to_string(error);
    if (length != 0 && buffer != nullptr) {
        std::wstring wide(buffer, buffer + length);
        std::string utf8 = utf8_from_wide(wide);
        while (!utf8.empty()
                && (utf8.back() == '\r' || utf8.back() == '\n' || utf8.back() == ' ')) {
            utf8.pop_back();
        }
        if (!utf8.empty()) {
            result += ": " + utf8;
        }
    }
    if (buffer != nullptr) {
        LocalFree(buffer);
    }
    return result;
}

void set_last_error_message(const std::vector<std::string>& failures) {
    std::lock_guard lock(error_mutex);
    if (failures.empty()) {
        last_error.clear();
        return;
    }

    std::ostringstream output;
    for (std::size_t index = 0; index < failures.size(); ++index) {
        if (index != 0) {
            output << "; ";
        }
        output << failures[index];
    }
    last_error = output.str();
}

std::string last_error_message() {
    std::lock_guard lock(error_mutex);
    return last_error;
}

} // namespace secure_process::core
