#include "attestation/cng_key.hpp"

#include "attestation/hashing.hpp"

#include <bcrypt.h>

namespace secure_process::attestation {
namespace {

constexpr wchar_t attestation_key_name[] =
        L"KaylasLauncher.SecureProcess.Attestation.v1";

} // namespace

CngAttestationKey::~CngAttestationKey() {
    reset();
}

bool CngAttestationKey::open_or_create(std::string& error) {
    reset();

    SECURITY_STATUS status = NCryptOpenStorageProvider(
            &provider_,
            MS_KEY_STORAGE_PROVIDER,
            0
    );
    if (status != ERROR_SUCCESS) {
        error = "NCryptOpenStorageProvider failed: " + std::to_string(status);
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
        error = "NCryptOpenKey failed: " + std::to_string(status);
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
        error = "NCryptCreatePersistedKey failed: " + std::to_string(status);
        reset();
        return false;
    }

    DWORD key_usage = NCRYPT_ALLOW_SIGNING_FLAG;
    status = NCryptSetProperty(
            key_,
            NCRYPT_KEY_USAGE_PROPERTY,
            reinterpret_cast<PBYTE>(&key_usage),
            sizeof(key_usage),
            0
    );
    if (status != ERROR_SUCCESS) {
        error = "NCryptSetProperty(key usage) failed: " + std::to_string(status);
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
        error = "NCryptSetProperty(export policy) failed: " + std::to_string(status);
        NCryptDeleteKey(key_, 0);
        key_ = 0;
        reset();
        return false;
    }

    status = NCryptFinalizeKey(key_, NCRYPT_SILENT_FLAG);
    if (status != ERROR_SUCCESS) {
        error = "NCryptFinalizeKey failed: " + std::to_string(status);
        NCryptDeleteKey(key_, 0);
        key_ = 0;
        reset();
        return false;
    }
    return true;
}

bool CngAttestationKey::export_public_key(
        std::vector<std::uint8_t>& public_key,
        std::string& error
) const {
    if (key_ == 0) {
        error = "Attestation key is not open";
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
        error = "NCryptExportKey(size) failed: " + std::to_string(status);
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
        error = "NCryptExportKey failed: " + std::to_string(status);
        return false;
    }
    public_key.resize(size);
    return true;
}

bool CngAttestationKey::sign_payload(
        const std::string& payload,
        std::vector<std::uint8_t>& signature,
        std::string& error
) const {
    if (key_ == 0) {
        error = "Attestation key is not open";
        return false;
    }

    std::vector<std::uint8_t> digest;
    if (!sha256_bytes(payload, digest, error)) {
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
        error = "NCryptSignHash(size) failed: " + std::to_string(status);
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
        error = "NCryptSignHash failed: " + std::to_string(status);
        return false;
    }
    signature.resize(size);
    return true;
}

void CngAttestationKey::reset() {
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
