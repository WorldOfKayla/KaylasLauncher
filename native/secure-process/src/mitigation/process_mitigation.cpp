#include "mitigation/process_mitigation.hpp"

#include "core/error_state.hpp"
#include "mitigation/flags.hpp"

#include <windows.h>

namespace secure_process::mitigation {
namespace {

bool set_policy(const PROCESS_MITIGATION_POLICY policy,
                const void* value,
                const SIZE_T size,
                const char* name,
                std::vector<std::string>& failures) {
    if (SetProcessMitigationPolicy(policy, const_cast<void*>(value), size) != FALSE) {
        return true;
    }

    failures.emplace_back(
            std::string(name) + " failed: " + core::format_windows_error(GetLastError())
    );
    return false;
}

bool apply_safe_dll_search(std::vector<std::string>& failures) {
    const DWORD directories = LOAD_LIBRARY_SEARCH_APPLICATION_DIR
            | LOAD_LIBRARY_SEARCH_SYSTEM32
            | LOAD_LIBRARY_SEARCH_USER_DIRS;

    bool success = true;
    if (SetDefaultDllDirectories(directories) == FALSE) {
        failures.emplace_back(
                "SetDefaultDllDirectories failed: "
                        + core::format_windows_error(GetLastError())
        );
        success = false;
    }
    if (SetDllDirectoryW(L"") == FALSE) {
        failures.emplace_back(
                "SetDllDirectoryW failed: " + core::format_windows_error(GetLastError())
        );
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

    failures.emplace_back("DEP policy failed: " + core::format_windows_error(GetLastError()));
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

bool apply_image_load_policy(const std::uint32_t requested,
                             std::vector<std::string>& failures) {
    PROCESS_MITIGATION_IMAGE_LOAD_POLICY policy{};
    policy.NoRemoteImages = (requested & block_remote_images) != 0;
    policy.NoLowMandatoryLabelImages = (requested & block_low_integrity_images) != 0;
    policy.PreferSystem32Images = (requested & prefer_system32_images) != 0;
    return set_policy(
            ProcessImageLoadPolicy,
            &policy,
            sizeof(policy),
            "image-load policy",
            failures
    );
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

} // namespace

Result apply(const std::uint32_t requested) {
    Result result{requested, 0, 0, {}};

    const auto apply_flag = [&](const std::uint32_t flag, const bool success) {
        if ((requested & flag) == 0) {
            return;
        }
        if (success) {
            result.applied |= flag;
        } else {
            result.failed |= flag;
        }
    };

    if ((requested & safe_dll_search) != 0) {
        apply_flag(safe_dll_search, apply_safe_dll_search(result.failures));
    }
    if ((requested & dep) != 0) {
        apply_flag(dep, apply_dep(result.failures));
    }
    if ((requested & aslr) != 0) {
        apply_flag(aslr, apply_aslr(result.failures));
    }
    if ((requested & disable_extension_points) != 0) {
        apply_flag(
                disable_extension_points,
                apply_extension_point_disable(result.failures)
        );
    }

    const std::uint32_t requested_image_flags = requested & image_load_flags;
    if (requested_image_flags != 0) {
        const bool success = apply_image_load_policy(requested, result.failures);
        if (success) {
            result.applied |= requested_image_flags;
        } else {
            result.failed |= requested_image_flags;
        }
    }

    if ((requested & strict_handle_checks) != 0) {
        apply_flag(strict_handle_checks, apply_strict_handle_checks(result.failures));
    }
    return result;
}

std::uint64_t pack_result(const std::uint32_t applied, const std::uint32_t failed) {
    return static_cast<std::uint64_t>(applied)
            | (static_cast<std::uint64_t>(failed) << 32u);
}

} // namespace secure_process::mitigation
