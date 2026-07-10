package org.takesome.launcher.security;

import java.time.Instant;
import java.util.List;

/** Snapshot of native modules loaded in the current launcher process. */
public record SecureProcessAuditReport(
        long processId,
        Instant capturedAt,
        List<Module> modules,
        String error
) {
    public record Module(
            String path,
            String baseAddress,
            long imageSize,
            boolean signatureTrusted,
            int signatureStatus,
            String sha256,
            boolean trustedLocation,
            boolean hashAllowlisted,
            Finding finding
    ) {
    }

    public enum Finding {
        TRUSTED_SIGNED,
        TRUSTED_LOCATION_UNSIGNED,
        HASH_ALLOWLISTED,
        UNTRUSTED_LOCATION,
        MISSING_FILE,
        AUDIT_ERROR
    }

    public boolean hasSuspiciousModules() {
        return modules != null && modules.stream().anyMatch(module ->
                module.finding() == Finding.UNTRUSTED_LOCATION
                        || module.finding() == Finding.MISSING_FILE
                        || module.finding() == Finding.AUDIT_ERROR
        );
    }
}
