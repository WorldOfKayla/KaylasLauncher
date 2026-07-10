package org.takesome.launcher.security;

import org.takesome.kaylasEngine.Engine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/** Fail-closed response for confirmed SecureProcess module-audit violations. */
public final class SecureProcessIncidentHandler {
    public static final int HALT_EXIT_CODE = 173;

    private static final AtomicBoolean HALTING = new AtomicBoolean();

    private SecureProcessIncidentHandler() {
    }

    public static void handle(SecureProcessAuditReport.Module module) {
        if (module == null || !HALTING.compareAndSet(false, true)) {
            return;
        }

        String incident = formatIncident(module);
        if (Engine.LOGGER != null) {
            Engine.LOGGER.fatal("SecureProcess confirmed process-integrity violation. Halting JVM. {}", incident);
        } else {
            System.err.println("SecureProcess confirmed process-integrity violation. Halting JVM. " + incident);
        }

        persistIncident(incident);
        Runtime.getRuntime().halt(HALT_EXIT_CODE);
    }

    private static String formatIncident(SecureProcessAuditReport.Module module) {
        return "timestamp=" + Instant.now()
                + ", finding=" + module.finding()
                + ", path=" + module.path()
                + ", sha256=" + module.sha256()
                + ", baseAddress=" + module.baseAddress()
                + ", imageSize=" + module.imageSize()
                + ", authenticodeTrusted=" + module.signatureTrusted()
                + ", authenticodeStatus=" + module.signatureStatus();
    }

    private static void persistIncident(String incident) {
        try {
            Path logDirectory = Path.of(System.getProperty("log.dir", System.getProperty("user.dir", ".")), "logs")
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(logDirectory);
            Files.writeString(
                    logDirectory.resolve("secure-process-incidents.log"),
                    incident + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
            // Failure to persist diagnostics must not delay fail-closed termination.
        }
    }
}
