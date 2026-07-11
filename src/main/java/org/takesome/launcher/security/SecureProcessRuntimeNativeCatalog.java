package org.takesome.launcher.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Builds a content-addressed trust catalog from native binaries shipped on the
 * runtime classpath. Trust is based only on SHA-256 equality, never file names.
 */
final class SecureProcessRuntimeNativeCatalog {
    private final Map<String, Set<String>> originsBySha256;

    private SecureProcessRuntimeNativeCatalog(Map<String, Set<String>> originsBySha256) {
        this.originsBySha256 = originsBySha256;
    }

    static SecureProcessRuntimeNativeCatalog scanRuntimeClasspath() {
        Map<String, Set<String>> catalog = new HashMap<>();
        String classPath = System.getProperty("java.class.path", "");

        for (String entry : classPath.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))) {
            if (entry == null || entry.isBlank()) {
                continue;
            }

            Path path;
            try {
                path = Path.of(entry).toAbsolutePath().normalize();
            } catch (RuntimeException error) {
                SecureProcessLog.logger().debug("Skipping invalid classpath entry '{}'", entry, error);
                continue;
            }

            try {
                if (Files.isDirectory(path)) {
                    scanDirectory(path, catalog);
                } else if (Files.isRegularFile(path) && isArchive(path)) {
                    scanJar(path, catalog);
                } else if (Files.isRegularFile(path) && isPortableExecutable(path)) {
                    addFile(path, path.toString(), catalog);
                }
            } catch (IOException | NoSuchAlgorithmException error) {
                SecureProcessLog.logger().debug("Unable to scan runtime classpath entry '{}'", path, error);
            }
        }

        Map<String, Set<String>> immutable = new HashMap<>();
        catalog.forEach((hash, origins) -> immutable.put(hash, Set.copyOf(origins)));
        SecureProcessLog.logger().info(
                "Runtime native trust catalog built: hashes={}, origins={}",
                immutable.size(),
                immutable.values().stream().mapToInt(Set::size).sum()
        );
        return new SecureProcessRuntimeNativeCatalog(Collections.unmodifiableMap(immutable));
    }

    boolean contains(String sha256) {
        return sha256 != null && originsBySha256.containsKey(sha256.toLowerCase(java.util.Locale.ROOT));
    }

    Set<String> origins(String sha256) {
        if (sha256 == null) {
            return Set.of();
        }
        return originsBySha256.getOrDefault(sha256.toLowerCase(java.util.Locale.ROOT), Set.of());
    }

    private static void scanDirectory(
            Path root,
            Map<String, Set<String>> catalog
    ) throws IOException, NoSuchAlgorithmException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                if (isArchive(path)) {
                    scanJar(path, catalog);
                } else if (isPortableExecutable(path)) {
                    addFile(path, path.toString(), catalog);
                }
            }
        }
    }

    private static void scanJar(
            Path jarPath,
            Map<String, Set<String>> catalog
    ) throws IOException, NoSuchAlgorithmException {
        try (JarFile jar = new JarFile(jarPath.toFile(), false)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                try (InputStream input = jar.getInputStream(entry)) {
                    if (!isPortableExecutable(input)) {
                        continue;
                    }
                }
                try (InputStream input = jar.getInputStream(entry)) {
                    String hash = sha256(input);
                    addOrigin(hash, jarPath + "!/" + entry.getName(), catalog);
                }
            }
        }
    }

    private static void addFile(
            Path path,
            String origin,
            Map<String, Set<String>> catalog
    ) throws IOException, NoSuchAlgorithmException {
        try (InputStream input = Files.newInputStream(path)) {
            addOrigin(sha256(input), origin, catalog);
        }
    }

    private static void addOrigin(
            String hash,
            String origin,
            Map<String, Set<String>> catalog
    ) {
        catalog.computeIfAbsent(hash, ignored -> new HashSet<>()).add(origin);
    }

    private static boolean isArchive(Path path) {
        String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(".jar") || name.endsWith(".zip");
    }

    private static boolean isPortableExecutable(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return isPortableExecutable(input);
        }
    }

    private static boolean isPortableExecutable(InputStream input) throws IOException {
        int first = input.read();
        int second = input.read();
        return first == 'M' && second == 'Z';
    }

    private static String sha256(InputStream input)
            throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            if (read > 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
