#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace secure_process::attestation {

bool sha256_bytes(
        const std::string& value,
        std::vector<std::uint8_t>& digest,
        std::string& error
);

bool sha256_file(
        const std::wstring& path,
        std::vector<std::uint8_t>& digest,
        std::string& error
);

} // namespace secure_process::attestation
