package org.takesome.launcher.security;

/** JNI entry points implemented by the SecureProcess native library. */
final class SecureProcessNative {
    private SecureProcessNative() {
    }

    static native long initialize(int requestedFlags);

    static native int verifyAuthenticode(String absolutePath);

    static native String auditLoadedModulesJson();

    static native String createAttestation(
            String challenge,
            String sessionBinding,
            String launcherBuildSha256,
            String launcherVersion,
            String protocolVersion
    );

    static native String createHardwareAttestation(
            String challenge,
            String sessionBinding,
            String launcherBuildSha256,
            String launcherVersion,
            String protocolVersion
    );

    static native String lastError();

    static native String version();
}
