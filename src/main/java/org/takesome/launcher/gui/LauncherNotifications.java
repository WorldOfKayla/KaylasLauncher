package org.takesome.launcher.gui;

import org.takesome.Launcher;

import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Lua-driven launcher notification facade. */
public final class LauncherNotifications {
    private static final String SCRIPT = "assets/scripts/launcher/notifications.lua";

    private LauncherNotifications() {
    }

    public static void show(
            Launcher launcher,
            String type,
            String location,
            long durationMs,
            String message
    ) {
        Map<String, Object> payload = basePayload(type, location, durationMs);
        payload.put("message", message == null ? "" : message);
        execute(launcher, payload);
    }

    public static void showLocalized(
            Launcher launcher,
            String type,
            String location,
            long durationMs,
            String localeKey,
            Map<String, ?> replacements,
            Rectangle bounds
    ) {
        Map<String, Object> payload = basePayload(type, location, durationMs);
        payload.put("localeKey", localeKey == null ? "" : localeKey);
        payload.put("replacements", replacements == null ? Map.of() : replacements);
        if (bounds != null) {
            payload.put("bounds", Map.of(
                    "x", bounds.x,
                    "y", bounds.y,
                    "width", bounds.width,
                    "height", bounds.height
            ));
        }
        execute(launcher, payload);
    }

    private static Map<String, Object> basePayload(
            String type,
            String location,
            long durationMs
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type == null || type.isBlank() ? "INFO" : type);
        payload.put("location", location == null || location.isBlank() ? "BOTTOM_RIGHT" : location);
        payload.put("durationMs", Math.max(0L, durationMs));
        return payload;
    }

    private static void execute(Launcher launcher, Map<String, Object> payload) {
        Objects.requireNonNull(launcher, "launcher");
        launcher.getGuiBuilder()
                .getComponentFactory()
                .getLuaUiScriptEngine()
                .executeScript(SCRIPT, payload);
    }
}
