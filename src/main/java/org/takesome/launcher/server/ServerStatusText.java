package org.takesome.launcher.server;

import org.takesome.kaylasEngine.locale.LanguageProvider;
import org.takesome.kaylasEngine.server.ServerAttributes;
import org.takesome.launcher.backend.LauncherServerStatus;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Produces concise presentation text for launcher server cards.
 *
 * <p>Keeping this logic outside {@link ServerInfoDisplayer} prevents backend response formatting,
 * endpoint normalization and fallback rules from leaking into Swing orchestration.</p>
 */
final class ServerStatusText {
    private static final String SEPARATOR = " | ";

    private final Function<String, String> localize;

    ServerStatusText(LanguageProvider language) {
        this(Objects.requireNonNull(language, "language")::getString);
    }

    ServerStatusText(Function<String, String> localize) {
        this.localize = Objects.requireNonNull(localize, "localize");
    }

    Summary summarize(ServerAttributes server) {
        Objects.requireNonNull(server, "server");

        String title = join(" ", server.getServerName(), server.getServerVersion());
        if (title.isEmpty()) {
            title = "Server";
        }

        String runtime = joinDistinct(
                SEPARATOR,
                server.getCoreType(),
                server.getJreVersion()
        );
        if (runtime.isEmpty()) {
            runtime = "Backend managed";
        }

        return new Summary(
                title,
                safe(server.getServerDescription()),
                runtime,
                endpoint(server)
        );
    }

    String pending() {
        return localized("server.updating", "Updating server...");
    }

    String unavailable(ServerAttributes fallback) {
        String error = localized("server.serverErr", "Status unavailable");
        String metadata = metadataStatus(fallback);
        return "Backend managed".equals(metadata) ? error : error + SEPARATOR + metadata;
    }

    String status(LauncherServerStatus status, ServerAttributes fallback) {
        if (status == null) {
            return metadataStatus(fallback);
        }
        if (!status.isOnline()) {
            String message = safe(status.getMessage());
            return message.isEmpty()
                    ? localized("server.serverOff", "Server off")
                    : message;
        }

        List<String> details = new ArrayList<>();
        if (status.getPlayersOnline() >= 0 && status.getPlayersMax() >= 0) {
            String template = localize.apply("server.serverOn");
            if (template != null && !template.isBlank() && !"server.serverOn".equals(template)) {
                details.add(template
                        .replace("%%", String.valueOf(status.getPlayersOnline()))
                        .replace("##", String.valueOf(status.getPlayersMax())));
            } else {
                details.add(status.getPlayersOnline() + " / " + status.getPlayersMax());
            }
        }
        if (status.getLatencyMs() >= 0) {
            details.add(status.getLatencyMs() + " ms");
        }
        if (details.isEmpty()) {
            String message = safe(status.getMessage());
            details.add(message.isEmpty() ? "Online" : message);
        }
        return String.join(SEPARATOR, details);
    }

    String metadataStatus(ServerAttributes server) {
        if (server == null) {
            return "Backend managed";
        }
        String client = safe(server.getClient());
        return client.isEmpty() ? "Backend managed" : client;
    }

    private String endpoint(ServerAttributes server) {
        String host = safe(server.getHost());
        int port = server.getPort();
        if (host.isEmpty()) {
            return port > 0 ? String.valueOf(port) : "Endpoint unavailable";
        }
        return port > 0 ? host + ':' + port : host;
    }

    private String localized(String key, String fallback) {
        String value = localize.apply(key);
        return value == null || value.isBlank() || key.equals(value) ? fallback : value;
    }

    private static String join(String separator, String... values) {
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            String safeValue = safe(value);
            if (!safeValue.isEmpty()) {
                normalized.add(safeValue);
            }
        }
        return String.join(separator, normalized);
    }

    private static String joinDistinct(String separator, String... values) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String safeValue = safe(value);
            if (!safeValue.isEmpty()) {
                normalized.add(safeValue);
            }
        }
        return String.join(separator, normalized);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    record Summary(String title, String description, String runtime, String endpoint) {
    }
}
