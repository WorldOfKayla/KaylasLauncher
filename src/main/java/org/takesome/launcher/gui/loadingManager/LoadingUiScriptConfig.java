package org.takesome.launcher.gui.loadingManager;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.gui.scripting.LuaConfigScript;
import org.takesome.kaylasEngine.gui.scripting.LuaConfigValues;
import org.takesome.kaylasEngine.gui.scripting.UiScriptContext;
import org.takesome.launcher.gui.LauncherUiProvider;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Launcher-owned loading UI visual policy loaded from Lua.
 *
 * KaylasUIEngine executes Lua and provides generic config readers. The launcher owns visual policy.
 */
final class LoadingUiScriptConfig {
    private static final String DEFAULT_PROGRESS_MESSAGES_SECTION = "progressMessages";
    private static final List<String> DEFAULT_PROGRESS_MESSAGE_KEYS = List.of();

    private static final LoadingUiScriptConfig FALLBACK = new LoadingUiScriptConfig(
            new Overlay("loadingOverlay", Color.BLACK, 0, 1, 1, 16, 0, 0, -1, -1),
            new Window(true, 20),
            new Typography(
                    new TextStyle("titleBold", "", 16, "plain", ""),
                    new TextStyle("title", "", 11, "plain", "")
            ),
            new Progress(
                    true,
                    100,
                    1,
                    0,
                    0,
                    500,
                    16,
                    -1,
                    true,
                    true,
                    true,
                    false,
                    true,
                    false,
                    true,
                    true,
                    DEFAULT_PROGRESS_MESSAGES_SECTION,
                    "assets/messages.json",
                    "assets/animation_config.json",
                    DEFAULT_PROGRESS_MESSAGE_KEYS,
                    "progressMini",
                    "",
                    0,
                    "",
                    ""
            )
    );

    private final Overlay overlay;
    private final Window window;
    private final Typography typography;
    private final Progress progress;

    private LoadingUiScriptConfig(Overlay overlay,
                                  Window window,
                                  Typography typography,
                                  Progress progress) {
        this.overlay = Objects.requireNonNull(overlay, "overlay");
        this.window = Objects.requireNonNull(window, "window");
        this.typography = Objects.requireNonNull(typography, "typography");
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
                    Overlay.from(LuaConfigValues.map(root, "overlay")),
                    Window.from(LuaConfigValues.map(root, "window")),
                    Typography.from(
                            LuaConfigValues.map(root, "typography"),
                            FALLBACK.typography
                    ),
                    Progress.from(LuaConfigValues.map(root, "progress"))
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

    Typography typography() {
        return typography;
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
            this.targetAlpha = LuaConfigValues.clamp(targetAlpha, 0, 255);
            this.fadeInMs = Math.max(1, fadeInMs);
            this.fadeOutMs = Math.max(1, fadeOutMs);
            this.frameDelayMs = Math.max(1, frameDelayMs);
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private static Overlay from(Map<String, Object> table) {
            String name = LuaConfigValues.string(table, "name", FALLBACK.overlay.name);
            Color color = LuaConfigValues.color(table, "color", FALLBACK.overlay.color);
            int alpha = LuaConfigValues.alpha(table, FALLBACK.overlay.targetAlpha);
            int fadeInMs = LuaConfigValues.integer(table, "fadeInMs", FALLBACK.overlay.fadeInMs);
            int fadeOutMs = LuaConfigValues.integer(table, "fadeOutMs", FALLBACK.overlay.fadeOutMs);
            int frameDelayMs = LuaConfigValues.integer(table, "frameDelayMs", FALLBACK.overlay.frameDelayMs);
            int x = LuaConfigValues.integer(table, "x", FALLBACK.overlay.x);
            int y = LuaConfigValues.integer(table, "y", FALLBACK.overlay.y);
            int width = LuaConfigValues.integer(table, "width", FALLBACK.overlay.width);
            int height = LuaConfigValues.integer(table, "height", FALLBACK.overlay.height);
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
                    LuaConfigValues.bool(table, "alwaysOnTop", FALLBACK.window.alwaysOnTop),
                    LuaConfigValues.integer(table, "cornerRadius", FALLBACK.window.cornerRadius)
            );
        }

        boolean alwaysOnTop() {
            return alwaysOnTop;
        }

        int cornerRadius() {
            return cornerRadius;
        }
    }

    static final class Typography {
        private final TextStyle title;
        private final TextStyle message;

        private Typography(TextStyle title, TextStyle message) {
            this.title = Objects.requireNonNull(title, "title");
            this.message = Objects.requireNonNull(message, "message");
        }

        private static Typography from(Map<String, Object> table, Typography fallback) {
            return new Typography(
                    TextStyle.from(LuaConfigValues.map(table, "title"), fallback.title),
                    TextStyle.from(LuaConfigValues.map(table, "message"), fallback.message)
            );
        }

        TextStyle title() { return title; }
        TextStyle message() { return message; }
    }

    static final class TextStyle {
        private final String styleName;
        private final String fontName;
        private final int fontSize;
        private final String fontStyle;
        private final String color;

        private TextStyle(String styleName,
                          String fontName,
                          int fontSize,
                          String fontStyle,
                          String color) {
            this.styleName = styleName == null ? "" : styleName.trim();
            this.fontName = fontName == null ? "" : fontName.trim();
            this.fontSize = Math.max(0, fontSize);
            this.fontStyle = fontStyle == null ? "" : fontStyle.trim();
            this.color = color == null ? "" : color.trim();
        }

        private static TextStyle from(Map<String, Object> table, TextStyle fallback) {
            return new TextStyle(
                    LuaConfigValues.string(table, "style", fallback.styleName),
                    LuaConfigValues.string(table, "font", fallback.fontName),
                    LuaConfigValues.integer(table, "fontSize", fallback.fontSize),
                    LuaConfigValues.string(table, "fontStyle", fallback.fontStyle),
                    LuaConfigValues.string(table, "color", fallback.color)
            );
        }

        String styleName() { return styleName; }
        String fontName() { return fontName; }
        int fontSize() { return fontSize; }
        String fontStyle() { return fontStyle; }
        String color() { return color; }
    }

    static final class Progress {
        private final boolean enabled;
        private final int updateMs;
        private final int step;
        private final int initialDelayMs;
        private final int cycleDelayMs;
        private final int timelineDurationMs;
        private final int timelineFrameDelayMs;
        private final int maxValue;
        private final boolean loop;
        private final boolean randomMessages;
        private final boolean showText;
        private final boolean showPercent;
        private final boolean resetOnStop;
        private final boolean hideOnStop;
        private final boolean animateEntrance;
        private final boolean animateExit;
        private final String messagesSection;
        private final String messagesResource;
        private final String animationConfigResource;
        private final List<String> messageKeys;
        private final String styleName;
        private final String fontName;
        private final int fontSize;
        private final String fontStyle;
        private final String textColor;

        private Progress(boolean enabled,
                         int updateMs,
                         int step,
                         int initialDelayMs,
                         int cycleDelayMs,
                         int timelineDurationMs,
                         int timelineFrameDelayMs,
                         int maxValue,
                         boolean loop,
                         boolean randomMessages,
                         boolean showText,
                         boolean showPercent,
                         boolean resetOnStop,
                         boolean hideOnStop,
                         boolean animateEntrance,
                         boolean animateExit,
                         String messagesSection,
                         String messagesResource,
                         String animationConfigResource,
                         List<String> messageKeys,
                         String styleName,
                         String fontName,
                         int fontSize,
                         String fontStyle,
                         String textColor) {
            this.enabled = enabled;
            this.updateMs = Math.max(1, updateMs);
            this.step = Math.max(1, step);
            this.initialDelayMs = Math.max(0, initialDelayMs);
            this.cycleDelayMs = Math.max(0, cycleDelayMs);
            this.timelineDurationMs = Math.max(1, timelineDurationMs);
            this.timelineFrameDelayMs = Math.max(1, timelineFrameDelayMs);
            this.maxValue = maxValue;
            this.loop = loop;
            this.randomMessages = randomMessages;
            this.showText = showText;
            this.showPercent = showPercent;
            this.resetOnStop = resetOnStop;
            this.hideOnStop = hideOnStop;
            this.animateEntrance = animateEntrance;
            this.animateExit = animateExit;
            this.messagesSection = messagesSection == null || messagesSection.isBlank()
                    ? DEFAULT_PROGRESS_MESSAGES_SECTION
                    : messagesSection.trim();
            this.messagesResource = messagesResource;
            this.animationConfigResource = animationConfigResource;
            this.messageKeys = List.copyOf(messageKeys == null ? List.of() : messageKeys);
            this.styleName = styleName == null || styleName.isBlank() ? "progressMini" : styleName.trim();
            this.fontName = fontName == null ? "" : fontName.trim();
            this.fontSize = Math.max(0, fontSize);
            this.fontStyle = fontStyle == null ? "" : fontStyle.trim();
            this.textColor = textColor == null ? "" : textColor.trim();
        }

        private static Progress from(Map<String, Object> table) {
            Progress fallback = FALLBACK.progress;
            return new Progress(
                    LuaConfigValues.bool(table, "enabled", fallback.enabled),
                    LuaConfigValues.integer(table, "updateMs", LuaConfigValues.integer(table, "progressUpdateMs", fallback.updateMs)),
                    LuaConfigValues.integer(table, "step", LuaConfigValues.integer(table, "progressStep", fallback.step)),
                    LuaConfigValues.integer(table, "initialDelayMs", fallback.initialDelayMs),
                    LuaConfigValues.integer(table, "cycleDelayMs", fallback.cycleDelayMs),
                    LuaConfigValues.integer(table, "timelineDurationMs", fallback.timelineDurationMs),
                    LuaConfigValues.integer(table, "timelineFrameDelayMs", fallback.timelineFrameDelayMs),
                    LuaConfigValues.integer(table, "maxValue", fallback.maxValue),
                    LuaConfigValues.bool(table, "loop", fallback.loop),
                    LuaConfigValues.bool(table, "randomMessages", fallback.randomMessages),
                    LuaConfigValues.bool(table, "showText", fallback.showText),
                    LuaConfigValues.bool(table, "showPercent", fallback.showPercent),
                    LuaConfigValues.bool(table, "resetOnStop", fallback.resetOnStop),
                    LuaConfigValues.bool(table, "hideOnStop", fallback.hideOnStop),
                    LuaConfigValues.bool(table, "animateEntrance", fallback.animateEntrance),
                    LuaConfigValues.bool(table, "animateExit", fallback.animateExit),
                    LuaConfigValues.string(table, "messagesSection", fallback.messagesSection),
                    LuaConfigValues.string(table, "messagesResource", fallback.messagesResource),
                    LuaConfigValues.string(table, "animationConfigResource", fallback.animationConfigResource),
                    stringList(table, "messageKeys", fallback.messageKeys),
                    LuaConfigValues.string(table, "style", fallback.styleName),
                    LuaConfigValues.string(table, "font", fallback.fontName),
                    LuaConfigValues.integer(table, "fontSize", fallback.fontSize),
                    LuaConfigValues.string(table, "fontStyle", fallback.fontStyle),
                    LuaConfigValues.string(table, "textColor", fallback.textColor)
            );
        }

        boolean enabled() { return enabled; }
        int updateMs() { return updateMs; }
        int step() { return step; }
        int timelineDurationMs() { return timelineDurationMs; }
        int timelineFrameDelayMs() { return timelineFrameDelayMs; }
        boolean loop() { return loop; }
        boolean randomMessages() { return randomMessages; }
        boolean showText() { return showText; }
        boolean showPercent() { return showPercent; }
        String messagesSection() { return messagesSection; }
        String messagesResource() { return messagesResource; }
        String animationConfigResource() { return animationConfigResource; }
        List<String> messageKeys() { return messageKeys; }
        String styleName() { return styleName; }
        String fontName() { return fontName; }
        int fontSize() { return fontSize; }
        String fontStyle() { return fontStyle; }
        String textColor() { return textColor; }

        org.takesome.kaylasEngine.gui.animation.ProgressBarAnimator.Options toEngineOptions() {
            return new org.takesome.kaylasEngine.gui.animation.ProgressBarAnimator.Options()
                    .setProgressUpdateMs(updateMs)
                    .setProgressStep(step)
                    .setInitialDelayMs(initialDelayMs)
                    .setCycleDelayMs(cycleDelayMs)
                    .setTimelineDurationMs(timelineDurationMs)
                    .setTimelineFrameDelayMs(timelineFrameDelayMs)
                    .setMaxValue(maxValue)
                    .setLoop(loop)
                    .setRandomMessages(randomMessages)
                    .setShowText(showText)
                    .setShowPercent(showPercent)
                    .setResetOnStop(resetOnStop)
                    .setHideOnStop(hideOnStop)
                    .setAnimateEntrance(animateEntrance)
                    .setAnimateExit(animateExit);
        }
    }

    private static List<String> stringList(Map<String, Object> table, String key, List<String> fallback) {
        Object value = table == null ? null : table.get(key);
        if (value instanceof String text) {
            String trimmed = text.trim();
            return trimmed.isEmpty() ? fallback : List.of(trimmed);
        }
        if (!(value instanceof Map<?, ?> map)) {
            return fallback;
        }

        List<Map.Entry<?, ?>> entries = new ArrayList<>(map.entrySet());
        entries.sort(Comparator.comparingInt(entry -> numericKey(entry.getKey())));

        List<String> values = new ArrayList<>();
        for (Map.Entry<?, ?> entry : entries) {
            Object rawValue = entry.getValue();
            if (rawValue == null) {
                continue;
            }
            String stringValue = String.valueOf(rawValue).trim();
            if (!stringValue.isEmpty()) {
                values.add(stringValue);
            }
        }
        return values.isEmpty() ? fallback : List.copyOf(values);
    }

    private static int numericKey(Object key) {
        if (key instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(key));
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }
}
