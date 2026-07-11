#include "attestation/attestation.hpp"

#include "attestation/cng_key.hpp"
#include "attestation/hashing.hpp"
#include "core/text.hpp"
#include "runtime/process_state.hpp"

#include <windows.h>

#include <chrono>
#include <cstdint>
#include <sstream>
#include <vector>

namespace secure_process::attestation {
namespace {

constexpr char protocol[] = "SP1";

bool safe_field(const std::string& value) {
    return value.find('\n') == std::string::npos
            && value.find('\r') == std::string::npos;
}

std::string error_json(const std::string& error) {
    return "{\"error\":\"" + core::json_escape(error) + "\"}";
}

} // namespace

std::string create_json(const std::string& challenge,
                        const std::string& session_binding,
                        const std::string& launcher_build_sha256,
                        const std::string& launcher_version,
                        const std::string& protocol_version) {
    if (challenge.empty() || session_binding.empty() || launcher_build_sha256.empty()) {
        return error_json(
                "Attestation challenge, session and launcher build hash are required"
        );
    }
    if (!safe_field(challenge)
            || !safe_field(session_binding)
            || !safe_field(launcher_build_sha256)
            || !safe_field(launcher_version)
            || !safe_field(protocol_version)) {
        return error_json("Attestation fields must not contain line breaks");
    }

    const std::wstring process_path_wide = runtime::current_process_path();
    const std::wstring secure_path_wide = runtime::secure_process_path();
    if (process_path_wide.empty() || secure_path_wide.empty()) {
        return error_json("Unable to resolve process or SecureProcess module path");
    }

    std::string error;
    std::vector<std::uint8_t> process_digest;
    std::vector<std::uint8_t> secure_digest;
    if (!sha256_file(process_path_wide, process_digest, error)) {
        return error_json(error);
    }
    if (!sha256_file(secure_path_wide, secure_digest, error)) {
        return error_json(error);
    }

    CngAttestationKey key;
    if (!key.open_or_create(error)) {
        return error_json(error);
    }

    std::vector<std::uint8_t> public_key;
    if (!key.export_public_key(public_key, error)) {
        return error_json(error);
    }

    std::vector<std::uint8_t> key_digest;
    const std::string public_key_binary(
            reinterpret_cast<const char*>(public_key.data()),
            public_key.size()
    );
    if (!sha256_bytes(public_key_binary, key_digest, error)) {
        return error_json(error);
    }

    const std::string process_hash = core::hex_encode(process_digest);
    const std::string secure_hash = core::hex_encode(secure_digest);
    const std::string key_id = core::hex_encode(key_digest);
    const auto issued_at = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()
    ).count();
    const std::uint32_t applied = runtime::applied_flags();
    const std::uint32_t failed = runtime::failed_flags();

    std::ostringstream canonical;
    canonical << protocol << '\n'
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
    if (!key.sign_payload(signed_payload, signature, error)) {
        return error_json(error);
    }

    const std::vector<std::uint8_t> payload_bytes(
            signed_payload.begin(),
            signed_payload.end()
    );
    const std::string process_path = core::utf8_from_wide(process_path_wide);
    const std::string secure_path = core::utf8_from_wide(secure_path_wide);

    std::ostringstream json;
    json << "{\"version\":\"" << protocol << "\""
         << ",\"challenge\":\"" << core::json_escape(challenge) << "\""
         << ",\"session\":\"" << core::json_escape(session_binding) << "\""
         << ",\"launcherBuildSha256\":\"" << launcher_build_sha256 << "\""
         << ",\"launcherVersion\":\"" << core::json_escape(launcher_version) << "\""
         << ",\"protocolVersion\":\"" << core::json_escape(protocol_version) << "\""
         << ",\"issuedAt\":" << issued_at
         << ",\"processId\":" << GetCurrentProcessId()
         << ",\"processPath\":\"" << core::json_escape(process_path) << "\""
         << ",\"processSha256\":\"" << process_hash << "\""
         << ",\"secureProcessPath\":\"" << core::json_escape(secure_path) << "\""
         << ",\"secureProcessSha256\":\"" << secure_hash << "\""
         << ",\"nativeVersion\":\"" << SECURE_PROCESS_VERSION << "\""
         << ",\"appliedFlags\":" << applied
         << ",\"failedFlags\":" << failed
         << ",\"keyId\":\"" << key_id << "\""
         << ",\"publicKey\":\"" << core::base64_encode(public_key) << "\""
         << ",\"signedPayload\":\"" << core::base64_encode(payload_bytes) << "\""
         << ",\"signature\":\"" << core::base64_encode(signature) << "\"}";
    return json.str();
}

} // namespace secure_process::attestation
