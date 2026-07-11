package org.takesome.launcher.security;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
    private static final String SHA256_PROPERTY = "kaylas.secureProcess.sha256";
    private static final String BUNDLED_RESOURCE = "/native/windows-x86_64/secure_process.dll";
    private static final String BUNDLED_SHA256 = "43408aacee7b573d044319fb91f5d7cd8b10d4c325168b70d3521f6cdc690b22";
    private static final AtomicReference<SecureProcessResult> RESULT = new AtomicReference<>();

    private SecureProcess() {
    }

    /** Initializes the native library and mitigation profile exactly once. */
    public static SecureProcessResult initializeEarly() {
        SecureProcessResult existing = RESULT.get();
        if (existing != null) {
            SecureProcessLog.logger().debug(
                    "SecureProcess initialization reused: loaded={}, applied=0x{}, failed=0x{}",
                    existing.nativeLibraryLoaded(),
                    Integer.toHexString(existing.appliedFlags()),
                    Integer.toHexString(existing.failedFlags()));
            return existing;
        }

        int requestedFlags = parseRequestedFlags();
        SecureProcessLog.logger().info("SecureProcess initialization started: requestedFlags=0x{}, strict={}, integrityRequired={}",
                Integer.toHexString(requestedFlags), required(), integrityRequired());
        SecureProcessResult created = initialize(requestedFlags);
        if (!RESULT.compareAndSet(null, created)) {
            return RESULT.get();
        }

        if (created.nativeLibraryLoaded()) {
            SecureProcessLog.logger().info(
                    "SecureProcess initialization completed: version={}, applied=0x{}, failed=0x{}, hashVerified={}, signatureVerified={}, authenticodeTrusted={}",
                    created.nativeVersion(),
                    Integer.toHexString(created.appliedFlags()),
                    Integer.toHexString(created.failedFlags()),
                    created.hashVerified(),
                    created.detachedSignatureVerified(),
                    created.authenticodeTrusted());
        } else {
            SecureProcessLog.logger().error("SecureProcess initialization failed: {}", created.message());
        }

        if (required() && !created.fullyApplied()) {
            SecureProcessLog.logger().fatal("Strict SecureProcess policy rejected startup: {}", created.message());
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
                SecureProcessLog.logger().debug("Native library resolved: '{}'", libraryPath);
                verification = SecureProcessIntegrity.verify(libraryPath);
                SecureProcessLog.logger().debug(
                        "Pre-load integrity verified: sha256={}, hashConfigured={}, hashVerified={}, signatureConfigured={}, signatureVerified={}",
                        verification.actualSha256(),
                        verification.hashConfigured(),
                        verification.hashVerified(),
                        verification.signatureConfigured(),
                        verification.signatureVerified());
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
                SecureProcessLog.logger().debug("Loading native library from '{}'", libraryPath);
                System.load(libraryPath.toString());
            } else {
                if (integrityRequired()) {
                    throw new SecurityException(
                            "SecureProcess integrity requires an explicit or resolvable DLL path"
                    );
                }
                SecureProcessLog.logger().warn("Native library path unresolved; loading through java.library.path");
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
            SecureProcessLog.logger().debug(
                    "Native mitigation result: applied=0x{}, failed=0x{}, authenticodeStatus={}, authenticodeTrusted={}",
                    Integer.toHexString(appliedFlags),
                    Integer.toHexString(failedFlags),
                    authenticodeStatus,
                    authenticodeTrusted);
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
            SecureProcessLog.logger().error("SecureProcess initialization exception", error);
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

    private static Path resolveNativeLibrary() throws IOException {
        String explicitPath = System.getProperty(LIBRARY_PROPERTY, "").trim();
        if (!explicitPath.isEmpty()) {
            Path path = Path.of(explicitPath).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path)) {
                throw new UnsatisfiedLinkError("SecureProcess DLL does not exist: " + path);
            }
            return path;
        }

        Path bundled = extractBundledNativeLibrary();
        if (bundled != null) {
            System.setProperty(SHA256_PROPERTY, BUNDLED_SHA256);
            return bundled;
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

    private static Path extractBundledNativeLibrary() throws IOException {
        try (InputStream input = SecureProcess.class.getResourceAsStream(BUNDLED_RESOURCE)) {
            if (input == null) {
                SecureProcessLog.logger().debug("Bundled SecureProcess resource '{}' is unavailable", BUNDLED_RESOURCE);
                return null;
            }

            Path directory = Path.of(
                    System.getProperty("java.io.tmpdir"),
                    "kaylas-launcher",
                    "secure-process",
                    BUNDLED_SHA256
            ).toAbsolutePath().normalize();
            Files.createDirectories(directory);

            Path target = directory.resolve("secure_process.dll");
            if (Files.isRegularFile(target)) {
                SecureProcessLog.logger().debug("Reusing extracted SecureProcess DLL '{}'", target);
                return target;
            }

            Path temporary = Files.createTempFile(directory, "secure_process-", ".tmp");
            try {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
                try {
                    Files.move(
                            temporary,
                            target,
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            target.toFile().deleteOnExit();
            SecureProcessLog.logger().info("Bundled SecureProcess DLL extracted to '{}'", target);
            return target;
        }
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
