#include <jni.h>
#include <windows.h>
#include <psapi.h>
#include <softpub.h>
#include <wintrust.h>

#include <cstdint>
#include <iomanip>
#include <mutex>
#include <sstream>
#include <string>
#include <vector>

namespace {

constexpr std::uint32_t kSafeDllSearch = 1u << 0;
constexpr std::uint32_t kDep = 1u << 1;
constexpr std::uint32_t kAslr = 1u << 2;
constexpr std::uint32_t kDisableExtensionPoints = 1u << 3;
constexpr std::uint32_t kBlockRemoteImages = 1u << 4;
constexpr std::uint32_t kBlockLowIntegrityImages = 1u << 5;
constexpr std::uint32_t kStrictHandleChecks = 1u << 6;
constexpr std::uint32_t kPreferSystem32Images = 1u << 7;

std::mutex g_error_mutex;
std::string g_last_error;

std::string utf8_from_wide(const std::wstring& input) {
    if (input.empty()) {
        return {};
    }
    const int required = WideCharToMultiByte(
            CP_UTF8,
            WC_ERR_INVALID_CHARS,
            input.data(),
            static_cast<int>(input.size()),
            nullptr,
            0,
            nullptr,
            nullptr
    );
    if (required <= 0) {
        return {};
    }
    std::string output(static_cast<std::size_t>(required), '\0');
    WideCharToMultiByte(
            CP_UTF8,
            WC_ERR_INVALID_CHARS,
            input.data(),
            static_cast<int>(input.size()),
            output.data(),
            required,
            nullptr,
            nullptr
    );
    return output;
}

std::wstring wide_from_java(JNIEnv* environment, jstring value) {
    if (value == nullptr) {
        return {};
    }
    const jchar* chars = environment->GetStringChars(value, nullptr);
    if (chars == nullptr) {
        return {};
    }
    const jsize length = environment->GetStringLength(value);
    std::wstring output(
            reinterpret_cast<const wchar_t*>(chars),
            reinterpret_cast<const wchar_t*>(chars) + length
    );
    environment->ReleaseStringChars(value, chars);
    return output;
}

std::string json_escape(const std::string& input) {
    std::ostringstream output;
    for (const unsigned char character : input) {
        switch (character) {
            case '"': output << "\\\""; break;
            case '\\': output << "\\\\"; break;
            case '\b': output << "\\b"; break;
            case '\f': output << "\\f"; break;
            case '\n': output << "\\n"; break;
            case '\r': output << "\\r"; break;
            case '\t': output << "\\t"; break;
            default:
                if (character < 0x20) {
                    output << "\\u"
                           << std::hex << std::setw(4) << std::setfill('0')
                           << static_cast<int>(character)
                           << std::dec;
                } else {
                    output << static_cast<char>(character);
                }
        }
    }
    return output.str();
}

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
        while (!utf8.empty() && (utf8.back() == '\r' || utf8.back() == '\n' || utf8.back() == ' ')) {
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
    std::lock_guard lock(g_error_mutex);
    if (failures.empty()) {
        g_last_error.clear();
        return;
    }

    std::ostringstream output;
    for (std::size_t index = 0; index < failures.size(); ++index) {
        if (index != 0) {
            output << "; ";
        }
        output << failures[index];
    }
    g_last_error = output.str();
}

bool set_policy(const PROCESS_MITIGATION_POLICY policy,
                const void* value,
                const SIZE_T size,
                const char* name,
                std::vector<std::string>& failures) {
    if (SetProcessMitigationPolicy(policy, const_cast<void*>(value), size) != FALSE) {
        return true;
    }

    const DWORD error = GetLastError();
    failures.emplace_back(std::string(name) + " failed: " + format_windows_error(error));
    return false;
}

bool apply_safe_dll_search(std::vector<std::string>& failures) {
    const DWORD directories = LOAD_LIBRARY_SEARCH_APPLICATION_DIR
            | LOAD_LIBRARY_SEARCH_SYSTEM32
            | LOAD_LIBRARY_SEARCH_USER_DIRS;

    bool success = true;
    if (SetDefaultDllDirectories(directories) == FALSE) {
        failures.emplace_back("SetDefaultDllDirectories failed: " + format_windows_error(GetLastError()));
        success = false;
    }
    if (SetDllDirectoryW(L"") == FALSE) {
        failures.emplace_back("SetDllDirectoryW failed: " + format_windows_error(GetLastError()));
        success = false;
    }
    return success;
}

bool apply_dep(std::vector<std::string>& failures) {
    PROCESS_MITIGATION_DEP_POLICY policy{};
    policy.Enable = 1;
    policy.Permanent = 1;
    if (SetProcessMitigationPolicy(ProcessDEPPolicy, &policy, sizeof(policy)) != FALSE) {
        return true;
    }

    PROCESS_MITIGATION_DEP_POLICY existing{};
    if (GetProcessMitigationPolicy(
            GetCurrentProcess(),
            ProcessDEPPolicy,
            &existing,
            sizeof(existing)
    ) != FALSE && existing.Enable != 0) {
        return true;
}


    failures.emplace_back("DEP policy failed: " + format_windows_error(GetLastError()));
    return false;
}
bool apply_aslr(std::vector<std::string>& failures) {
    PROCESS_MITIGATION_ASLR_POLICY policy{};
    policy.EnableBottomUpRandomization = 1;
    policy.EnableHighEntropy = 1;
    return set_policy(ProcessASLRPolicy, &policy, sizeof(policy), "ASLR policy", failures);
}

bool apply_extension_point_disable(std::vector<std::string>& failures) {
    PROCESS_MITIGATION_EXTENSION_POINT_DISABLE_POLICY policy{};
    policy.DisableExtensionPoints = 1;
    return set_policy(
            ProcessExtensionPointDisablePolicy,
            &policy,
            sizeof(policy),
            "extension-point policy",
            failures
    );
}

bool apply_image_load_policy(const std::uint32_t requested, std::vector<std::string>& failures) {
    PROCESS_MITIGATION_IMAGE_LOAD_POLICY policy{};
    policy.NoRemoteImages = (requested & kBlockRemoteImages) != 0;
    policy.NoLowMandatoryLabelImages = (requested & kBlockLowIntegrityImages) != 0;
    policy.PreferSystem32Images = (requested & kPreferSystem32Images) != 0;
    return set_policy(ProcessImageLoadPolicy, &policy, sizeof(policy), "image-load policy", failures);
}

bool apply_strict_handle_checks(std::vector<std::string>& failures) {
    PROCESS_MITIGATION_STRICT_HANDLE_CHECK_POLICY policy{};
    policy.RaiseExceptionOnInvalidHandleReference = 1;
    policy.HandleExceptionsPermanentlyEnabled = 1;
    return set_policy(
            ProcessStrictHandleCheckPolicy,
            &policy,
            sizeof(policy),
            "strict-handle policy",
            failures
    );
}

std::uint64_t pack_result(const std::uint32_t applied, const std::uint32_t failed) {
    return static_cast<std::uint64_t>(applied)
            | (static_cast<std::uint64_t>(failed) << 32u);
}

LONG verify_authenticode(const std::wstring& path) {
    if (path.empty()) {
        return static_cast<LONG>(TRUST_E_NOSIGNATURE);
    }

    WINTRUST_FILE_INFO file_info{};
    file_info.cbStruct = sizeof(file_info);
    file_info.pcwszFilePath = path.c_str();

    WINTRUST_DATA trust_data{};
    trust_data.cbStruct = sizeof(trust_data);
    trust_data.dwUIChoice = WTD_UI_NONE;
    trust_data.fdwRevocationChecks = WTD_REVOKE_NONE;
    trust_data.dwUnionChoice = WTD_CHOICE_FILE;
    trust_data.pFile = &file_info;
    trust_data.dwStateAction = WTD_STATEACTION_VERIFY;
    trust_data.dwProvFlags = WTD_CACHE_ONLY_URL_RETRIEVAL | WTD_REVOCATION_CHECK_NONE;

    GUID action = WINTRUST_ACTION_GENERIC_VERIFY_V2;
    const LONG status = WinVerifyTrust(nullptr, &action, &trust_data);

    trust_data.dwStateAction = WTD_STATEACTION_CLOSE;
    WinVerifyTrust(nullptr, &action, &trust_data);
    return status;
}

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

std::string audit_loaded_modules_json() {
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
            return "{\"error\":\"" + json_escape(format_windows_error(GetLastError())) + "\",\"modules\":[]}";
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
        const std::string path = utf8_from_wide(wide_path);
        const LONG signature_status = verify_authenticode(wide_path);

        output << "{\"path\":\"" << json_escape(path) << "\""
               << ",\"baseAddress\":\"0x" << std::hex
               << reinterpret_cast<std::uintptr_t>(module_info.lpBaseOfDll)
               << std::dec << "\""
               << ",\"imageSize\":" << (info_ok != FALSE ? module_info.SizeOfImage : 0)
               << ",\"signatureTrusted\":" << (signature_status == ERROR_SUCCESS ? "true" : "false")
               << ",\"signatureStatus\":" << signature_status
               << '}';
    }
    output << "]}";
    return output.str();
}

} // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_org_takesome_launcher_security_SecureProcessNative_initialize(
        JNIEnv*,
        jclass,
        const jint requested_flags
) {
    const auto requested = static_cast<std::uint32_t>(requested_flags);
    std::uint32_t applied = 0;
    std::uint32_t failed = 0;
    std::vector<std::string> failures;

    const auto apply = [&](const std::uint32_t flag, const bool success) {
        if ((requested & flag) == 0) {
            return;
        }
        if (success) {
            applied |= flag;
        } else {
            failed |= flag;
        }
    };

    if ((requested & kSafeDllSearch) != 0) {
        apply(kSafeDllSearch, apply_safe_dll_search(failures));
    }
    if ((requested & kDep) != 0) {
        apply(kDep, apply_dep(failures));
    }
    if ((requested & kAslr) != 0) {
        apply(kAslr, apply_aslr(failures));
    }
    if ((requested & kDisableExtensionPoints) != 0) {
        apply(kDisableExtensionPoints, apply_extension_point_disable(failures));
    }

    const std::uint32_t image_flags = requested
            & (kBlockRemoteImages | kBlockLowIntegrityImages | kPreferSystem32Images);
    if (image_flags != 0) {
        const bool success = apply_image_load_policy(requested, failures);
        if (success) {
            applied |= image_flags;
        } else {
            failed |= image_flags;
        }
    }

    if ((requested & kStrictHandleChecks) != 0) {
        apply(kStrictHandleChecks, apply_strict_handle_checks(failures));
    }

    set_last_error_message(failures);
    return static_cast<jlong>(pack_result(applied, failed));
}

extern "C" JNIEXPORT jint JNICALL
Java_org_takesome_launcher_security_SecureProcessNative_verifyAuthenticode(
        JNIEnv* environment,
        jclass,
        jstring path
) {
    return static_cast<jint>(verify_authenticode(wide_from_java(environment, path)));
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_takesome_launcher_security_SecureProcessNative_auditLoadedModulesJson(
        JNIEnv* environment,
        jclass
) {
    const std::string json = audit_loaded_modules_json();
    return environment->NewStringUTF(json.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_takesome_launcher_security_SecureProcessNative_lastError(
        JNIEnv* environment,
        jclass
) {
    std::lock_guard lock(g_error_mutex);
    return environment->NewStringUTF(g_last_error.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_takesome_launcher_security_SecureProcessNative_version(
        JNIEnv* environment,
        jclass
) {
    return environment->NewStringUTF(SECURE_PROCESS_VERSION);
}

BOOL APIENTRY DllMain(HMODULE module, DWORD reason, LPVOID) {
    if (reason == DLL_PROCESS_ATTACH) {
        DisableThreadLibraryCalls(module);
    }
    return TRUE;
}
