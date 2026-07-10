package org.takesome.launcher.security;

import java.nio.file.Path;

/** Build-time verification for SecureProcess fail-open startup semantics. */
public final class SecureProcessVerification {
    private SecureProcessVerification() {
    }

    public static void main(String[] args) {
        Path missingLibrary = Path.of("build", "verification", "missing-secure-process.dll")
                .toAbsolutePath()
                .normalize();

        System.setProperty("kaylas.secureProcess.required", "false");
        System.setProperty("kaylas.secureProcess.library", missingLibrary.toString());

        SecureProcessResult result = SecureProcess.initializeEarly();
        require(!result.nativeLibraryLoaded(), "Missing native library was reported as loaded");
        require(result.appliedFlags() == 0, "Mitigations were reported as applied without the DLL");
        require(result.failedFlags() == SecureProcess.BASELINE, "Unexpected failed mitigation mask");
        require(!result.message().isBlank(), "Missing native library produced no diagnostic message");

        System.out.println("SecureProcess fail-open verification passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
