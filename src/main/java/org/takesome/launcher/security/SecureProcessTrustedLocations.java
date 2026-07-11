package org.takesome.launcher.security;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/** Resolves protected module roots accepted by the SecureProcess audit policy. */
final class SecureProcessTrustedLocations {
    private SecureProcessTrustedLocations() {
    }

    static Set<Path> resolve(SecureProcessResult secureProcess) {
        Set<Path> roots = new HashSet<>();
        addRoot(roots, System.getProperty("java.home", ""));
        addRoot(roots, System.getProperty("user.dir", ""));
        addSecureProcessRoot(roots, secureProcess);
        addWindowsSystemRoots(roots, System.getenv("SystemRoot"));
        addConfiguredRoots(roots, System.getProperty(
                "kaylas.secureProcess.trustedModuleRoots",
                ""
        ));
        return Set.copyOf(roots);
    }

    static Set<Path> windowsSystemRoots(Path systemRoot) {
        Set<Path> roots = new HashSet<>();
        if (systemRoot == null) {
            return Set.of();
        }
        addRoot(roots, systemRoot.resolve("System32"));
        addRoot(roots, systemRoot.resolve("SysWOW64"));
        addRoot(roots, systemRoot.resolve("WinSxS"));
        return Set.copyOf(roots);
    }

    static boolean contains(Set<Path> roots, Path modulePath) {
        if (roots == null || roots.isEmpty() || modulePath == null) {
            return false;
        }
        Path normalized = modulePath.toAbsolutePath().normalize();
        return roots.stream().anyMatch(normalized::startsWith);
    }

    private static void addSecureProcessRoot(Set<Path> roots, SecureProcessResult secureProcess) {
        if (secureProcess == null || !secureProcess.nativeLibraryLoaded()) {
            return;
        }
        try {
            Path loadedLibrary = Path.of(secureProcess.libraryPath())
                    .toAbsolutePath()
                    .normalize();
            Path parent = loadedLibrary.getParent();
            if (parent != null) {
                addRoot(roots, parent);
            }
        } catch (RuntimeException ignored) {
            // Continue auditing if the native layer reports a non-path location.
        }
    }

    private static void addWindowsSystemRoots(Set<Path> roots, String systemRoot) {
        if (systemRoot == null || systemRoot.isBlank()) {
            return;
        }
        try {
            roots.addAll(windowsSystemRoots(Path.of(systemRoot)));
        } catch (RuntimeException ignored) {
            // Invalid environment data must not disable the remaining audit roots.
        }
    }

    private static void addConfiguredRoots(Set<Path> roots, String configured) {
        if (configured == null || configured.isBlank()) {
            return;
        }
        for (String value : configured.split(Pattern.quote(java.io.File.pathSeparator))) {
            addRoot(roots, value);
        }
    }

    private static void addRoot(Set<Path> roots, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            addRoot(roots, Path.of(value));
        } catch (RuntimeException ignored) {
            // Ignore malformed optional roots and retain the valid policy entries.
        }
    }

    private static void addRoot(Set<Path> roots, Path value) {
        if (value != null) {
            roots.add(value.toAbsolutePath().normalize());
        }
    }
}
