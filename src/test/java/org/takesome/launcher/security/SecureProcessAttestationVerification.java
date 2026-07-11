package org.takesome.launcher.security;

import java.util.Map;

/** Executable verification for native SecureProcess challenge signing. */
public final class SecureProcessAttestationVerification {
    private SecureProcessAttestationVerification() {
    }

    public static void main(String[] args) {
        SecureProcessResult result = SecureProcess.initializeEarly();
        require(result.nativeLibraryLoaded(), "SecureProcess native library did not load");
        require(result.fullyApplied(), "SecureProcess mitigation baseline is incomplete");
        require("0.3.0".equals(result.nativeVersion()),
                "Unexpected SecureProcess native version: " + result.nativeVersion());

        Map<String, Object> evidence = SecureProcessAttestation.create(
                "verification-challenge",
                "verification-session",
                "2.12.0",
                "0.1.0"
        );
        require("SP1".equals(evidence.get("version")), "Unexpected attestation protocol");
        require("verification-challenge".equals(evidence.get("challenge")),
                "Challenge was not bound into evidence");
        require("verification-session".equals(evidence.get("session")),
                "Session was not bound into evidence");
        require(!String.valueOf(evidence.get("keyId")).isBlank(), "Attestation keyId is missing");
        require(!String.valueOf(evidence.get("publicKey")).isBlank(), "Attestation public key is missing");
        require(!String.valueOf(evidence.get("signature")).isBlank(), "Attestation signature is missing");
        require(result.sha256().equals(evidence.get("secureProcessSha256")),
                "Signed SecureProcess hash differs from verified DLL hash");

        System.out.println("SecureProcess native attestation verification passed.");
        System.out.println("SecureProcess attestation keyId=" + evidence.get("keyId"));
        System.out.println("Launcher build sha256=" + evidence.get("launcherBuildSha256"));
        System.out.println("SecureProcess sha256=" + evidence.get("secureProcessSha256"));
        System.out.println("Process sha256=" + evidence.get("processSha256"));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
