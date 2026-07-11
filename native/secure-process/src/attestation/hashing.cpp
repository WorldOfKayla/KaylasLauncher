#include "attestation/hashing.hpp"

#include "core/error_state.hpp"

#include <windows.h>
#include <bcrypt.h>

#include <array>

namespace secure_process::attestation {
namespace {

class HashContext final {
public:
    HashContext() = default;
    ~HashContext() {
        if (hash_ != nullptr) {
            BCryptDestroyHash(hash_);
        }
        if (algorithm_ != nullptr) {
            BCryptCloseAlgorithmProvider(algorithm_, 0);
        }
    }

    HashContext(const HashContext&) = delete;
    HashContext& operator=(const HashContext&) = delete;

    bool initialize(std::vector<std::uint8_t>& digest, std::string& error) {
        NTSTATUS status = BCryptOpenAlgorithmProvider(
                &algorithm_,
                BCRYPT_SHA256_ALGORITHM,
                nullptr,
                0
        );
        if (status < 0) {
            error = "BCryptOpenAlgorithmProvider failed: " + std::to_string(status);
            return false;
        }

        DWORD object_length = 0;
        DWORD digest_length = 0;
        DWORD copied = 0;
        status = BCryptGetProperty(
                algorithm_,
                BCRYPT_OBJECT_LENGTH,
                reinterpret_cast<PUCHAR>(&object_length),
                sizeof(object_length),
                &copied,
                0
        );
        if (status < 0 || object_length == 0) {
            error = "BCryptGetProperty(BCRYPT_OBJECT_LENGTH) failed: "
                    + std::to_string(status);
            return false;
        }
        status = BCryptGetProperty(
                algorithm_,
                BCRYPT_HASH_LENGTH,
                reinterpret_cast<PUCHAR>(&digest_length),
                sizeof(digest_length),
                &copied,
                0
        );
        if (status < 0 || digest_length == 0) {
            error = "BCryptGetProperty(BCRYPT_HASH_LENGTH) failed: "
                    + std::to_string(status);
            return false;
        }

        object_.resize(object_length);
        digest.resize(digest_length);
        status = BCryptCreateHash(
                algorithm_,
                &hash_,
                object_.data(),
                static_cast<ULONG>(object_.size()),
                nullptr,
                0,
                0
        );
        if (status < 0) {
            error = "BCryptCreateHash failed: " + std::to_string(status);
            return false;
        }
        return true;
    }

    bool update(const std::uint8_t* data, const ULONG size, std::string& error) {
        const NTSTATUS status = BCryptHashData(
                hash_,
                const_cast<PUCHAR>(data),
                size,
                0
        );
        if (status < 0) {
            error = "BCryptHashData failed: " + std::to_string(status);
            return false;
        }
        return true;
    }

    bool finish(std::vector<std::uint8_t>& digest, std::string& error) {
        const NTSTATUS status = BCryptFinishHash(
                hash_,
                digest.data(),
                static_cast<ULONG>(digest.size()),
                0
        );
        if (status < 0) {
            error = "BCryptFinishHash failed: " + std::to_string(status);
            return false;
        }
        return true;
    }

private:
    BCRYPT_ALG_HANDLE algorithm_ = nullptr;
    BCRYPT_HASH_HANDLE hash_ = nullptr;
    std::vector<std::uint8_t> object_;
};

} // namespace

bool sha256_bytes(const std::string& value,
                  std::vector<std::uint8_t>& digest,
                  std::string& error) {
    HashContext context;
    if (!context.initialize(digest, error)) {
        return false;
    }
    if (!context.update(
            reinterpret_cast<const std::uint8_t*>(value.data()),
            static_cast<ULONG>(value.size()),
            error
    )) {
        return false;
    }
    return context.finish(digest, error);
}

bool sha256_file(const std::wstring& path,
                 std::vector<std::uint8_t>& digest,
                 std::string& error) {
    HANDLE file = CreateFileW(
            path.c_str(),
            GENERIC_READ,
            FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE,
            nullptr,
            OPEN_EXISTING,
            FILE_ATTRIBUTE_NORMAL | FILE_FLAG_SEQUENTIAL_SCAN,
            nullptr
    );
    if (file == INVALID_HANDLE_VALUE) {
        error = "CreateFileW failed for attestation input: "
                + core::format_windows_error(GetLastError());
        return false;
    }

    HashContext context;
    if (!context.initialize(digest, error)) {
        CloseHandle(file);
        return false;
    }

    std::array<std::uint8_t, 64 * 1024> buffer{};
    bool success = true;
    while (success) {
        DWORD read = 0;
        if (ReadFile(
                file,
                buffer.data(),
                static_cast<DWORD>(buffer.size()),
                &read,
                nullptr
        ) == FALSE) {
            error = "ReadFile failed for attestation input: "
                    + core::format_windows_error(GetLastError());
            success = false;
            break;
        }
        if (read == 0) {
            break;
        }
        success = context.update(buffer.data(), read, error);
    }

    if (success) {
        success = context.finish(digest, error);
    }
    CloseHandle(file);
    return success;
}

} // namespace secure_process::attestation
