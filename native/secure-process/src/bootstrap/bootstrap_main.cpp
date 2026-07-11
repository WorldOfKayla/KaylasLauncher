#include "attestation/hashing.hpp"
#include "audit/authenticode.hpp"
#include "core/text.hpp"
#include "mitigation/process_mitigation.hpp"
#include "runtime/process_state.hpp"

#include <windows.h>
#include <bcrypt.h>

#include <cstdint>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>

#ifndef SECURE_PROCESS_PINNED_LAUNCHER_SHA256
#define SECURE_PROCESS_PINNED_LAUNCHER_SHA256 ""
#endif

namespace {

std::wstring quote_argument(const std::wstring& value) {
    if (value.find_first_of(L" \t\"") == std::wstring::npos) {
        return value;
    }
    std::wstring result = L"\"";
    std::size_t slashes = 0;
    for (const wchar_t character : value) {
        if (character == L'\\') {
            ++slashes;
            continue;
        }
        if (character == L'\"') {
            result.append(slashes * 2 + 1, L'\\');
            result.push_back(L'\"');
            slashes = 0;
            continue;
        }
        result.append(slashes, L'\\');
        slashes = 0;
        result.push_back(character);
    }
    result.append(slashes * 2, L'\\');
    result.push_back(L'\"');
    return result;
}

std::wstring current_path() {
    std::vector<wchar_t> buffer(32768);
    const DWORD length = GetModuleFileNameW(
            nullptr,
            buffer.data(),
            static_cast<DWORD>(buffer.size())
    );
    return length == 0 ? std::wstring{} : std::wstring(buffer.data(), length);
}

std::wstring random_nonce() {
    std::vector<std::uint8_t> bytes(32);
    if (BCryptGenRandom(
            nullptr,
            bytes.data(),
            static_cast<ULONG>(bytes.size()),
            BCRYPT_USE_SYSTEM_PREFERRED_RNG
    ) < 0) {
        return {};
    }
    const std::string encoded = secure_process::core::hex_encode(bytes);
    return std::wstring(encoded.begin(), encoded.end());
}

bool verify_hash(const std::wstring& path, const std::string& expected, std::string& error) {
    std::vector<std::uint8_t> digest;
    if (!secure_process::attestation::sha256_file(path, digest, error)) {
        return false;
    }
    const std::string actual = secure_process::core::hex_encode(digest);
    if (expected.empty()) {
        error = "Bootstrap was built without a pinned launcher SHA-256";
        return false;
    }
    if (_stricmp(actual.c_str(), expected.c_str()) != 0) {
        error = "Launcher SHA-256 does not match the bootstrap pin";
        return false;
    }
    return true;
}

void usage() {
    std::wcerr
            << L"Usage: secure_process_bootstrap.exe --launcher <jar> "
            << L"--secure-process <dll> --java <java.exe> "
            << L"[--require-signature] [--verify-only] [-- <launcher args...>]\n";
}

} // namespace

int wmain(const int argc, wchar_t** argv) {
    std::wstring launcher;
    std::wstring secure_process_dll;
    std::wstring java_executable;
    bool require_signature = false;
    bool verify_only = false;
    int launcher_arguments_index = argc;

    for (int index = 1; index < argc; ++index) {
        const std::wstring argument = argv[index];
        if (argument == L"--") {
            launcher_arguments_index = index + 1;
            break;
        }
        if (argument == L"--launcher" && index + 1 < argc) {
            launcher = argv[++index];
        } else if (argument == L"--secure-process" && index + 1 < argc) {
            secure_process_dll = argv[++index];
        } else if (argument == L"--java" && index + 1 < argc) {
            java_executable = argv[++index];
        } else if (argument == L"--require-signature") {
            require_signature = true;
        } else if (argument == L"--verify-only") {
            verify_only = true;
        } else {
            usage();
            return 2;
        }
    }

    if (launcher.empty() || secure_process_dll.empty() || java_executable.empty()) {
        usage();
        return 2;
    }

    const secure_process::mitigation::Result mitigations =
            secure_process::mitigation::apply(0xffu);
    if (mitigations.failed != 0) {
        std::cerr << "Bootstrap mitigation baseline failed\n";
        return 3;
    }

    std::string error;
    if (!verify_hash(launcher, SECURE_PROCESS_PINNED_LAUNCHER_SHA256, error)) {
        std::cerr << error << '\n';
        return 4;
    }
    if (GetFileAttributesW(java_executable.c_str()) == INVALID_FILE_ATTRIBUTES) {
        std::cerr << "Java executable does not exist\n";
        return 5;
    }

    const std::wstring bootstrap_path = current_path();
    if (bootstrap_path.empty()) {
        std::cerr << "Unable to resolve bootstrap path\n";
        return 6;
    }
    if (require_signature) {
        if (secure_process::audit::verify_authenticode(bootstrap_path) != ERROR_SUCCESS
                || secure_process::audit::verify_authenticode(secure_process_dll) != ERROR_SUCCESS
                || secure_process::audit::verify_authenticode(java_executable) != ERROR_SUCCESS) {
            std::cerr << "Bootstrap, SecureProcess DLL or Java runtime is not Authenticode trusted\n";
            return 7;
        }
    }

    const std::wstring nonce = random_nonce();
    if (nonce.empty()) {
        std::cerr << "Unable to generate bootstrap nonce\n";
        return 10;
    }

    std::vector<std::uint8_t> secure_process_digest;
    if (!secure_process::attestation::sha256_file(
            secure_process_dll,
            secure_process_digest,
            error
    )) {
        std::cerr << error << '\n';
        return 9;
    }
    const std::string secure_process_sha256 =
            secure_process::core::hex_encode(secure_process_digest);

    if (verify_only) {
        std::cout << "Launcher pin verified\n"
                  << "SecureProcess SHA-256: " << secure_process_sha256 << '\n'
                  << "Authenticode required: "
                  << (require_signature ? "true" : "false") << '\n';
        return 0;
    }

    SetEnvironmentVariableW(
            L"KAYLAS_SECURE_BOOTSTRAP_PID",
            std::to_wstring(GetCurrentProcessId()).c_str()
    );
    SetEnvironmentVariableW(L"KAYLAS_SECURE_BOOTSTRAP_PATH", bootstrap_path.c_str());
    SetEnvironmentVariableW(L"KAYLAS_SECURE_BOOTSTRAP_NONCE", nonce.c_str());
    SetEnvironmentVariableW(L"KAYLAS_SECURE_PROCESS_DLL", secure_process_dll.c_str());

    const std::wstring secure_process_hash_wide(
            secure_process_sha256.begin(),
            secure_process_sha256.end()
    );
    std::wostringstream command;
    command << quote_argument(java_executable)
            << L" -Dkaylas.secureProcess.required=true"
            << L" -Dkaylas.secureProcess.integrityRequired=true"
            << L" -Dkaylas.secureProcess.authenticodeRequired="
            << (require_signature ? L"true" : L"false")
            << L" -Dkaylas.secureProcess.attestationProfile=SP2"
            << L" -Dkaylas.secureProcess.library="
            << quote_argument(secure_process_dll)
            << L" -Dkaylas.secureProcess.sha256="
            << secure_process_hash_wide
            << L" -jar " << quote_argument(launcher);
    for (int index = launcher_arguments_index; index < argc; ++index) {
        command << L' ' << quote_argument(argv[index]);
    }
    std::wstring command_line = command.str();

    STARTUPINFOW startup{};
    startup.cb = sizeof(startup);
    PROCESS_INFORMATION process{};
    if (CreateProcessW(
            nullptr,
            command_line.data(),
            nullptr,
            nullptr,
            FALSE,
            CREATE_UNICODE_ENVIRONMENT,
            nullptr,
            nullptr,
            &startup,
            &process
    ) == FALSE) {
        std::cerr << "CreateProcessW failed: " << GetLastError() << '\n';
        return 8;
    }

    CloseHandle(process.hThread);
    WaitForSingleObject(process.hProcess, INFINITE);
    DWORD exit_code = 1;
    GetExitCodeProcess(process.hProcess, &exit_code);
    CloseHandle(process.hProcess);
    return static_cast<int>(exit_code);
}
