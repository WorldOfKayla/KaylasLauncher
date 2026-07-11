#include "audit/module_audit.hpp"

#include "audit/authenticode.hpp"
#include "core/error_state.hpp"
#include "core/text.hpp"

#include <windows.h>
#include <psapi.h>

#include <cstdint>
#include <sstream>
#include <vector>

namespace secure_process::audit {
namespace {

std::wstring module_path(HANDLE process, HMODULE module) {
    std::vector<wchar_t> buffer(32768);
    const DWORD length = GetModuleFileNameExW(
            process,
            module,
            buffer.data(),
            static_cast<DWORD>(buffer.size())
    );
    if (length == 0) {
        return {};
    }
    return std::wstring(buffer.data(), buffer.data() + length);
}

} // namespace

std::string loaded_modules_json() {
    HANDLE process = GetCurrentProcess();
    DWORD bytes_needed = 0;
    std::vector<HMODULE> modules(256);

    while (true) {
        if (EnumProcessModulesEx(
                process,
                modules.data(),
                static_cast<DWORD>(modules.size() * sizeof(HMODULE)),
                &bytes_needed,
                LIST_MODULES_ALL
        ) == FALSE) {
            return "{\"error\":\""
                    + core::json_escape(core::format_windows_error(GetLastError()))
                    + "\",\"modules\":[]}";
        }
        if (bytes_needed <= modules.size() * sizeof(HMODULE)) {
            modules.resize(bytes_needed / sizeof(HMODULE));
            break;
        }
        modules.resize((bytes_needed / sizeof(HMODULE)) + 32);
    }

    std::ostringstream output;
    output << "{\"processId\":" << GetCurrentProcessId() << ",\"modules\":[";
    for (std::size_t index = 0; index < modules.size(); ++index) {
        if (index != 0) {
            output << ',';
        }

        MODULEINFO module_info{};
        const BOOL info_ok = GetModuleInformation(
                process,
                modules[index],
                &module_info,
                sizeof(module_info)
        );
        const std::wstring wide_path = module_path(process, modules[index]);
        const std::string path = core::utf8_from_wide(wide_path);
        const LONG signature_status = verify_authenticode(wide_path);

        output << "{\"path\":\"" << core::json_escape(path) << "\""
               << ",\"baseAddress\":\"0x" << std::hex
               << reinterpret_cast<std::uintptr_t>(module_info.lpBaseOfDll)
               << std::dec << "\""
               << ",\"imageSize\":" << (info_ok != FALSE ? module_info.SizeOfImage : 0)
               << ",\"signatureTrusted\":"
               << (signature_status == ERROR_SUCCESS ? "true" : "false")
               << ",\"signatureStatus\":" << signature_status
               << '}';
    }
    output << "]}";
    return output.str();
}

} // namespace secure_process::audit
