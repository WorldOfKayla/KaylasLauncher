#include <jni.h>
#include <windows.h>

#include "attestation/attestation.hpp"
#include "attestation/hardware_attestation.hpp"
#include "audit/authenticode.hpp"
#include "audit/module_audit.hpp"
#include "core/error_state.hpp"
#include "core/text.hpp"
#include "mitigation/process_mitigation.hpp"
#include "runtime/process_state.hpp"

#include <cstdint>
#include <string>

extern "C" JNIEXPORT jlong JNICALL
Java_org_takesome_launcher_security_SecureProcessNative_initialize(
        JNIEnv*,
        jclass,
        const jint requested_flags
) {
    const auto requested = static_cast<std::uint32_t>(requested_flags);
    const secure_process::mitigation::Result result =
            secure_process::mitigation::apply(requested);

    secure_process::runtime::set_mitigation_state(result.applied, result.failed);
    secure_process::core::set_last_error_message(result.failures);
    return static_cast<jlong>(secure_process::mitigation::pack_result(
            result.applied,
            result.failed
    ));
}

extern "C" JNIEXPORT jint JNICALL
Java_org_takesome_launcher_security_SecureProcessNative_verifyAuthenticode(
        JNIEnv* environment,
        jclass,
        jstring path
) {
    return static_cast<jint>(secure_process::audit::verify_authenticode(
            secure_process::core::wide_from_java(environment, path)
    ));
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_takesome_launcher_security_SecureProcessNative_auditLoadedModulesJson(
        JNIEnv* environment,
        jclass
) {
    const std::string json = secure_process::audit::loaded_modules_json();
    return environment->NewStringUTF(json.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_takesome_launcher_security_SecureProcessNative_createAttestation(
        JNIEnv* environment,
        jclass,
        jstring challenge,
        jstring session_binding,
        jstring launcher_build_sha256,
        jstring launcher_version,
        jstring protocol_version
) {
    const std::string json = secure_process::attestation::create_json(
            secure_process::core::utf8_from_wide(
                    secure_process::core::wide_from_java(environment, challenge)
            ),
            secure_process::core::utf8_from_wide(
                    secure_process::core::wide_from_java(environment, session_binding)
            ),
            secure_process::core::utf8_from_wide(
                    secure_process::core::wide_from_java(environment, launcher_build_sha256)
            ),
            secure_process::core::utf8_from_wide(
                    secure_process::core::wide_from_java(environment, launcher_version)
            ),
            secure_process::core::utf8_from_wide(
                    secure_process::core::wide_from_java(environment, protocol_version)
            )
    );
    return environment->NewStringUTF(json.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_takesome_launcher_security_SecureProcessNative_createHardwareAttestation(
        JNIEnv* environment,
        jclass,
        jstring challenge,
        jstring session_binding,
        jstring launcher_build_sha256,
        jstring launcher_version,
        jstring protocol_version
) {
    const std::string json = secure_process::attestation::create_hardware_json(
            secure_process::core::utf8_from_wide(
                    secure_process::core::wide_from_java(environment, challenge)
            ),
            secure_process::core::utf8_from_wide(
                    secure_process::core::wide_from_java(environment, session_binding)
            ),
            secure_process::core::utf8_from_wide(
                    secure_process::core::wide_from_java(environment, launcher_build_sha256)
            ),
            secure_process::core::utf8_from_wide(
                    secure_process::core::wide_from_java(environment, launcher_version)
            ),
            secure_process::core::utf8_from_wide(
                    secure_process::core::wide_from_java(environment, protocol_version)
            )
    );
    return environment->NewStringUTF(json.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_takesome_launcher_security_SecureProcessNative_lastError(
        JNIEnv* environment,
        jclass
) {
    const std::string error = secure_process::core::last_error_message();
    return environment->NewStringUTF(error.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_takesome_launcher_security_SecureProcessNative_version(
        JNIEnv* environment,
        jclass
) {
    return environment->NewStringUTF(SECURE_PROCESS_VERSION);
}

BOOL APIENTRY DllMain(HMODULE module, DWORD reason, LPVOID) {
    if (reason == DLL_PROCESS_ATTACH) {
        secure_process::runtime::set_module(module);
        DisableThreadLibraryCalls(module);
    }
    return TRUE;
}
