package org.takesome.launcher.security;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/** Early-process hardening and native-library integrity facade for KaylasLauncher. */
public final class SecureProcess {
    public static final int SAFE_DLL_SEARCH = 1 << 0;
    public static final int DEP = 1 << 1;
    public static final int ASLR = 1 << 2;
    public static final int DISABLE_EXTENSION_POINTS = 1 << 3;
    public static final int BLOCK_REMOTE_IMAGES = 1 << 4;
    public static final int BLOCK_LOW_INTEGRITY_IMAGES = 1 << 5;
    public static final int STRICT_HANDLE_CHECKS = 1 << 6;
    public static final int PREFER_SYSTEM32_IMAGES = 1 << 7;

    public static final int BASELINE = SAFE_DLL_SEARCH
            | DEP
            | ASLR
            | DISABLE_EXTENSION_POINTS
            | BLOCK_REMOTE_IMAGES
            | BLOCK_LOW_INTEGRITY_IMAGES
            | STRICT_HANDLE_CHECKS
            | PREFER_SYSTEM32_IMAGES;

    private static final String REQUIRED_PROPERTY = "kaylas.secureProcess.required";
    private static final String INTEGRITY_REQUIRED_PROPERTY = "kaylas.secureProcess.integrityRequired";
    private static final String LIBRARY_PROPERTY = "kaylas.secureProcess.library";
    private static final String PROFILE_PROPERTY = "kaylas.secureProcess.flags";
    private static final AtomicReference<SecureProcessResult> RESULT = new AtomicReference<>();

    private SecureProcess() {
    }

    /** Initializes the native library and mitigation profile exactly once. */
    public static SecureProcessResult initializeEarly() {
        SecureProcessResult existing = RESULT.get();
        if (existing != null) {
            return existing;
        }

        SecureProcessResult created = initialize(parseRequestedFlags());
        if (!RESULT.compareAndSet(null, created)) {
            return RESULT.get();
        }

        if (required() && !created.fullyApplied()) {
            throw new IllegalStateException("SecureProcess is required but failed: " + created.message());
        }
        return created;
    }

    public static SecureProcessResult currentResult() {
        return RESULT.get();
    }

    public static boolean isNativeLibraryLoaded() {
        SecureProcessResult result = RESULT.get();
        return result != null && result.nativeLibraryLoaded();
    }

    private static SecureProcessResult initialize(int requestedFlags) {
        if (!isWindows()) {
            return failure(requestedFlags, "SecureProcess currently supports Windows only");
        }

        try {
            Path libraryPath = resolveNativeLibrary();
            SecureProcessIntegrity.Verification verification = null;

            if (libraryPath != null) {
                verification = SecureProcessIntegrity.verify(libraryPath);
                if (!verification.accepted()) {
                    throw new SecurityException(
                            "SecureProcess DLL integrity verification failed: sha256="
                                    + verification.actualSha256()
                    );
                }
                if (integrityRequired()
                        && !verification.hashConfigured()
                        && !verification.signatureConfigured()) {
                    throw new SecurityException(
                            "SecureProcess integrity is required but no SHA-256 or detached signature is configured"
                    );
                }
                System.load(libraryPath.toString());
            } else {
                if (integrityRequired()) {
                    throw new SecurityException(
                            "SecureProcess integrity requires an explicit or resolvable DLL path"
                    );
                }
                System.loadLibrary("secure_process");
            }

            long packedResult = SecureProcessNative.initialize(requestedFlags);
            int appliedFlags = (int) packedResult;
            int failedFlags = (int) (packedResult >>> 32);
            String nativeVersion = safeNativeString(SecureProcessNative.version(), "unknown");
            String nativeError = safeNativeString(SecureProcessNative.lastError(), "");

            int authenticodeStatus = libraryPath == null
                    ? -1
                    : SecureProcessNative.verifyAuthenticode(libraryPath.toString());
            boolean authenticodeTrusted = authenticodeStatus == 0;
            boolean requireAuthenticode = Boolean.parseBoolean(
                    System.getProperty("kaylas.secureProcess.authenticodeRequired", "false")
            );
            if (requireAuthenticode && !authenticodeTrusted) {
                throw new SecurityException(
                        "SecureProcess Authenticode verification failed with status " + authenticodeStatus
                );
            }

            String message = failedFlags == 0
                    ? "SecureProcess baseline applied"
                    : nativeError.isBlank()
                    ? "One or more SecureProcess mitigations failed"
                    : nativeError;

            return new SecureProcessResult(
                    true,
                    requestedFlags,
                    appliedFlags,
                    failedFlags,
                    nativeVersion,
                    libraryPath == null ? "java.library.path" : libraryPath.toString(),
                    verification == null ? "unverified" : verification.actualSha256(),
                    verification != null && verification.hashConfigured(),
                    verification == null || verification.hashVerified(),
                    verification != null && verification.signatureConfigured(),
                    verification == null || verification.signatureVerified(),
                    authenticodeTrusted,
                    authenticodeStatus,
                    message
            );
        } catch (Exception | UnsatisfiedLinkError error) {
            return failure(
                    requestedFlags,
                    error.getClass().getSimpleName() + ": " + safeMessage(error)
            );
        }
    }

    private static SecureProcessResult failure(int requestedFlags, String message) {
        return new SecureProcessResult(
                false,
                requestedFlags,
                0,
                requestedFlags,
                "unavailable",
                "unavailable",
                "unavailable",
                false,
                false,
                false,
                false,
                false,
                -1,
                message
        );
    }

    private static Path resolveNativeLibrary() {
        String explicitPath = System.getProperty(LIBRARY_PROPERTY, "").trim();
        if (!explicitPath.isEmpty()) {
            Path path = Path.of(explicitPath).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path)) {
                throw new UnsatisfiedLinkError("SecureProcess DLL does not exist: " + path);
            }
            return path;
        }

        String mappedName = System.mapLibraryName("secure_process");
        String libraryPath = System.getProperty("java.library.path", "");
        for (String directory : libraryPath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (directory.isBlank()) {
                continue;
            }
            Path candidate = Path.of(directory, mappedName).toAbsolutePath().normalize();
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static int parseRequestedFlags() {
        String configured = System.getProperty(PROFILE_PROPERTY, "").trim();
        if (configured.isEmpty()) {
            return BASELINE;
        }
        try {
            return Integer.decode(configured);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    "Invalid " + PROFILE_PROPERTY + " value: " + configured,
                    error
            );
        }
    }

    private static boolean required() {
        return Boolean.parseBoolean(System.getProperty(REQUIRED_PROPERTY, "false"));
    }

    private static boolean integrityRequired() {
        return Boolean.parseBoolean(
                System.getProperty(INTEGRITY_REQUIRED_PROPERTY, Boolean.toString(required()))
        );
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("windows");
    }

    private static String safeNativeString(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private static String safeMessage(Throwable error) {
        return error.getMessage() == null || error.getMessage().isBlank()
                ? error.getClass().getName()
                : error.getMessage();
    }
}
