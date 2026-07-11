package org.takesome.launcher.security;

import java.util.Map;

/** Executable verification for native SecureProcess SP1 challenge signing. */
public final class SecureProcessAttestationVerification {
    private SecureProcessAttestationVerification() {
    }

    public static void main(String[] args) {
        SecureProcessResult result = SecureProcess.initializeEarly();
        require(result.nativeLibraryLoaded(), "SecureProcess native library did not load");
        require(result.fullyApplied(), "SecureProcess mitigation baseline is incomplete");
        require("0.4.0".equals(result.nativeVersion()),
                "Unexpected SecureProcess native version: " + result.nativeVersion());

        Map<String, Object> softwareEvidence = SecureProcessAttestation.create(
                "verification-challenge",
                "verification-session",
                "2.12.0",
                "0.1.0",
                SecureProcessAttestation.SOFTWARE_PROTOCOL
        );
        require("SP1".equals(softwareEvidence.get("version")),
                "Unexpected software attestation protocol");
        require("verification-challenge".equals(softwareEvidence.get("challenge")),
                "Challenge was not bound into evidence");
        require("verification-session".equals(softwareEvidence.get("session")),
                "Session was not bound into evidence");
        require(!String.valueOf(softwareEvidence.get("keyId")).isBlank(),
                "Attestation keyId is missing");
        require(!String.valueOf(softwareEvidence.get("publicKey")).isBlank(),
                "Attestation public key is missing");
        require(!String.valueOf(softwareEvidence.get("signature")).isBlank(),
                "Attestation signature is missing");
        require(result.sha256().equals(softwareEvidence.get("secureProcessSha256")),
                "Signed SecureProcess hash differs from verified DLL hash");

        System.out.println("SecureProcess native attestation verification passed.");
        System.out.println("SecureProcess attestation keyId="
                + softwareEvidence.get("keyId"));
        System.out.println("Launcher build sha256="
                + softwareEvidence.get("launcherBuildSha256"));
        System.out.println("SecureProcess sha256="
                + softwareEvidence.get("secureProcessSha256"));
        System.out.println("Process sha256="
                + softwareEvidence.get("processSha256"));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
