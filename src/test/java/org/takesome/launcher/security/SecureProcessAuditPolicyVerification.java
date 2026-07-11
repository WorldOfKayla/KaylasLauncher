package org.takesome.launcher.security;

import java.nio.file.Path;
import java.util.Set;

/** Verifies protected Windows module roots without loading the native audit library. */
public final class SecureProcessAuditPolicyVerification {
    private SecureProcessAuditPolicyVerification() {
    }

    public static void main(String[] args) {
        Path windows = Path.of("build", "verification", "Windows")
                .toAbsolutePath()
                .normalize();
        Set<Path> roots = SecureProcessTrustedLocations.windowsSystemRoots(windows);

        require(roots.contains(windows.resolve("System32")),
                "System32 was not registered as a trusted system root");
        require(roots.contains(windows.resolve("SysWOW64")),
                "SysWOW64 was not registered as a trusted system root");
        require(roots.contains(windows.resolve("WinSxS")),
                "WinSxS was not registered as a trusted system root");

        Path gdiplus = windows
                .resolve("WinSxS")
                .resolve("amd64_microsoft.windows.gdiplus_example")
                .resolve("gdiplus.dll");
        require(SecureProcessTrustedLocations.contains(roots, gdiplus),
                "WinSxS system module was classified as untrusted");
        require(SecureProcessTrustedLocations.contains(
                        roots,
                        windows.resolve("System32").resolve("d3d11.dll")
                ),
                "System32 module was classified as untrusted");
        require(!SecureProcessTrustedLocations.contains(
                        roots,
                        windows.resolve("Temp").resolve("injected.dll")
                ),
                "Windows Temp was incorrectly classified as a trusted module root");
        require(!SecureProcessTrustedLocations.contains(
                        roots,
                        windows.getParent().resolve("Downloads").resolve("injected.dll")
                ),
                "user-controlled module path was incorrectly trusted");

        System.out.println("SecureProcess module trust policy verification passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
