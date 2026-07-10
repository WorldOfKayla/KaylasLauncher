package org.takesome.launcher.security;

/** Immutable result of SecureProcess initialization. */
public record SecureProcessResult(
        boolean nativeLibraryLoaded,
        int requestedFlags,
        int appliedFlags,
        int failedFlags,
        String nativeVersion,
        String libraryPath,
        String sha256,
        boolean hashConfigured,
        boolean hashVerified,
        boolean detachedSignatureConfigured,
        boolean detachedSignatureVerified,
        boolean authenticodeTrusted,
        int authenticodeStatus,
        String message
) {
    public boolean fullyApplied() {
        return nativeLibraryLoaded
                && failedFlags == 0
                && appliedFlags == requestedFlags
                && (!hashConfigured || hashVerified)
                && (!detachedSignatureConfigured || detachedSignatureVerified);
    }

    public boolean partiallyApplied() {
        return nativeLibraryLoaded && appliedFlags != 0 && failedFlags != 0;
    }
}
