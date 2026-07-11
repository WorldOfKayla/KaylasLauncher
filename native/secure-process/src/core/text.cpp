#include "core/text.hpp"

#include <windows.h>
#include <wincrypt.h>

#include <iomanip>
#include <sstream>

namespace secure_process::core {

std::string utf8_from_wide(const std::wstring& input) {
    if (input.empty()) {
        return {};
    }
    const int required = WideCharToMultiByte(
            CP_UTF8,
            WC_ERR_INVALID_CHARS,
            input.data(),
            static_cast<int>(input.size()),
            nullptr,
            0,
            nullptr,
            nullptr
    );
    if (required <= 0) {
        return {};
    }
    std::string output(static_cast<std::size_t>(required), '\0');
    WideCharToMultiByte(
            CP_UTF8,
            WC_ERR_INVALID_CHARS,
            input.data(),
            static_cast<int>(input.size()),
            output.data(),
            required,
            nullptr,
            nullptr
    );
    return output;
}

std::wstring wide_from_java(JNIEnv* environment, jstring value) {
    if (value == nullptr) {
        return {};
    }
    const jchar* chars = environment->GetStringChars(value, nullptr);
    if (chars == nullptr) {
        return {};
    }
    const jsize length = environment->GetStringLength(value);
    std::wstring output(
            reinterpret_cast<const wchar_t*>(chars),
            reinterpret_cast<const wchar_t*>(chars) + length
    );
    environment->ReleaseStringChars(value, chars);
    return output;
}

std::string json_escape(const std::string& input) {
    std::ostringstream output;
    for (const unsigned char character : input) {
        switch (character) {
            case '"': output << "\\\""; break;
            case '\\': output << "\\\\"; break;
            case '\b': output << "\\b"; break;
            case '\f': output << "\\f"; break;
            case '\n': output << "\\n"; break;
            case '\r': output << "\\r"; break;
            case '\t': output << "\\t"; break;
            default:
                if (character < 0x20) {
                    output << "\\u"
                           << std::hex << std::setw(4) << std::setfill('0')
                           << static_cast<int>(character)
                           << std::dec;
                } else {
                    output << static_cast<char>(character);
                }
        }
    }
    return output.str();
}

std::string hex_encode(const std::vector<std::uint8_t>& bytes) {
    std::ostringstream output;
    output << std::hex << std::setfill('0');
    for (const std::uint8_t value : bytes) {
        output << std::setw(2) << static_cast<unsigned int>(value);
    }
    return output.str();
}

std::string base64_encode(const std::vector<std::uint8_t>& bytes) {
    if (bytes.empty()) {
        return {};
    }
    DWORD required = 0;
    if (CryptBinaryToStringA(
            bytes.data(),
            static_cast<DWORD>(bytes.size()),
            CRYPT_STRING_BASE64 | CRYPT_STRING_NOCRLF,
            nullptr,
            &required
    ) == FALSE || required == 0) {
        return {};
    }
    std::string output(static_cast<std::size_t>(required), '\0');
    if (CryptBinaryToStringA(
            bytes.data(),
            static_cast<DWORD>(bytes.size()),
            CRYPT_STRING_BASE64 | CRYPT_STRING_NOCRLF,
            output.data(),
            &required
    ) == FALSE) {
        return {};
    }
    while (!output.empty() && output.back() == '\0') {
        output.pop_back();
    }
    return output;
}

} // namespace secure_process::core
