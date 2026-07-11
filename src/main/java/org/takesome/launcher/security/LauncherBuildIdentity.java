package org.takesome.launcher.security;

import org.takesome.Launcher;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/** Computes a stable launcher build identity for native attestation. */
public final class LauncherBuildIdentity {
    private static final List<String> EXTERNAL_IDENTITY_RESOURCES = List.of("engine.json");

    private LauncherBuildIdentity() {
    }

    public static String sha256() {
        try {
            URI location = Launcher.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI();
            Path codeSource = Path.of(location).toAbsolutePath().normalize();
            if (Files.isRegularFile(codeSource)) {
                return sha256File(codeSource);
            }
            if (Files.isDirectory(codeSource)) {
                return sha256Directory(codeSource);
            }
            throw new IllegalStateException("Unsupported launcher code source: " + codeSource);
        } catch (Exception error) {
            throw new IllegalStateException("Unable to calculate launcher build identity", error);
        }
    }

    private static String sha256File(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        updateFile(digest, path);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String sha256Directory(Path root) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var files = Files.walk(root)) {
            for (Path file : files
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> root.relativize(path).toString()))
                    .toList()) {
                String relative = root.relativize(file)
                        .toString()
                        .replace('\\', '/');
                updateDelimited(digest, relative.getBytes(StandardCharsets.UTF_8));
                updateFile(digest, file);
            }
        }

        ClassLoader loader = Launcher.class.getClassLoader();
        for (String resource : EXTERNAL_IDENTITY_RESOURCES) {
            updateDelimited(digest, resource.getBytes(StandardCharsets.UTF_8));
            try (InputStream input = loader.getResourceAsStream(resource)) {
                if (input == null) {
                    throw new IllegalStateException("Launcher identity resource is missing: " + resource);
                }
                updateStream(digest, input);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void updateFile(MessageDigest digest, Path file) throws Exception {
        try (InputStream input = Files.newInputStream(file)) {
            updateStream(digest, input);
        }
    }

    private static void updateStream(MessageDigest digest, InputStream input) throws Exception {
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            if (read > 0) {
                digest.update(buffer, 0, read);
            }
        }
        digest.update((byte) 0xff);
    }

    private static void updateDelimited(MessageDigest digest, byte[] value) {
        digest.update(value);
        digest.update((byte) 0);
    }
}
