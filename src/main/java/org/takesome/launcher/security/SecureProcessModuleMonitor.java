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
        monitor.executor.scheduleWithFixedDelay(
                monitor::auditSafely,
                0,
                Math.max(1, effectiveInterval.toSeconds()),
                TimeUnit.SECONDS
        );
        return monitor;
    }

    private void auditSafely() {
        try {
            SecureProcessAuditReport report = SecureProcessAudit.snapshot();
            if (report.modules() == null) {
                return;
            }
            synchronized (observedModules) {
                for (SecureProcessAuditReport.Module module : report.modules()) {
                    String previousHash = observedModules.putIfAbsent(module.path(), module.sha256());
                    if (previousHash == null && isFinding(module)) {
                        findingConsumer.accept(module);
                    } else if (previousHash != null && !Objects.equals(previousHash, module.sha256())) {
                        findingConsumer.accept(module);
                        observedModules.put(module.path(), module.sha256());
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // Audit must never destabilize launcher execution.
        }
    }

    private static boolean isFinding(SecureProcessAuditReport.Module module) {
        return module.finding() == SecureProcessAuditReport.Finding.UNTRUSTED_LOCATION
                || module.finding() == SecureProcessAuditReport.Finding.MISSING_FILE
                || module.finding() == SecureProcessAuditReport.Finding.AUDIT_ERROR;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
