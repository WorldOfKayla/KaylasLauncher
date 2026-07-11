#include "attestation/hashing.hpp"
#include "attestation/tpm_key.hpp"
#include "core/text.hpp"

#include <cstdint>
#include <iostream>
#include <string>
#include <vector>

int main() {
    secure_process::attestation::TpmAttestationKey key;
    std::string error;
    if (!key.open_or_create(error)) {
        std::cerr << "failureKind="
                  << (key.failure_kind()
                              == secure_process::attestation::TpmFailureKind::unavailable
                          ? "unavailable"
                          : "operational")
                  << '\n'
                  << "TPM key unavailable: " << error << '\n';
        return 2;
    }

    secure_process::attestation::TpmPlatformInfo info;
    if (!key.platform_info(info, error)) {
        std::cerr << "TPM metadata unavailable: " << error << '\n';
        return 3;
    }

    std::vector<std::uint8_t> public_key;
    if (!key.export_public_key(public_key, error)) {
        std::cerr << "TPM public key export failed: " << error << '\n';
        return 4;
    }

    std::vector<std::uint8_t> nonce;
    if (!secure_process::attestation::sha256_bytes(
            "SecureProcess-SP2-TPM-probe",
            nonce,
            error
    )) {
        std::cerr << "Nonce hash failed: " << error << '\n';
        return 5;
    }

    constexpr std::uint32_t pcr_mask = (1u << 0u)
            | (1u << 2u)
            | (1u << 4u)
            | (1u << 7u)
            | (1u << 11u);
    std::vector<std::uint8_t> quote;
    if (!key.create_platform_quote(nonce, pcr_mask, quote, error)) {
        std::cerr << "TPM quote failed: " << error << '\n';
        return 6;
    }

    std::vector<std::uint8_t> signature;
    if (!key.sign_payload("SecureProcess-SP2-signature-probe", signature, error)) {
        std::cerr << "TPM signature failed: " << error << '\n';
        return 7;
    }

    std::cout << "hardwareBacked=" << (info.hardware_backed ? "true" : "false") << '\n'
              << "implementationType=0x" << std::hex << info.implementation_type << std::dec << '\n'
              << "platformType=" << info.platform_type << '\n'
              << "providerVersion=" << info.provider_version << '\n'
              << "tpmVersion=" << info.tpm_version << '\n'
              << "manufacturerId=0x" << std::hex << info.manufacturer_id << std::dec << '\n'
              << "firmwareVersion=" << info.firmware_version << '\n'
              << "publicKeyBytes=" << public_key.size() << '\n'
              << "ekPublicBytes=" << info.ek_public.size() << '\n'
              << "ekCertificateBytes=" << info.ek_certificate.size() << '\n'
              << "quoteBytes=" << quote.size() << '\n'
              << "signatureBytes=" << signature.size() << '\n'
              << "quoteBase64=" << secure_process::core::base64_encode(quote) << '\n';
    return info.hardware_backed && !quote.empty() ? 0 : 8;
}
