#pragma once

#include <string>

namespace secure_process::attestation {

std::string create_json(
        const std::string& challenge,
        const std::string& session_binding,
        const std::string& launcher_build_sha256,
        const std::string& launcher_version,
        const std::string& protocol_version
);

} // namespace secure_process::attestation
