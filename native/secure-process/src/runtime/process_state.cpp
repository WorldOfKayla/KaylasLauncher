#include "runtime/process_state.hpp"

#include <atomic>
#include <vector>

namespace secure_process::runtime {
namespace {

std::atomic<std::uint32_t> applied{0};
std::atomic<std::uint32_t> failed{0};
std::atomic<HMODULE> secure_module{nullptr};

std::wstring module_file_path(HMODULE module_handle) {
    std::vector<wchar_t> buffer(32768);
    const DWORD length = GetModuleFileNameW(
            module_handle,
            buffer.data(),
            static_cast<DWORD>(buffer.size())
    );
    return length == 0 ? std::wstring{} : std::wstring(buffer.data(), length);
}

} // namespace

void set_module(HMODULE module_handle) {
    secure_module.store(module_handle);
}

HMODULE module() {
    return secure_module.load();
}

void set_mitigation_state(const std::uint32_t applied_flags_value,
                          const std::uint32_t failed_flags_value) {
    applied.store(applied_flags_value);
    failed.store(failed_flags_value);
}

std::uint32_t applied_flags() {
    return applied.load();
}

std::uint32_t failed_flags() {
    return failed.load();
}

std::wstring current_process_path() {
    return module_file_path(nullptr);
}

std::wstring secure_process_path() {
    const HMODULE module_handle = module();
    return module_handle == nullptr ? std::wstring{} : module_file_path(module_handle);
}

} // namespace secure_process::runtime
