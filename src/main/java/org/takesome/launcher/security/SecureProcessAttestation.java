package org.takesome.launcher.security;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Creates signed SecureProcess evidence for a backend challenge. */
public final class SecureProcessAttestation {
    public static final String PROTOCOL_VERSION = "SP1";

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, Object>>() { }.getType();

    private SecureProcessAttestation() {
    }

    public static Map<String, Object> create(String challenge,
                                             String sessionBinding,
                                             String launcherVersion,
                                             String backendProtocolVersion) {
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

        String json = SecureProcessNative.createAttestation(
                challenge,
                sessionBinding,
                LauncherBuildIdentity.sha256(),
                launcherVersion == null ? "unknown" : launcherVersion,
                backendProtocolVersion == null ? "unknown" : backendProtocolVersion
        );
        if (json == null || json.isBlank()) {
            throw new IllegalStateException("SecureProcess returned empty attestation evidence");
        }

        Map<String, Object> evidence = GSON.fromJson(json, MAP_TYPE);
        if (evidence == null) {
            throw new IllegalStateException("SecureProcess returned invalid attestation evidence");
        }
        Object nativeError = evidence.get("error");
        if (nativeError != null && !String.valueOf(nativeError).isBlank()) {
            throw new IllegalStateException("SecureProcess attestation failed: " + nativeError);
        }
        if (!PROTOCOL_VERSION.equals(String.valueOf(evidence.get("version")))) {
            throw new IllegalStateException("Unsupported SecureProcess attestation protocol");
        }
        return Map.copyOf(evidence);
    }
}
