#include <jni.h>
#include <windows.h>
#include <psapi.h>
#include <softpub.h>
#include <wintrust.h>
#include <bcrypt.h>
#include <ncrypt.h>
#include <wincrypt.h>

#include <array>
#include <atomic>
#include <chrono>
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
std::atomic<std::uint32_t> g_applied_flags{0};
std::atomic<std::uint32_t> g_failed_flags{0};
HMODULE g_module = nullptr;
constexpr wchar_t kAttestationKeyName[] = L"KaylasLauncher.SecureProcess.Attestation.v1";
constexpr char kAttestationProtocol[] = "SP1";

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


std::string hex_encode(const std::vector<std::uint8_t>& bytes) {
    std::ostringstream output;
    output << std::hex << std::setfill('0');
    for (const std::uint8_t value : bytes) {
        output << std::setw(2) << static_cast<unsigned int>(value);
    }
    return output.str();
}

std::string base64_encode(const std::vector<std::uint8_t>& bytes) {
    if (bytes.empty()) {
        return {};
    }
    DWORD required = 0;
    if (CryptBinaryToStringA(
            bytes.data(),
            static_cast<DWORD>(bytes.size()),
            CRYPT_STRING_BASE64 | CRYPT_STRING_NOCRLF,
            nullptr,
            &required
    ) == FALSE || required == 0) {
        return {};
    }
    std::string output(static_cast<std::size_t>(required), '\0');
    if (CryptBinaryToStringA(
            bytes.data(),
            static_cast<DWORD>(bytes.size()),
            CRYPT_STRING_BASE64 | CRYPT_STRING_NOCRLF,
            output.data(),
            &required
    ) == FALSE) {
        return {};
    }
    while (!output.empty() && output.back() == '\0') {
        output.pop_back();
    }
    return output;
}

bool sha256_begin(BCRYPT_ALG_HANDLE& algorithm,
                  BCRYPT_HASH_HANDLE& hash,
                  std::vector<std::uint8_t>& object,
                  std::vector<std::uint8_t>& digest,
                  std::string& error) {
    NTSTATUS status = BCryptOpenAlgorithmProvider(
            &algorithm,
            BCRYPT_SHA256_ALGORITHM,
            nullptr,
            0
    );
    if (status < 0) {
        error = "BCryptOpenAlgorithmProvider failed: " + std::to_string(status);
        return false;
    }

    DWORD object_length = 0;
    DWORD digest_length = 0;
    DWORD copied = 0;
    status = BCryptGetProperty(
            algorithm,
            BCRYPT_OBJECT_LENGTH,
            reinterpret_cast<PUCHAR>(&object_length),
            sizeof(object_length),
            &copied,
            0
    );
    if (status < 0 || object_length == 0) {
        error = "BCryptGetProperty(BCRYPT_OBJECT_LENGTH) failed: " + std::to_string(status);
        BCryptCloseAlgorithmProvider(algorithm, 0);
        algorithm = nullptr;
        return false;
    }
    status = BCryptGetProperty(
            algorithm,
            BCRYPT_HASH_LENGTH,
            reinterpret_cast<PUCHAR>(&digest_length),
            sizeof(digest_length),
            &copied,
            0
    );
    if (status < 0 || digest_length == 0) {
        error = "BCryptGetProperty(BCRYPT_HASH_LENGTH) failed: " + std::to_string(status);
        BCryptCloseAlgorithmProvider(algorithm, 0);
        algorithm = nullptr;
        return false;
    }

    object.resize(object_length);
    digest.resize(digest_length);
    status = BCryptCreateHash(
            algorithm,
            &hash,
            object.data(),
            static_cast<ULONG>(object.size()),
            nullptr,
            0,
            0
    );
    if (status < 0) {
        error = "BCryptCreateHash failed: " + std::to_string(status);
        BCryptCloseAlgorithmProvider(algorithm, 0);
        algorithm = nullptr;
        return false;
    }
    return true;
}

void sha256_close(BCRYPT_ALG_HANDLE algorithm, BCRYPT_HASH_HANDLE hash) {
    if (hash != nullptr) {
        BCryptDestroyHash(hash);
    }
    if (algorithm != nullptr) {
        BCryptCloseAlgorithmProvider(algorithm, 0);
    }
}

bool sha256_bytes(const std::string& value,
                  std::vector<std::uint8_t>& digest,
                  std::string& error) {
    BCRYPT_ALG_HANDLE algorithm = nullptr;
    BCRYPT_HASH_HANDLE hash = nullptr;
    std::vector<std::uint8_t> object;
    if (!sha256_begin(algorithm, hash, object, digest, error)) {
        return false;
    }
    const NTSTATUS update_status = BCryptHashData(
            hash,
            reinterpret_cast<PUCHAR>(const_cast<char*>(value.data())),
            static_cast<ULONG>(value.size()),
            0
    );
    if (update_status < 0) {
        error = "BCryptHashData failed: " + std::to_string(update_status);
        sha256_close(algorithm, hash);
        return false;
    }
    const NTSTATUS finish_status = BCryptFinishHash(
            hash,
            digest.data(),
            static_cast<ULONG>(digest.size()),
            0
    );
    if (finish_status < 0) {
        error = "BCryptFinishHash failed: " + std::to_string(finish_status);
        sha256_close(algorithm, hash);
        return false;
    }
    sha256_close(algorithm, hash);
    return true;
}

bool sha256_file(const std::wstring& path,
                 std::vector<std::uint8_t>& digest,
                 std::string& error) {
    HANDLE file = CreateFileW(
            path.c_str(),
            GENERIC_READ,
            FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE,
            nullptr,
            OPEN_EXISTING,
            FILE_ATTRIBUTE_NORMAL | FILE_FLAG_SEQUENTIAL_SCAN,
            nullptr
    );
    if (file == INVALID_HANDLE_VALUE) {
        error = "CreateFileW failed for attestation input: " + format_windows_error(GetLastError());
        return false;
    }

    BCRYPT_ALG_HANDLE algorithm = nullptr;
    BCRYPT_HASH_HANDLE hash = nullptr;
    std::vector<std::uint8_t> object;
    if (!sha256_begin(algorithm, hash, object, digest, error)) {
        CloseHandle(file);
        return false;
    }

    std::array<std::uint8_t, 64 * 1024> buffer{};
    DWORD read = 0;
    bool success = true;
    while (ReadFile(file, buffer.data(), static_cast<DWORD>(buffer.size()), &read, nullptr) != FALSE) {
        if (read == 0) {
            break;
        }
        const NTSTATUS status = BCryptHashData(hash, buffer.data(), read, 0);
        if (status < 0) {
            error = "BCryptHashData(file) failed: " + std::to_string(status);
            success = false;
            break;
        }
    }
    if (success && read != 0) {
        error = "ReadFile failed for attestation input: " + format_windows_error(GetLastError());
        success = false;
    }
    if (success) {
        const NTSTATUS status = BCryptFinishHash(
                hash,
                digest.data(),
                static_cast<ULONG>(digest.size()),
                0
        );
        if (status < 0) {
            error = "BCryptFinishHash(file) failed: " + std::to_string(status);
            success = false;
        }
    }
    sha256_close(algorithm, hash);
    CloseHandle(file);
    return success;
}

std::wstring current_process_path() {
    std::vector<wchar_t> buffer(32768);
    const DWORD length = GetModuleFileNameW(
            nullptr,
            buffer.data(),
            static_cast<DWORD>(buffer.size())
    );
    return length == 0 ? std::wstring{} : std::wstring(buffer.data(), length);
}

std::wstring secure_process_path() {
    if (g_module == nullptr) {
        return {};
    }
    std::vector<wchar_t> buffer(32768);
    const DWORD length = GetModuleFileNameW(
            g_module,
            buffer.data(),
            static_cast<DWORD>(buffer.size())
    );
    return length == 0 ? std::wstring{} : std::wstring(buffer.data(), length);
}

bool safe_attestation_field(const std::string& value) {
    return value.find('\n') == std::string::npos && value.find('\r') == std::string::npos;
}

bool open_or_create_attestation_key(NCRYPT_PROV_HANDLE& provider,
                                    NCRYPT_KEY_HANDLE& key,
                                    std::string& error) {
    SECURITY_STATUS status = NCryptOpenStorageProvider(
            &provider,
            MS_KEY_STORAGE_PROVIDER,
            0
    );
    if (status != ERROR_SUCCESS) {
        error = "NCryptOpenStorageProvider failed: " + std::to_string(status);
        return false;
    }

    status = NCryptOpenKey(
            provider,
            &key,
            kAttestationKeyName,
            0,
            NCRYPT_SILENT_FLAG
    );
    if (status == ERROR_SUCCESS) {
        return true;
    }
    if (status != NTE_BAD_KEYSET && status != NTE_NOT_FOUND) {
        error = "NCryptOpenKey failed: " + std::to_string(status);
        NCryptFreeObject(provider);
        provider = 0;
        return false;
    }

    status = NCryptCreatePersistedKey(
            provider,
            &key,
            NCRYPT_ECDSA_P256_ALGORITHM,
            kAttestationKeyName,
            0,
            NCRYPT_SILENT_FLAG
    );
    if (status != ERROR_SUCCESS) {
        error = "NCryptCreatePersistedKey failed: " + std::to_string(status);
        NCryptFreeObject(provider);
        provider = 0;
        return false;
    }

    DWORD key_usage = NCRYPT_ALLOW_SIGNING_FLAG;
    status = NCryptSetProperty(
            key,
            NCRYPT_KEY_USAGE_PROPERTY,
            reinterpret_cast<PBYTE>(&key_usage),
            sizeof(key_usage),
            0
    );
    if (status != ERROR_SUCCESS) {
        error = "NCryptSetProperty(key usage) failed: " + std::to_string(status);
        NCryptDeleteKey(key, 0);
        key = 0;
        NCryptFreeObject(provider);
        provider = 0;
        return false;
    }

    DWORD export_policy = 0;
    status = NCryptSetProperty(
            key,
            NCRYPT_EXPORT_POLICY_PROPERTY,
            reinterpret_cast<PBYTE>(&export_policy),
            sizeof(export_policy),
            0
    );
    if (status != ERROR_SUCCESS) {
        error = "NCryptSetProperty(export policy) failed: " + std::to_string(status);
        NCryptDeleteKey(key, 0);
        key = 0;
        NCryptFreeObject(provider);
        provider = 0;
        return false;
    }

    status = NCryptFinalizeKey(key, NCRYPT_SILENT_FLAG);
    if (status != ERROR_SUCCESS) {
        error = "NCryptFinalizeKey failed: " + std::to_string(status);
        NCryptDeleteKey(key, 0);
        key = 0;
        NCryptFreeObject(provider);
        provider = 0;
        return false;
    }
    return true;
}

void close_attestation_key(NCRYPT_PROV_HANDLE provider, NCRYPT_KEY_HANDLE key) {
    if (key != 0) {
        NCryptFreeObject(key);
    }
    if (provider != 0) {
        NCryptFreeObject(provider);
    }
}

bool export_attestation_public_key(NCRYPT_KEY_HANDLE key,
                                   std::vector<std::uint8_t>& public_key,
                                   std::string& error) {
    DWORD size = 0;
    SECURITY_STATUS status = NCryptExportKey(
            key,
            0,
            BCRYPT_ECCPUBLIC_BLOB,
            nullptr,
            nullptr,
            0,
            &size,
            0
    );
    if (status != ERROR_SUCCESS || size == 0) {
        error = "NCryptExportKey(size) failed: " + std::to_string(status);
        return false;
    }
    public_key.resize(size);
    status = NCryptExportKey(
            key,
            0,
            BCRYPT_ECCPUBLIC_BLOB,
            nullptr,
            public_key.data(),
            static_cast<DWORD>(public_key.size()),
            &size,
            0
    );
    if (status != ERROR_SUCCESS) {
        error = "NCryptExportKey failed: " + std::to_string(status);
        return false;
    }
    public_key.resize(size);
    return true;
}

bool sign_attestation_payload(NCRYPT_KEY_HANDLE key,
                              const std::string& payload,
                              std::vector<std::uint8_t>& signature,
                              std::string& error) {
    std::vector<std::uint8_t> digest;
    if (!sha256_bytes(payload, digest, error)) {
        return false;
    }

    DWORD size = 0;
    SECURITY_STATUS status = NCryptSignHash(
            key,
            nullptr,
            digest.data(),
            static_cast<DWORD>(digest.size()),
            nullptr,
            0,
            &size,
            0
    );
    if (status != ERROR_SUCCESS || size == 0) {
        error = "NCryptSignHash(size) failed: " + std::to_string(status);
        return false;
    }
    signature.resize(size);
    status = NCryptSignHash(
            key,
            nullptr,
            digest.data(),
            static_cast<DWORD>(digest.size()),
            signature.data(),
            static_cast<DWORD>(signature.size()),
            &size,
            0
    );
    if (status != ERROR_SUCCESS) {
        error = "NCryptSignHash failed: " + std::to_string(status);
        return false;
    }
    signature.resize(size);
    return true;
}

std::string attestation_error_json(const std::string& error) {
    return "{\"error\":\"" + json_escape(error) + "\"}";
}

std::string create_attestation_json(const std::string& challenge,
                                    const std::string& session_binding,
                                    const std::string& launcher_build_sha256,
                                    const std::string& launcher_version,
                                    const std::string& protocol_version) {
    if (challenge.empty() || session_binding.empty() || launcher_build_sha256.empty()) {
        return attestation_error_json("Attestation challenge, session and launcher build hash are required");
    }
    if (!safe_attestation_field(challenge)
            || !safe_attestation_field(session_binding)
            || !safe_attestation_field(launcher_build_sha256)
            || !safe_attestation_field(launcher_version)
            || !safe_attestation_field(protocol_version)) {
        return attestation_error_json("Attestation fields must not contain line breaks");
    }

    const std::wstring process_path_wide = current_process_path();
    const std::wstring secure_path_wide = secure_process_path();
    if (process_path_wide.empty() || secure_path_wide.empty()) {
        return attestation_error_json("Unable to resolve process or SecureProcess module path");
    }

    std::string error;
    std::vector<std::uint8_t> process_digest;
    std::vector<std::uint8_t> secure_digest;
    if (!sha256_file(process_path_wide, process_digest, error)) {
        return attestation_error_json(error);
    }
    if (!sha256_file(secure_path_wide, secure_digest, error)) {
        return attestation_error_json(error);
    }

    NCRYPT_PROV_HANDLE provider = 0;
    NCRYPT_KEY_HANDLE key = 0;
    if (!open_or_create_attestation_key(provider, key, error)) {
        return attestation_error_json(error);
    }

    std::vector<std::uint8_t> public_key;
    if (!export_attestation_public_key(key, public_key, error)) {
        close_attestation_key(provider, key);
        return attestation_error_json(error);
    }
    std::vector<std::uint8_t> key_digest;
    const std::string public_key_binary(
            reinterpret_cast<const char*>(public_key.data()),
            public_key.size()
    );
    if (!sha256_bytes(public_key_binary, key_digest, error)) {
        close_attestation_key(provider, key);
        return attestation_error_json(error);
    }

    const std::string process_hash = hex_encode(process_digest);
    const std::string secure_hash = hex_encode(secure_digest);
    const std::string key_id = hex_encode(key_digest);
    const auto issued_at = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()
    ).count();
    const std::uint32_t applied = g_applied_flags.load();
    const std::uint32_t failed = g_failed_flags.load();

    std::ostringstream canonical;
    canonical << kAttestationProtocol << '\n'
              << challenge << '\n'
              << session_binding << '\n'
              << launcher_build_sha256 << '\n'
              << launcher_version << '\n'
              << protocol_version << '\n'
              << issued_at << '\n'
              << GetCurrentProcessId() << '\n'
              << process_hash << '\n'
              << secure_hash << '\n'
              << SECURE_PROCESS_VERSION << '\n'
              << applied << '\n'
              << failed << '\n'
              << key_id;
    const std::string signed_payload = canonical.str();

    std::vector<std::uint8_t> signature;
    if (!sign_attestation_payload(key, signed_payload, signature, error)) {
        close_attestation_key(provider, key);
        return attestation_error_json(error);
    }
    close_attestation_key(provider, key);

    const std::vector<std::uint8_t> payload_bytes(
            signed_payload.begin(),
            signed_payload.end()
    );
    const std::string process_path = utf8_from_wide(process_path_wide);
    const std::string secure_path = utf8_from_wide(secure_path_wide);

    std::ostringstream json;
    json << "{\"version\":\"" << kAttestationProtocol << "\""
         << ",\"challenge\":\"" << json_escape(challenge) << "\""
         << ",\"session\":\"" << json_escape(session_binding) << "\""
         << ",\"launcherBuildSha256\":\"" << launcher_build_sha256 << "\""
         << ",\"launcherVersion\":\"" << json_escape(launcher_version) << "\""
         << ",\"protocolVersion\":\"" << json_escape(protocol_version) << "\""
         << ",\"issuedAt\":" << issued_at
         << ",\"processId\":" << GetCurrentProcessId()
         << ",\"processPath\":\"" << json_escape(process_path) << "\""
         << ",\"processSha256\":\"" << process_hash << "\""
         << ",\"secureProcessPath\":\"" << json_escape(secure_path) << "\""
         << ",\"secureProcessSha256\":\"" << secure_hash << "\""
         << ",\"nativeVersion\":\"" << SECURE_PROCESS_VERSION << "\""
         << ",\"appliedFlags\":" << applied
         << ",\"failedFlags\":" << failed
         << ",\"keyId\":\"" << key_id << "\""
         << ",\"publicKey\":\"" << base64_encode(public_key) << "\""
         << ",\"signedPayload\":\"" << base64_encode(payload_bytes) << "\""
         << ",\"signature\":\"" << base64_encode(signature) << "\"}"
         ;
    return json.str();
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

    g_applied_flags.store(applied);
    g_failed_flags.store(failed);
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
Java_org_takesome_launcher_security_SecureProcessNative_createAttestation(
        JNIEnv* environment,
        jclass,
        jstring challenge,
        jstring session_binding,
        jstring launcher_build_sha256,
        jstring launcher_version,
        jstring protocol_version
) {
    const std::string json = create_attestation_json(
            utf8_from_wide(wide_from_java(environment, challenge)),
            utf8_from_wide(wide_from_java(environment, session_binding)),
            utf8_from_wide(wide_from_java(environment, launcher_build_sha256)),
            utf8_from_wide(wide_from_java(environment, launcher_version)),
            utf8_from_wide(wide_from_java(environment, protocol_version))
    );
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
        g_module = module;
        DisableThreadLibraryCalls(module);
    }
    return TRUE;
}
