package org.takesome.launcher.gui.loadingManager;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.gui.scripting.LuaConfigScript;
import org.takesome.kaylasEngine.gui.scripting.UiScriptContext;
import org.takesome.launcher.gui.LauncherUiProvider;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Map;
import java.util.Objects;

/**
 * Launcher-owned loading UI visual policy loaded from Lua.
 *
 * KaylasUIEngine executes Lua and provides generic functionality. The launcher owns visual policy.
 */
final class LoadingUiScriptConfig {
    private static final LoadingUiScriptConfig FALLBACK = new LoadingUiScriptConfig(
            new Overlay("loadingOverlay", Color.BLACK, 0, 1, 1, 16, 0, 0, -1, -1),
            new Window(true, 20),
            new Progress(true)
    );

    private final Overlay overlay;
    private final Window window;
    private final Progress progress;

    private LoadingUiScriptConfig(Overlay overlay, Window window, Progress progress) {
        this.overlay = Objects.requireNonNull(overlay, "overlay");
        this.window = Objects.requireNonNull(window, "window");
        this.progress = Objects.requireNonNull(progress, "progress");
    }

    static LoadingUiScriptConfig load(Launcher launcher) {
        try {
            UiScriptContext context = launcher.getGuiBuilder()
                    .getComponentFactory()
                    .getLuaUiScriptEngine()
                    .getContext();
            String scriptPath = LauncherUiProvider.load().loadingUiScriptPath();
            Map<String, Object> root = LuaConfigScript.load(context, scriptPath);
            return new LoadingUiScriptConfig(
                    Overlay.from(mapValue(root, "overlay")),
                    Window.from(mapValue(root, "window")),
                    Progress.from(mapValue(root, "progress"))
            );
        } catch (Exception error) {
            Engine.getLOGGER().warn("Unable to load launcher loading UI Lua config. Using fallback.", error);
            return FALLBACK;
        }
    }

    Overlay overlay() {
        return overlay;
    }

    Window window() {
        return window;
    }

    Progress progress() {
        return progress;
    }

    static final class Overlay {
        private final String name;
        private final Color color;
        private final int targetAlpha;
        private final int fadeInMs;
        private final int fadeOutMs;
        private final int frameDelayMs;
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private Overlay(String name, Color color, int targetAlpha, int fadeInMs, int fadeOutMs,
                        int frameDelayMs, int x, int y, int width, int height) {
            this.name = name;
            this.color = color;
            this.targetAlpha = clamp(targetAlpha, 0, 255);
            this.fadeInMs = Math.max(1, fadeInMs);
            this.fadeOutMs = Math.max(1, fadeOutMs);
            this.frameDelayMs = Math.max(1, frameDelayMs);
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private static Overlay from(Map<String, Object> table) {
            String name = stringValue(table, "name", FALLBACK.overlay.name);
            Color color = colorValue(table, "color", FALLBACK.overlay.color);
            int alpha = alphaValue(table, FALLBACK.overlay.targetAlpha);
            int fadeInMs = intValue(table, "fadeInMs", FALLBACK.overlay.fadeInMs);
            int fadeOutMs = intValue(table, "fadeOutMs", FALLBACK.overlay.fadeOutMs);
            int frameDelayMs = intValue(table, "frameDelayMs", FALLBACK.overlay.frameDelayMs);
            int x = intValue(table, "x", FALLBACK.overlay.x);
            int y = intValue(table, "y", FALLBACK.overlay.y);
            int width = intValue(table, "width", FALLBACK.overlay.width);
            int height = intValue(table, "height", FALLBACK.overlay.height);
            return new Overlay(name, color, alpha, fadeInMs, fadeOutMs, frameDelayMs, x, y, width, height);
        }

        String name() {
            return name;
        }

        Color color() {
            return color;
        }

        int targetAlpha() {
            return targetAlpha;
        }

        int fadeInMs() {
            return fadeInMs;
        }

        int fadeOutMs() {
            return fadeOutMs;
        }

        int frameDelayMs() {
            return frameDelayMs;
        }

        Rectangle bounds(int frameWidth, int frameHeight) {
            int resolvedWidth = width < 0 ? frameWidth : width;
            int resolvedHeight = height < 0 ? frameHeight : height;
            return new Rectangle(x, y, Math.max(0, resolvedWidth), Math.max(0, resolvedHeight));
        }
    }

    static final class Window {
        private final boolean alwaysOnTop;
        private final int cornerRadius;

        private Window(boolean alwaysOnTop, int cornerRadius) {
            this.alwaysOnTop = alwaysOnTop;
            this.cornerRadius = Math.max(0, cornerRadius);
        }

        private static Window from(Map<String, Object> table) {
            return new Window(
                    booleanValue(table, "alwaysOnTop", FALLBACK.window.alwaysOnTop),
                    intValue(table, "cornerRadius", FALLBACK.window.cornerRadius)
            );
        }

        boolean alwaysOnTop() {
            return alwaysOnTop;
        }

        int cornerRadius() {
            return cornerRadius;
        }
    }

    static final class Progress {
        private final boolean enabled;

        private Progress(boolean enabled) {
            this.enabled = enabled;
        }

        private static Progress from(Map<String, Object> table) {
            return new Progress(booleanValue(table, "enabled", FALLBACK.progress.enabled));
        }

        boolean enabled() {
            return enabled;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Map<String, Object> root, String key) {
        Object value = root == null ? null : root.get(key);
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static String stringValue(Map<String, Object> table, String key, String fallback) {
        Object value = table.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static int intValue(Map<String, Object> table, String key, int fallback) {
        Object value = table.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static boolean booleanValue(Map<String, Object> table, String key, boolean fallback) {
        Object value = table.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text.trim());
        }
        return fallback;
    }

    private static int alphaValue(Map<String, Object> table, int fallback) {
        Object alpha = table.get("alpha");
        if (alpha instanceof Number number) {
            return clamp(number.intValue(), 0, 255);
        }
        Object opacity = table.get("opacity");
        if (opacity instanceof Number number) {
            return clamp((int) Math.round(number.doubleValue() * 255.0), 0, 255);
        }
        return fallback;
    }

    private static Color colorValue(Map<String, Object> table, String key, Color fallback) {
        Object value = table.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            String raw = String.valueOf(value).trim();
            return Color.decode(raw.startsWith("#") ? raw : "#" + raw);
        } catch (Exception error) {
            Engine.getLOGGER().warn("Invalid loading overlay color '{}'.", value);
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
