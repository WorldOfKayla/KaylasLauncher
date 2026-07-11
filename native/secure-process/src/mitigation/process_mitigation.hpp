#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace secure_process::mitigation {

struct Result final {
    std::uint32_t requested;
    std::uint32_t applied;
    std::uint32_t failed;
    std::vector<std::string> failures;
};

Result apply(std::uint32_t requested);
std::uint64_t pack_result(std::uint32_t applied, std::uint32_t failed);

} // namespace secure_process::mitigation
