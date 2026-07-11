#include "bootstrap/bootstrap_evidence.hpp"

#include "attestation/hashing.hpp"
#include "audit/authenticode.hpp"
#include "core/text.hpp"

#include <tlhelp32.h>

#include <algorithm>
#include <cwchar>
#include <vector>

namespace secure_process::bootstrap {
namespace {

std::wstring environment_value(const wchar_t* name) {
    const DWORD required = GetEnvironmentVariableW(name, nullptr, 0);
    if (required == 0) {
        return {};
    }
    std::wstring value(required, L'\0');
    const DWORD written = GetEnvironmentVariableW(
            name,
            value.data(),
            static_cast<DWORD>(value.size())
    );
    if (written == 0 || written >= value.size()) {
        return {};
    }
    value.resize(written);
    return value;
}

DWORD parent_process_id() {
    const DWORD current = GetCurrentProcessId();
    HANDLE snapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (snapshot == INVALID_HANDLE_VALUE) {
        return 0;
    }

    PROCESSENTRY32W entry{};
    entry.dwSize = sizeof(entry);
    DWORD parent = 0;
    if (Process32FirstW(snapshot, &entry) != FALSE) {
        do {
            if (entry.th32ProcessID == current) {
                parent = entry.th32ParentProcessID;
                break;
            }
        } while (Process32NextW(snapshot, &entry) != FALSE);
    }
    CloseHandle(snapshot);
    return parent;
}

std::wstring process_path(const DWORD process_id) {
    HANDLE process = OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, FALSE, process_id);
    if (process == nullptr) {
        return {};
    }
    std::vector<wchar_t> buffer(32768);
    DWORD length = static_cast<DWORD>(buffer.size());
    const BOOL success = QueryFullProcessImageNameW(
            process,
            0,
            buffer.data(),
            &length
    );
    CloseHandle(process);
    return success == FALSE ? std::wstring{} : std::wstring(buffer.data(), length);
}

bool same_path(const std::wstring& left, const std::wstring& right) {
    return !left.empty() && !right.empty() && _wcsicmp(left.c_str(), right.c_str()) == 0;
}

} // namespace

Evidence measure(std::string& error) {
    Evidence evidence;
    const std::wstring expected_pid_text = environment_value(L"KAYLAS_SECURE_BOOTSTRAP_PID");
    const std::wstring expected_path = environment_value(L"KAYLAS_SECURE_BOOTSTRAP_PATH");
    const std::wstring nonce = environment_value(L"KAYLAS_SECURE_BOOTSTRAP_NONCE");
    if (expected_pid_text.empty() || expected_path.empty() || nonce.empty()) {
        return evidence;
    }

    evidence.present = true;
    evidence.nonce = core::utf8_from_wide(nonce);
    try {
        evidence.process_id = static_cast<std::uint32_t>(std::stoul(expected_pid_text));
    } catch (...) {
        error = "Invalid secure bootstrap process id";
        return evidence;
    }

    const DWORD actual_parent = parent_process_id();
    const std::wstring actual_path = process_path(actual_parent);
    evidence.parent_verified = actual_parent == evidence.process_id
            && same_path(actual_path, expected_path);
    evidence.path = core::utf8_from_wide(actual_path);
    if (actual_path.empty()) {
        error = "Unable to resolve secure bootstrap process path";
        return evidence;
    }

    std::vector<std::uint8_t> digest;
    if (!attestation::sha256_file(actual_path, digest, error)) {
        return evidence;
    }
    evidence.sha256 = core::hex_encode(digest);
    evidence.signature_status = audit::verify_authenticode(actual_path);
    evidence.authenticode_trusted = evidence.signature_status == ERROR_SUCCESS;
    return evidence;
}

} // namespace secure_process::bootstrap
