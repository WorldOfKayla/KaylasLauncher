package org.takesome.launcher.auth;

import java.util.Locale;

public enum AuthProviderType {
    NO_PASSWORD,
    WEB_API,
    WS;

    public static AuthProviderType from(Object value) {
        if (value == null) {
            return WEB_API;
        }
        String normalized = String.valueOf(value)
                .trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return WEB_API;
        }
        return switch (normalized) {
            case "NO_PASSWORD", "NOPASSWORD", "NO_PASS", "NO_PASSWD", "LOCAL", "OFFLINE" -> NO_PASSWORD;
            case "WS", "WEBSOCKET", "WEB_SOCKET" -> WS;
            case "WEB", "WEB_API", "API", "HTTP", "HTTPS" -> WEB_API;
            default -> WEB_API;
        };
    }
}
