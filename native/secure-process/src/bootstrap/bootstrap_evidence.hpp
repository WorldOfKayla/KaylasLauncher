#pragma once

#include <windows.h>

#include <cstdint>
#include <string>

namespace secure_process::bootstrap {

struct Evidence final {
    bool present = false;
    bool parent_verified = false;
    bool authenticode_trusted = false;
    std::uint32_t process_id = 0;
    LONG signature_status = TRUST_E_NOSIGNATURE;
    std::string path;
    std::string sha256;
    std::string nonce;
};

Evidence measure(std::string& error);

} // namespace secure_process::bootstrap
