package org.takesome.launcher.security;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Read-only audit of modules loaded in the current launcher process. */
public final class SecureProcessAudit {
    private static final Gson GSON = new Gson();

    private SecureProcessAudit() {
    }

    public static SecureProcessAuditReport snapshot() {
        if (!SecureProcess.isNativeLibraryLoaded()) {
            return new SecureProcessAuditReport(
                    ProcessHandle.current().pid(),
                    Instant.now(),
                    List.of(),
                    "SecureProcess native library is not loaded"
            );
        }

        try {
            NativeReport nativeReport = GSON.fromJson(
                    SecureProcessNative.auditLoadedModulesJson(),
                    NativeReport.class
            );
            if (nativeReport == null) {
                throw new IllegalStateException("Native audit returned no report");
            }

            Set<Path> trustedRoots = trustedRoots();
            Set<String> allowedHashes = allowedHashes();
            List<SecureProcessAuditReport.Module> modules = new ArrayList<>();

            if (nativeReport.modules != null) {
                for (NativeModule nativeModule : nativeReport.modules) {
                    modules.add(enrich(nativeModule, trustedRoots, allowedHashes));
                }
            }

            return new SecureProcessAuditReport(
                    nativeReport.processId,
                    Instant.now(),
                    List.copyOf(modules),
                    nativeReport.error == null ? "" : nativeReport.error
            );
        } catch (RuntimeException error) {
            return new SecureProcessAuditReport(
                    ProcessHandle.current().pid(),
                    Instant.now(),
                    List.of(),
                    error.getClass().getSimpleName() + ": " + error.getMessage()
            );
        }
    }

    private static SecureProcessAuditReport.Module enrich(
            NativeModule module,
            Set<Path> trustedRoots,
            Set<String> allowedHashes
    ) {
        Path path;
        try {
            path = Path.of(module.path).toAbsolutePath().normalize();
        } catch (RuntimeException error) {
            return new SecureProcessAuditReport.Module(
                    module.path,
                    module.baseAddress,
                    module.imageSize,
                    module.signatureTrusted,
                    module.signatureStatus,
                    "unavailable",
                    false,
                    false,
                    SecureProcessAuditReport.Finding.AUDIT_ERROR
            );
        }

        boolean trustedLocation = trustedRoots.stream().anyMatch(path::startsWith);
        if (!Files.isRegularFile(path)) {
            return new SecureProcessAuditReport.Module(
                    path.toString(),
                    module.baseAddress,
                    module.imageSize,
                    module.signatureTrusted,
                    module.signatureStatus,
                    "missing",
                    trustedLocation,
                    false,
                    SecureProcessAuditReport.Finding.MISSING_FILE
            );
        }

        String sha256;
        try {
            sha256 = sha256(path);
        } catch (IOException | java.security.NoSuchAlgorithmException error) {
            return new SecureProcessAuditReport.Module(
                    path.toString(),
                    module.baseAddress,
                    module.imageSize,
                    module.signatureTrusted,
                    module.signatureStatus,
                    "unavailable",
                    trustedLocation,
                    false,
                    SecureProcessAuditReport.Finding.AUDIT_ERROR
            );
        }

        boolean hashAllowlisted = allowedHashes.contains(sha256);
        SecureProcessAuditReport.Finding finding;
        if (hashAllowlisted) {
            finding = SecureProcessAuditReport.Finding.HASH_ALLOWLISTED;
        } else if (module.signatureTrusted) {
            finding = SecureProcessAuditReport.Finding.TRUSTED_SIGNED;
        } else if (trustedLocation) {
            finding = SecureProcessAuditReport.Finding.TRUSTED_LOCATION_UNSIGNED;
        } else {
            finding = SecureProcessAuditReport.Finding.UNTRUSTED_LOCATION;
        }

        return new SecureProcessAuditReport.Module(
                path.toString(),
                module.baseAddress,
                module.imageSize,
                module.signatureTrusted,
                module.signatureStatus,
                sha256,
                trustedLocation,
                hashAllowlisted,
                finding
        );
    }

    private static Set<Path> trustedRoots() {
        Set<Path> roots = new HashSet<>();
        addRoot(roots, System.getProperty("java.home", ""));
        addRoot(roots, System.getProperty("user.dir", ""));

        SecureProcessResult secureProcess = SecureProcess.currentResult();
        if (secureProcess != null && secureProcess.nativeLibraryLoaded()) {
            try {
                Path loadedLibrary = Path.of(secureProcess.libraryPath()).toAbsolutePath().normalize();
                Path parent = loadedLibrary.getParent();
                addRoot(roots, parent == null ? "" : parent.toString());
            } catch (RuntimeException ignored) {
                // Keep auditing if a non-path library location was reported.
            }
        }

        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot != null && !systemRoot.isBlank()) {
            addRoot(roots, Path.of(systemRoot, "System32").toString());
        }

        String configured = System.getProperty("kaylas.secureProcess.trustedModuleRoots", "");
        for (String value : configured.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))) {
            addRoot(roots, value);
        }
        return Set.copyOf(roots);
    }

    private static Set<String> allowedHashes() {
        Set<String> hashes = new HashSet<>();
        String configured = System.getProperty("kaylas.secureProcess.allowedModuleSha256", "");
        for (String value : configured.split("[,;\\s]+")) {
            if (!value.isBlank()) {
                hashes.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return Set.copyOf(hashes);
    }

    private static void addRoot(Set<Path> roots, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        roots.add(Path.of(value).toAbsolutePath().normalize());
    }

    private static String sha256(Path path)
            throws IOException, java.security.NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static final class NativeReport {
        private long processId;
        private List<NativeModule> modules;
        private String error;
    }

    private static final class NativeModule {
        private String path;
        private String baseAddress;
        private long imageSize;
        private boolean signatureTrusted;
        private int signatureStatus;
    }
}
