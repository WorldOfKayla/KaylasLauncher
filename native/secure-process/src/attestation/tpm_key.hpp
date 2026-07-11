#pragma once

#include <windows.h>
#include <ncrypt.h>

#include <cstdint>
#include <string>
#include <vector>

namespace secure_process::attestation {

enum class TpmFailureKind {
    none,
    unavailable,
    operational
};

struct TpmPlatformInfo final {
    bool hardware_backed = false;
    std::uint32_t implementation_type = 0;
    std::uint32_t platform_type = 0;
    std::uint32_t provider_version = 0;
    std::uint32_t tpm_version = 0;
    std::uint32_t manufacturer_id = 0;
    std::uint64_t firmware_version = 0;
    std::vector<std::uint8_t> ek_public;
    std::vector<std::uint8_t> ek_certificate;
};

class TpmAttestationKey final {
public:
    TpmAttestationKey() = default;
    ~TpmAttestationKey();

    TpmAttestationKey(const TpmAttestationKey&) = delete;
    TpmAttestationKey& operator=(const TpmAttestationKey&) = delete;

    bool open_or_create(std::string& error);
    bool export_public_key(std::vector<std::uint8_t>& public_key, std::string& error) const;
    bool sign_payload(
            const std::string& payload,
            std::vector<std::uint8_t>& signature,
            std::string& error
    ) const;
    bool create_platform_quote(
            const std::vector<std::uint8_t>& nonce,
            std::uint32_t pcr_mask,
            std::vector<std::uint8_t>& quote,
            std::string& error
    ) const;
    bool platform_info(TpmPlatformInfo& info, std::string& error) const;
    [[nodiscard]] TpmFailureKind failure_kind() const noexcept;

private:
    bool read_property(
            NCRYPT_HANDLE handle,
            LPCWSTR property,
            std::vector<std::uint8_t>& value,
            std::string& error,
            bool optional = false
    ) const;
    void set_failure(SECURITY_STATUS status) const noexcept;
    void set_failure(TpmFailureKind kind) const noexcept;
    void clear_failure() const noexcept;
    void reset();

    NCRYPT_PROV_HANDLE provider_ = 0;
    NCRYPT_KEY_HANDLE key_ = 0;
    mutable TpmFailureKind failure_kind_ = TpmFailureKind::none;
};

} // namespace secure_process::attestation
