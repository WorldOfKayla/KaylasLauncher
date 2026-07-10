package org.takesome.launcher.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

/** Pre-load integrity verification for the SecureProcess native library. */
final class SecureProcessIntegrity {
    private SecureProcessIntegrity() {
    }

    static Verification verify(Path libraryPath) throws IOException, GeneralSecurityException {
        byte[] bytes = Files.readAllBytes(libraryPath);
        String actualSha256 = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes)
        );

        String expectedSha256 = System.getProperty("kaylas.secureProcess.sha256", "")
                .trim()
                .toLowerCase();
        boolean hashConfigured = !expectedSha256.isEmpty();
        boolean hashVerified = !hashConfigured
                || MessageDigest.isEqual(
                        actualSha256.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                        expectedSha256.getBytes(java.nio.charset.StandardCharsets.US_ASCII)
                );

        String signatureBase64 = System.getProperty("kaylas.secureProcess.signature", "").trim();
        String publicKeyBase64 = System.getProperty("kaylas.secureProcess.publicKey", "").trim();
        boolean signatureConfigured = !signatureBase64.isEmpty() || !publicKeyBase64.isEmpty();
        boolean signatureVerified = !signatureConfigured;

        if (signatureConfigured) {
            if (signatureBase64.isEmpty() || publicKeyBase64.isEmpty()) {
                throw new GeneralSecurityException(
                        "Both kaylas.secureProcess.signature and kaylas.secureProcess.publicKey are required"
                );
            }
            PublicKey publicKey = KeyFactory.getInstance("Ed25519").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64))
            );
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(bytes);
            signatureVerified = verifier.verify(Base64.getDecoder().decode(signatureBase64));
        }

        return new Verification(
                actualSha256,
                hashConfigured,
                hashVerified,
                signatureConfigured,
                signatureVerified
        );
    }

    record Verification(
            String actualSha256,
            boolean hashConfigured,
            boolean hashVerified,
            boolean signatureConfigured,
            boolean signatureVerified
    ) {
        boolean accepted() {
            return hashVerified && signatureVerified;
        }
    }
}
