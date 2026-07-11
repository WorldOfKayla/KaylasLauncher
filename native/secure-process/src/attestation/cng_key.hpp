#pragma once

#include <windows.h>
#include <ncrypt.h>

#include <cstdint>
#include <string>
#include <vector>

namespace secure_process::attestation {

class CngAttestationKey final {
public:
    CngAttestationKey() = default;
    ~CngAttestationKey();

    CngAttestationKey(const CngAttestationKey&) = delete;
    CngAttestationKey& operator=(const CngAttestationKey&) = delete;

    bool open_or_create(std::string& error);
    bool export_public_key(std::vector<std::uint8_t>& public_key, std::string& error) const;
    bool sign_payload(
            const std::string& payload,
            std::vector<std::uint8_t>& signature,
            std::string& error
    ) const;

private:
    void reset();

    NCRYPT_PROV_HANDLE provider_ = 0;
    NCRYPT_KEY_HANDLE key_ = 0;
};

} // namespace secure_process::attestation
