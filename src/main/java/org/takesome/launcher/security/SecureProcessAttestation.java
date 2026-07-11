package org.takesome.launcher.security;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Creates signed SecureProcess evidence for a backend challenge. */
public final class SecureProcessAttestation {
    public static final String SOFTWARE_PROTOCOL = "SP1";
    public static final String HARDWARE_PROTOCOL = "SP2";

    private static final String PROFILE_PROPERTY =
            "kaylas.secureProcess.attestationProfile";
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE =
            new TypeToken<LinkedHashMap<String, Object>>() { }.getType();

    private SecureProcessAttestation() {
    }

    public static Map<String, Object> create(String challenge,
                                             String sessionBinding,
                                             String launcherVersion,
                                             String backendProtocolVersion) {
        return create(
                challenge,
                sessionBinding,
                launcherVersion,
                backendProtocolVersion,
                System.getProperty(PROFILE_PROPERTY, SOFTWARE_PROTOCOL)
        );
    }

    public static Map<String, Object> create(String challenge,
                                             String sessionBinding,
                                             String launcherVersion,
                                             String backendProtocolVersion,
                                             String attestationProtocol) {
        Objects.requireNonNull(challenge, "challenge");
        Objects.requireNonNull(sessionBinding, "sessionBinding");

        SecureProcessResult result = SecureProcess.currentResult();
        if (result == null || !result.nativeLibraryLoaded()) {
            throw new IllegalStateException("SecureProcess native library is not active");
        }
        if (!result.fullyApplied()) {
            throw new IllegalStateException(
                    "SecureProcess baseline is incomplete: failedFlags=0x"
                            + Integer.toHexString(result.failedFlags())
            );
        }

        String profile = normalizeProfile(attestationProtocol);
        String buildSha256 = LauncherBuildIdentity.sha256();
        String normalizedLauncherVersion = launcherVersion == null
                ? "unknown"
                : launcherVersion;
        String normalizedBackendProtocol = backendProtocolVersion == null
                ? "unknown"
                : backendProtocolVersion;

        String json = HARDWARE_PROTOCOL.equals(profile)
                ? SecureProcessNative.createHardwareAttestation(
                        challenge,
                        sessionBinding,
                        buildSha256,
                        normalizedLauncherVersion,
                        normalizedBackendProtocol
                )
                : SecureProcessNative.createAttestation(
                        challenge,
                        sessionBinding,
                        buildSha256,
                        normalizedLauncherVersion,
                        normalizedBackendProtocol
                );
        if (json == null || json.isBlank()) {
            throw new IllegalStateException(
                    "SecureProcess returned empty " + profile + " attestation evidence"
            );
        }

        Map<String, Object> evidence = GSON.fromJson(json, MAP_TYPE);
        if (evidence == null) {
            throw new IllegalStateException(
                    "SecureProcess returned invalid " + profile + " attestation evidence"
            );
        }
        Object nativeError = evidence.get("error");
        if (nativeError != null && !String.valueOf(nativeError).isBlank()) {
            throw new IllegalStateException(
                    "SecureProcess " + profile + " attestation failed: " + nativeError
            );
        }
        if (!profile.equals(String.valueOf(evidence.get("version")))) {
            throw new IllegalStateException(
                    "SecureProcess returned protocol " + evidence.get("version")
                            + " while " + profile + " was required"
            );
        }
        return Map.copyOf(evidence);
    }

    private static String normalizeProfile(String value) {
        String profile = value == null
                ? SOFTWARE_PROTOCOL
                : value.trim().toUpperCase(Locale.ROOT);
        if (profile.isEmpty()) {
            return SOFTWARE_PROTOCOL;
        }
        if (!SOFTWARE_PROTOCOL.equals(profile) && !HARDWARE_PROTOCOL.equals(profile)) {
            throw new IllegalArgumentException(
                    "Unsupported SecureProcess attestation profile: " + value
            );
        }
        return profile;
    }
}
