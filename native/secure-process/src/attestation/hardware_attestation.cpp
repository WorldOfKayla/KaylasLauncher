#include "attestation/hardware_attestation.hpp"

#include "attestation/hashing.hpp"
#include "attestation/tpm_key.hpp"
#include "bootstrap/bootstrap_evidence.hpp"
#include "core/text.hpp"
#include "runtime/process_state.hpp"

#include <windows.h>

#include <chrono>
#include <cstdint>
#include <sstream>
#include <vector>

namespace secure_process::attestation {
namespace {

constexpr char protocol[] = "SP2";
constexpr std::uint32_t required_pcr_mask = (1u << 0u)
        | (1u << 2u)
        | (1u << 4u)
        | (1u << 7u)
        | (1u << 11u);

bool safe_field(const std::string& value) {
    return value.find('\n') == std::string::npos
            && value.find('\r') == std::string::npos;
}

std::string error_json(const std::string& error) {
    return "{\"error\":\"" + core::json_escape(error) + "\"}";
}

std::string tpm_failure_json(const TpmAttestationKey&, const std::string& error) {
    return error_json(error);
}

std::string sha256_hex(const std::string& value, std::string& error) {
    std::vector<std::uint8_t> digest;
    if (!sha256_bytes(value, digest, error)) {
        return {};
    }
    return core::hex_encode(digest);
}

std::string sha256_hex(const std::vector<std::uint8_t>& value, std::string& error) {
    const std::string bytes(
            reinterpret_cast<const char*>(value.data()),
            value.size()
    );
    return sha256_hex(bytes, error);
}

} // namespace

std::string create_hardware_json(const std::string& challenge,
                                 const std::string& session_binding,
                                 const std::string& launcher_build_sha256,
                                 const std::string& launcher_version,
                                 const std::string& protocol_version) {
    if (challenge.empty() || session_binding.empty() || launcher_build_sha256.empty()) {
        return error_json(
                "Hardware attestation challenge, session and launcher build hash are required"
        );
    }
    if (!safe_field(challenge)
            || !safe_field(session_binding)
            || !safe_field(launcher_build_sha256)
            || !safe_field(launcher_version)
            || !safe_field(protocol_version)) {
        return error_json("Hardware attestation fields must not contain line breaks");
    }

    std::string error;
    TpmAttestationKey key;
    if (!key.open_or_create(error)) {
        return tpm_failure_json(key, error);
    }

    TpmPlatformInfo platform;
    if (!key.platform_info(platform, error)) {
        return tpm_failure_json(key, error);
    }
    if (!platform.hardware_backed) {
        return error_json(
                "TPM Platform Crypto Provider is not hardware backed"
        );
    }

    const bootstrap::Evidence bootstrap = bootstrap::measure(error);
    if (!bootstrap.present) {
        return error_json("Signed native bootstrap evidence is missing");
    }
    if (!bootstrap.parent_verified) {
        return error_json(
                error.empty()
                        ? "Signed native bootstrap is not the parent process"
                        : error
        );
    }

    const std::wstring process_path_wide = runtime::current_process_path();
    const std::wstring secure_path_wide = runtime::secure_process_path();
    if (process_path_wide.empty() || secure_path_wide.empty()) {
        return error_json("Unable to resolve process or SecureProcess module path");
    }

    std::vector<std::uint8_t> process_digest;
    std::vector<std::uint8_t> secure_digest;
    if (!sha256_file(process_path_wide, process_digest, error)) {
        return error_json(error);
    }
    if (!sha256_file(secure_path_wide, secure_digest, error)) {
        return error_json(error);
    }

    std::vector<std::uint8_t> public_key;
    if (!key.export_public_key(public_key, error)) {
        return tpm_failure_json(key, error);
    }

    const std::string key_id = sha256_hex(public_key, error);
    if (key_id.empty()) {
        return error_json(error);
    }
    if (platform.ek_public.empty()) {
        return error_json("TPM endorsement public key is unavailable");
    }
    const std::string ek_public_sha256 = sha256_hex(platform.ek_public, error);
    if (ek_public_sha256.empty()) {
        return error_json(error);
    }

    const std::string process_path = core::utf8_from_wide(process_path_wide);
    const std::string secure_path = core::utf8_from_wide(secure_path_wide);
    if (!safe_field(process_path)
            || !safe_field(secure_path)
            || !safe_field(bootstrap.path)
            || !safe_field(bootstrap.nonce)) {
        return error_json("Measured paths or bootstrap nonce contain line breaks");
    }

    const std::string process_hash = core::hex_encode(process_digest);
    const std::string secure_hash = core::hex_encode(secure_digest);
    const auto issued_at = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()
    ).count();
    const std::uint32_t applied = runtime::applied_flags();
    const std::uint32_t failed = runtime::failed_flags();

    std::ostringstream measurement;
    measurement << protocol << '\n'
                << challenge << '\n'
                << session_binding << '\n'
                << launcher_build_sha256 << '\n'
                << launcher_version << '\n'
                << protocol_version << '\n'
                << issued_at << '\n'
                << GetCurrentProcessId() << '\n'
                << process_path << '\n'
                << process_hash << '\n'
                << secure_path << '\n'
                << secure_hash << '\n'
                << bootstrap.process_id << '\n'
                << bootstrap.path << '\n'
                << bootstrap.sha256 << '\n'
                << SECURE_PROCESS_VERSION << '\n'
                << applied << '\n'
                << failed << '\n'
                << key_id << '\n'
                << platform.implementation_type << '\n'
                << platform.platform_type << '\n'
                << platform.provider_version << '\n'
                << required_pcr_mask << '\n'
                << platform.tpm_version << '\n'
                << platform.manufacturer_id << '\n'
                << platform.firmware_version << '\n'
                << ek_public_sha256 << '\n'
                << (bootstrap.present ? 1 : 0) << '\n'
                << (bootstrap.parent_verified ? 1 : 0) << '\n'
                << (bootstrap.authenticode_trusted ? 1 : 0) << '\n'
                << bootstrap.signature_status << '\n'
                << bootstrap.nonce;
    const std::string measurement_payload = measurement.str();

    std::vector<std::uint8_t> quote_nonce;
    if (!sha256_bytes(measurement_payload, quote_nonce, error)) {
        return error_json(error);
    }
    const std::string quote_nonce_hex = core::hex_encode(quote_nonce);

    std::vector<std::uint8_t> quote;
    if (!key.create_platform_quote(
            quote_nonce,
            required_pcr_mask,
            quote,
            error
    )) {
        return tpm_failure_json(key, error);
    }
    const std::string quote_sha256 = sha256_hex(quote, error);
    if (quote_sha256.empty()) {
        return error_json(error);
    }

    const std::string signed_payload = measurement_payload
            + '\n' + quote_nonce_hex
            + '\n' + quote_sha256;
    std::vector<std::uint8_t> signature;
    if (!key.sign_payload(signed_payload, signature, error)) {
        return tpm_failure_json(key, error);
    }

    const std::vector<std::uint8_t> payload_bytes(
            signed_payload.begin(),
            signed_payload.end()
    );
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
         << ",\"hardwareBacked\":true"
         << ",\"implementationType\":" << platform.implementation_type
         << ",\"platformType\":" << platform.platform_type
         << ",\"providerVersion\":" << platform.provider_version
         << ",\"tpmVersion\":" << platform.tpm_version
         << ",\"tpmManufacturerId\":" << platform.manufacturer_id
         << ",\"tpmFirmwareVersion\":" << platform.firmware_version
         << ",\"ekPublicSha256\":\"" << ek_public_sha256 << "\""
         << ",\"ekPublic\":\"" << core::base64_encode(platform.ek_public) << "\""
         << ",\"ekCertificate\":\""
         << core::base64_encode(platform.ek_certificate) << "\""
         << ",\"pcrMask\":" << required_pcr_mask
         << ",\"quoteNonce\":\"" << quote_nonce_hex << "\""
         << ",\"quoteSha256\":\"" << quote_sha256 << "\""
         << ",\"tpmQuote\":\"" << core::base64_encode(quote) << "\""
         << ",\"bootstrapPresent\":" << (bootstrap.present ? "true" : "false")
         << ",\"bootstrapParentVerified\":"
         << (bootstrap.parent_verified ? "true" : "false")
         << ",\"bootstrapAuthenticodeTrusted\":"
         << (bootstrap.authenticode_trusted ? "true" : "false")
         << ",\"bootstrapSignatureStatus\":" << bootstrap.signature_status
         << ",\"bootstrapProcessId\":" << bootstrap.process_id
         << ",\"bootstrapPath\":\"" << core::json_escape(bootstrap.path) << "\""
         << ",\"bootstrapSha256\":\"" << bootstrap.sha256 << "\""
         << ",\"bootstrapNonce\":\"" << core::json_escape(bootstrap.nonce) << "\""
         << ",\"signedPayload\":\"" << core::base64_encode(payload_bytes) << "\""
         << ",\"signature\":\"" << core::base64_encode(signature) << "\"}";
    return json.str();
}

} // namespace secure_process::attestation
