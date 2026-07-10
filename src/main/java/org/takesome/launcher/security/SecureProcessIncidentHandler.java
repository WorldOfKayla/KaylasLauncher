package org.takesome.launcher.security;

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
        if (module == null) {
            SecureProcessLog.logger().error("SecureProcess incident handler received a null module");
            return;
        }
        if (!HALTING.compareAndSet(false, true)) {
            SecureProcessLog.logger().warn("SecureProcess halt already in progress; duplicate finding ignored for '{}'", module.path());
            return;
        }

        String incident = formatIncident(module);
        SecureProcessLog.logger().fatal(
                "Confirmed process-integrity violation; persisting incident and halting JVM with exitCode={}. {}",
                HALT_EXIT_CODE,
                incident
        );

        Path incidentFile = persistIncident(incident);
        if (incidentFile != null) {
            SecureProcessLog.logger().fatal("Incident persisted to '{}'", incidentFile);
        }
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

    private static Path persistIncident(String incident) {
        try {
            Path logDirectory = Path.of(System.getProperty("log.dir", System.getProperty("user.dir", ".")), "logs")
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(logDirectory);
            Path incidentFile = logDirectory.resolve("secure-process-incidents.log");
            Files.writeString(
                    incidentFile,
                    incident + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );
            return incidentFile;
        } catch (IOException error) {
            SecureProcessLog.logger().error("Failed to persist SecureProcess incident report", error);
            return null;
        }
    }
}
