#pragma once

#include <jni.h>

#include <cstdint>
#include <string>
#include <vector>

namespace secure_process::core {

std::string utf8_from_wide(const std::wstring& input);
std::wstring wide_from_java(JNIEnv* environment, jstring value);
std::string json_escape(const std::string& input);
std::string hex_encode(const std::vector<std::uint8_t>& bytes);
std::string base64_encode(const std::vector<std::uint8_t>& bytes);

} // namespace secure_process::core
