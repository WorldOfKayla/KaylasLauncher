#include "attestation/tpm_key.hpp"

#include "attestation/hashing.hpp"

#include <bcrypt.h>

#include <cstring>
#include <iomanip>
#include <sstream>

#ifndef NCRYPT_ALLOW_KEY_ATTESTATION_FLAG
#define NCRYPT_ALLOW_KEY_ATTESTATION_FLAG 0x00000010
#endif

namespace secure_process::attestation {
namespace {

constexpr wchar_t attestation_key_name[] =
        L"KaylasLauncher.SecureProcess.TpmAttestation.v1";

std::uint32_t read_u32(const std::vector<std::uint8_t>& value) {
    std::uint32_t result = 0;
    if (value.size() >= sizeof(result)) {
        std::memcpy(&result, value.data(), sizeof(result));
    }
    return result;
}

bool is_tpm_unavailable_status(const SECURITY_STATUS status) noexcept {
    return status == NTE_DEVICE_NOT_READY
            || status == NTE_NOT_SUPPORTED
            || status == NTE_NOT_FOUND
            || status == NTE_BAD_PROVIDER
            || status == NTE_PROVIDER_DLL_FAIL;
}

std::string status_text(const SECURITY_STATUS status) {
    std::ostringstream output;
    switch (status) {
        case NTE_DEVICE_NOT_READY:
            output << "NTE_DEVICE_NOT_READY";
            break;
        case NTE_BAD_KEYSET:
            output << "NTE_BAD_KEYSET";
            break;
        case NTE_NOT_SUPPORTED:
            output << "NTE_NOT_SUPPORTED";
            break;
        case NTE_NOT_FOUND:
            output << "NTE_NOT_FOUND";
            break;
        default:
            output << "SECURITY_STATUS";
            break;
    }
    output << " (0x"
           << std::hex << std::uppercase << std::setw(8) << std::setfill('0')
           << static_cast<std::uint32_t>(status)
           << ')';
    return output.str();
}

std::uint64_t read_u64(const std::vector<std::uint8_t>& value) {
    std::uint64_t result = 0;
    if (value.size() >= sizeof(result)) {
        std::memcpy(&result, value.data(), sizeof(result));
    }
    return result;
}

} // namespace

TpmAttestationKey::~TpmAttestationKey() {
    reset();
}

bool TpmAttestationKey::open_or_create(std::string& error) {
    reset();
    clear_failure();

    SECURITY_STATUS status = NCryptOpenStorageProvider(
            &provider_,
            MS_PLATFORM_CRYPTO_PROVIDER,
            0
    );
    if (status != ERROR_SUCCESS) {
        set_failure(status);
        error = "Microsoft Platform Crypto Provider is unavailable: "
                + status_text(status);
        reset();
        return false;
    }

    std::vector<std::uint8_t> implementation;
    if (!read_property(
            provider_,
            NCRYPT_IMPL_TYPE_PROPERTY,
            implementation,
            error
    )) {
        reset();
        return false;
    }
    if ((read_u32(implementation) & NCRYPT_IMPL_HARDWARE_FLAG) == 0) {
        set_failure(TpmFailureKind::unavailable);
        error = "Platform Crypto Provider is not hardware backed";
        reset();
        return false;
    }

    status = NCryptOpenKey(
            provider_,
            &key_,
            attestation_key_name,
            0,
            NCRYPT_SILENT_FLAG
    );
    if (status == ERROR_SUCCESS) {
        return true;
    }
    if (status != NTE_BAD_KEYSET && status != NTE_NOT_FOUND) {
        set_failure(status);
        error = "NCryptOpenKey(TPM) failed: " + status_text(status);
        reset();
        return false;
    }

    status = NCryptCreatePersistedKey(
            provider_,
            &key_,
            NCRYPT_ECDSA_P256_ALGORITHM,
            attestation_key_name,
            0,
            NCRYPT_SILENT_FLAG
    );
    if (status != ERROR_SUCCESS) {
        set_failure(status);
        error = "NCryptCreatePersistedKey(TPM ECDSA P-256) failed: "
                + status_text(status);
        reset();
        return false;
    }

    DWORD key_usage = NCRYPT_ALLOW_SIGNING_FLAG | NCRYPT_ALLOW_KEY_ATTESTATION_FLAG;
    status = NCryptSetProperty(
            key_,
            NCRYPT_KEY_USAGE_PROPERTY,
            reinterpret_cast<PBYTE>(&key_usage),
            sizeof(key_usage),
            0
    );
    if (status != ERROR_SUCCESS) {
        set_failure(status);
        error = "NCryptSetProperty(TPM key usage) failed: " + status_text(status);
        NCryptDeleteKey(key_, 0);
        key_ = 0;
        reset();
        return false;
    }

    DWORD export_policy = 0;
    status = NCryptSetProperty(
            key_,
            NCRYPT_EXPORT_POLICY_PROPERTY,
            reinterpret_cast<PBYTE>(&export_policy),
            sizeof(export_policy),
            0
    );
    if (status != ERROR_SUCCESS) {
        set_failure(status);
        error = "NCryptSetProperty(TPM export policy) failed: "
                + status_text(status);
        NCryptDeleteKey(key_, 0);
        key_ = 0;
        reset();
        return false;
    }

    status = NCryptFinalizeKey(key_, NCRYPT_SILENT_FLAG);
    if (status != ERROR_SUCCESS) {
        set_failure(status);
        error = "NCryptFinalizeKey(TPM) failed: " + status_text(status);
        NCryptDeleteKey(key_, 0);
        key_ = 0;
        reset();
        return false;
    }
    return true;
}

bool TpmAttestationKey::export_public_key(
        std::vector<std::uint8_t>& public_key,
        std::string& error
) const {
    clear_failure();
    if (key_ == 0) {
        set_failure(TpmFailureKind::operational);
        error = "TPM attestation key is not open";
        return false;
    }

    DWORD size = 0;
    SECURITY_STATUS status = NCryptExportKey(
            key_,
            0,
            BCRYPT_ECCPUBLIC_BLOB,
            nullptr,
            nullptr,
            0,
            &size,
            0
    );
    if (status != ERROR_SUCCESS || size == 0) {
        set_failure(status == ERROR_SUCCESS ? TpmFailureKind::operational :
                                            (is_tpm_unavailable_status(status)
                                                     ? TpmFailureKind::unavailable
                                                     : TpmFailureKind::operational));
        error = "NCryptExportKey(TPM size) failed: " + status_text(status);
        return false;
    }

    public_key.resize(size);
    status = NCryptExportKey(
            key_,
            0,
            BCRYPT_ECCPUBLIC_BLOB,
            nullptr,
            public_key.data(),
            static_cast<DWORD>(public_key.size()),
            &size,
            0
    );
    if (status != ERROR_SUCCESS) {
        set_failure(status);
        error = "NCryptExportKey(TPM) failed: " + status_text(status);
        return false;
    }
    public_key.resize(size);
    return true;
}

bool TpmAttestationKey::sign_payload(
        const std::string& payload,
        std::vector<std::uint8_t>& signature,
        std::string& error
) const {
    clear_failure();
    if (key_ == 0) {
        set_failure(TpmFailureKind::operational);
        error = "TPM attestation key is not open";
        return false;
    }

    std::vector<std::uint8_t> digest;
    if (!sha256_bytes(payload, digest, error)) {
        set_failure(TpmFailureKind::operational);
        return false;
    }

    DWORD size = 0;
    SECURITY_STATUS status = NCryptSignHash(
            key_,
            nullptr,
            digest.data(),
            static_cast<DWORD>(digest.size()),
            nullptr,
            0,
            &size,
            0
    );
    if (status != ERROR_SUCCESS || size == 0) {
        set_failure(status == ERROR_SUCCESS ? TpmFailureKind::operational :
                                            (is_tpm_unavailable_status(status)
                                                     ? TpmFailureKind::unavailable
                                                     : TpmFailureKind::operational));
        error = "NCryptSignHash(TPM size) failed: " + status_text(status);
        return false;
    }

    signature.resize(size);
    status = NCryptSignHash(
            key_,
            nullptr,
            digest.data(),
            static_cast<DWORD>(digest.size()),
            signature.data(),
            static_cast<DWORD>(signature.size()),
            &size,
            0
    );
    if (status != ERROR_SUCCESS) {
        set_failure(status);
        error = "NCryptSignHash(TPM) failed: " + status_text(status);
        return false;
    }
    signature.resize(size);
    return true;
}

bool TpmAttestationKey::create_platform_quote(
        const std::vector<std::uint8_t>& nonce,
        const std::uint32_t pcr_mask,
        std::vector<std::uint8_t>& quote,
        std::string& error
) const {
    clear_failure();
    if (key_ == 0) {
        set_failure(TpmFailureKind::operational);
        error = "TPM attestation key is not open";
        return false;
    }
    if (nonce.empty()) {
        set_failure(TpmFailureKind::operational);
        error = "TPM quote nonce is empty";
        return false;
    }

    DWORD mutable_pcr_mask = pcr_mask;
    DWORD static_create = FALSE;
    NCryptBuffer buffers[] = {
            {
                    sizeof(mutable_pcr_mask),
                    NCRYPTBUFFER_TPM_PLATFORM_CLAIM_PCR_MASK,
                    reinterpret_cast<PBYTE>(&mutable_pcr_mask)
            },
            {
                    static_cast<ULONG>(nonce.size()),
                    NCRYPTBUFFER_TPM_PLATFORM_CLAIM_NONCE,
                    const_cast<PBYTE>(nonce.data())
            },
            {
                    sizeof(static_create),
                    NCRYPTBUFFER_TPM_PLATFORM_CLAIM_STATIC_CREATE,
                    reinterpret_cast<PBYTE>(&static_create)
            }
    };
    NCryptBufferDesc parameters{
            NCRYPTBUFFER_VERSION,
            static_cast<ULONG>(std::size(buffers)),
            buffers
    };

    DWORD required = 0;
    SECURITY_STATUS status = NCryptCreateClaim(
            key_,
            0,
            NCRYPT_CLAIM_PLATFORM,
            &parameters,
            nullptr,
            0,
            &required,
            0
    );
    if (status != ERROR_SUCCESS || required == 0) {
        set_failure(status == ERROR_SUCCESS ? TpmFailureKind::operational :
                                            (is_tpm_unavailable_status(status)
                                                     ? TpmFailureKind::unavailable
                                                     : TpmFailureKind::operational));
        error = "NCryptCreateClaim(TPM quote size) failed: "
                + status_text(status);
        return false;
    }

    quote.resize(required);
    status = NCryptCreateClaim(
            key_,
            0,
            NCRYPT_CLAIM_PLATFORM,
            &parameters,
            quote.data(),
            static_cast<DWORD>(quote.size()),
            &required,
            0
    );
    if (status != ERROR_SUCCESS) {
        set_failure(status);
        error = "NCryptCreateClaim(TPM quote) failed: " + status_text(status);
        return false;
    }
    quote.resize(required);
    return true;
}

bool TpmAttestationKey::platform_info(TpmPlatformInfo& info, std::string& error) const {
    clear_failure();
    if (provider_ == 0) {
        set_failure(TpmFailureKind::operational);
        error = "TPM provider is not open";
        return false;
    }

    std::vector<std::uint8_t> value;
    if (!read_property(provider_, NCRYPT_IMPL_TYPE_PROPERTY, value, error)) {
        return false;
    }
    info.implementation_type = read_u32(value);
    info.hardware_backed = (info.implementation_type & NCRYPT_IMPL_HARDWARE_FLAG) != 0;

    if (!read_property(provider_, NCRYPT_PCP_PLATFORM_TYPE_PROPERTY, value, error)) {
        return false;
    }
    info.platform_type = read_u32(value);

    if (!read_property(provider_, NCRYPT_PCP_PROVIDER_VERSION_PROPERTY, value, error)) {
        return false;
    }
    info.provider_version = read_u32(value);

    if (!read_property(provider_, NCRYPT_PCP_TPM_VERSION_PROPERTY, value, error)) {
        return false;
    }
    info.tpm_version = read_u32(value);

    if (!read_property(provider_, NCRYPT_PCP_TPM_MANUFACTURER_ID_PROPERTY, value, error)) {
        return false;
    }
    info.manufacturer_id = read_u32(value);

    if (!read_property(provider_, NCRYPT_PCP_TPM_FW_VERSION_PROPERTY, value, error)) {
        return false;
    }
    info.firmware_version = read_u64(value);

    if (!read_property(
            provider_,
            NCRYPT_PCP_EKPUB_PROPERTY,
            info.ek_public,
            error,
            true
    )) {
        return false;
    }
    if (info.ek_public.empty()
            && !read_property(
                    provider_,
                    NCRYPT_PCP_ECC_EKPUB_PROPERTY,
                    info.ek_public,
                    error,
                    true
            )) {
        return false;
    }
    if (!read_property(
            provider_,
            NCRYPT_PCP_EKCERT_PROPERTY,
            info.ek_certificate,
            error,
            true
    )) {
        return false;
    }
    if (info.ek_certificate.empty()
            && !read_property(
                    provider_,
                    NCRYPT_PCP_ECC_EKCERT_PROPERTY,
                    info.ek_certificate,
                    error,
                    true
            )) {
        return false;
    }
    return true;
}

bool TpmAttestationKey::read_property(
        const NCRYPT_HANDLE handle,
        const LPCWSTR property,
        std::vector<std::uint8_t>& value,
        std::string& error,
        const bool optional
) const {
    value.clear();
    DWORD required = 0;
    SECURITY_STATUS status = NCryptGetProperty(
            handle,
            property,
            nullptr,
            0,
            &required,
            0
    );
    if (status != ERROR_SUCCESS) {
        if (optional && (status == NTE_NOT_FOUND || status == NTE_NOT_SUPPORTED)) {
            return true;
        }
        set_failure(status);
        error = "NCryptGetProperty(size) failed: " + status_text(status);
        return false;
    }
    if (required == 0) {
        return true;
    }

    value.resize(required);
    status = NCryptGetProperty(
            handle,
            property,
            value.data(),
            static_cast<DWORD>(value.size()),
            &required,
            0
    );
    if (status != ERROR_SUCCESS) {
        if (optional && (status == NTE_NOT_FOUND || status == NTE_NOT_SUPPORTED)) {
            value.clear();
            return true;
        }
        set_failure(status);
        error = "NCryptGetProperty failed: " + status_text(status);
        value.clear();
        return false;
    }
    value.resize(required);
    return true;
}

TpmFailureKind TpmAttestationKey::failure_kind() const noexcept {
    return failure_kind_;
}

void TpmAttestationKey::set_failure(const SECURITY_STATUS status) const noexcept {
    failure_kind_ = is_tpm_unavailable_status(status)
            ? TpmFailureKind::unavailable
            : TpmFailureKind::operational;
}

void TpmAttestationKey::set_failure(const TpmFailureKind kind) const noexcept {
    failure_kind_ = kind;
}

void TpmAttestationKey::clear_failure() const noexcept {
    failure_kind_ = TpmFailureKind::none;
}

void TpmAttestationKey::reset() {
    if (key_ != 0) {
        NCryptFreeObject(key_);
        key_ = 0;
    }
    if (provider_ != 0) {
        NCryptFreeObject(provider_);
        provider_ = 0;
    }
}

} // namespace secure_process::attestation
