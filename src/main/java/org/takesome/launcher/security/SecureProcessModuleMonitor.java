package org.takesome.launcher.security;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Periodic read-only monitor for newly loaded modules in the current process. */
public final class SecureProcessModuleMonitor implements AutoCloseable {
    private final ScheduledExecutorService executor;
    private final Consumer<SecureProcessAuditReport.Module> findingConsumer;
    private final Map<String, String> observedModules = new HashMap<>();
    private long auditSequence;

    private SecureProcessModuleMonitor(Consumer<SecureProcessAuditReport.Module> findingConsumer) {
        this.findingConsumer = Objects.requireNonNull(findingConsumer, "findingConsumer");
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "secure-process-module-audit");
            thread.setDaemon(true);
            return thread;
        });
    }

    public static SecureProcessModuleMonitor start(
            Duration interval,
            Consumer<SecureProcessAuditReport.Module> findingConsumer
    ) {
        Duration effectiveInterval = interval == null || interval.isNegative() || interval.isZero()
                ? Duration.ofSeconds(30)
                : interval;
        SecureProcessModuleMonitor monitor = new SecureProcessModuleMonitor(findingConsumer);
        long intervalSeconds = Math.max(1, effectiveInterval.toSeconds());
        SecureProcessLog.logger().info("Module audit monitor starting: intervalSeconds={}, failClosed=true", intervalSeconds);
        monitor.executor.scheduleWithFixedDelay(
                monitor::auditSafely,
                0,
                intervalSeconds,
                TimeUnit.SECONDS
        );
        return monitor;
    }

    private void auditSafely() {
        long sequence = ++auditSequence;
        long startedAt = System.nanoTime();
        try {
            SecureProcessAuditReport report = SecureProcessAudit.snapshot();
            if (report.error() != null && !report.error().isBlank()) {
                SecureProcessLog.logger().error("Module audit #{} failed: {}", sequence, report.error());
            }
            if (report.modules() == null) {
                SecureProcessLog.logger().warn("Module audit #{} returned no module collection", sequence);
                return;
            }

            int findings = 0;
            int newModules = 0;
            int changedModules = 0;
            synchronized (observedModules) {
                for (SecureProcessAuditReport.Module module : report.modules()) {
                    String previousHash = observedModules.putIfAbsent(module.path(), module.sha256());
                    if (previousHash == null) {
                        newModules++;
                        SecureProcessLog.logger().debug(
                                "Observed module: path='{}', sha256={}, finding={}, signed={}, size={}",
                                module.path(), module.sha256(), module.finding(), module.signatureTrusted(), module.imageSize());
                        if (isFinding(module)) {
                            findings++;
                            logFinding("new-untrusted-module", module, previousHash);
                            findingConsumer.accept(module);
                        }
                    } else if (!Objects.equals(previousHash, module.sha256())) {
                        findings++;
                        changedModules++;
                        logFinding("module-hash-changed", module, previousHash);
                        findingConsumer.accept(module);
                        observedModules.put(module.path(), module.sha256());
                    }
                }
            }

            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            SecureProcessLog.logger().debug(
                    "Module audit #{} completed: modules={}, new={}, changed={}, findings={}, durationMs={}",
                    sequence, report.modules().size(), newModules, changedModules, findings, durationMs);
        } catch (RuntimeException error) {
            SecureProcessLog.logger().error("Module audit #{} raised an exception", sequence, error);
        }
    }

    private static void logFinding(
            String reason,
            SecureProcessAuditReport.Module module,
            String previousHash
    ) {
        SecureProcessLog.logger().fatal(
                "Module integrity finding: reason={}, finding={}, path='{}', previousSha256={}, currentSha256={}, baseAddress={}, imageSize={}, signatureTrusted={}, signatureStatus={}",
                reason,
                module.finding(),
                module.path(),
                previousHash == null ? "none" : previousHash,
                module.sha256(),
                module.baseAddress(),
                module.imageSize(),
                module.signatureTrusted(),
                module.signatureStatus()
        );
    }

    private static boolean isFinding(SecureProcessAuditReport.Module module) {
        return module.finding() == SecureProcessAuditReport.Finding.UNTRUSTED_LOCATION
                || module.finding() == SecureProcessAuditReport.Finding.MISSING_FILE
                || module.finding() == SecureProcessAuditReport.Finding.AUDIT_ERROR;
    }

    @Override
    public void close() {
        SecureProcessLog.logger().info("Module audit monitor stopping: auditsCompleted={}, observedModules={}",
                auditSequence, observedModules.size());
        executor.shutdownNow();
    }
}
