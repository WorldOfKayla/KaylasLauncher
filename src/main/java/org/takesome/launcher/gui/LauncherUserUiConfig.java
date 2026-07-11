package org.takesome.launcher.gui;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.gui.scripting.LuaConfigScript;
import org.takesome.kaylasEngine.gui.scripting.UiScriptContext;

import java.util.Map;
import java.util.Objects;

/**
 * Launcher user-panel policy loaded from {@code assets/scripts/launcher/User.lua}.
 *
 * <p>User.java should orchestrate runtime behavior only. UI strings, component IDs, panel specs,
 * dimensions, timings and resource paths belong to Lua.</p>
 */
public final class LauncherUserUiConfig {
    private static final LauncherUserUiConfig FALLBACK = new LauncherUserUiConfig(
            new Panels(
                    "userPane",
                    "loggedForm",
                    "newsForm",
                    "userBadges",
                    "loggedForm->true|newsForm->true|authForm->false",
                    "loggedForm->false|newsForm->true|authForm->true",
                    "loggedForm->false|newsForm->true|authForm->true"
            ),
            new Auth(200),
            new Balance("crystals", "units", "0"),
            new ServerBox("valuesChanged", "serversLoaded"),
            new Discord("general.launcher", "game.login", "login", "launcher"),
            new Greet("logged.greet", "login"),
            new Loaders("GET"),
            new HeadIcon(72, 80),
            new Badges(5, 4, 25, 25,
                    "user.badges.singleFailure",
                    "user.badges.allFailure"),
            new SkinHover(180, 15),
            new LoginNotification("auth.loggedIn", "login", 10, 40, 340, 45, 3000),
            new TaskManager("admin", "assets/ui/icons/threadBolt.png", false),
            new NewsItem(10, 10, 10, 10, "#000000", "#808080", "mcfontBold", "mcfont", 11),
            new Tasks("updateServer")
    );

    private final Panels panels;
    private final Auth auth;
    private final Balance balance;
    private final ServerBox serverBox;
    private final Discord discord;
    private final Greet greet;
    private final Loaders loaders;
    private final HeadIcon headIcon;
    private final Badges badges;
    private final SkinHover skinHover;
    private final LoginNotification loginNotification;
    private final TaskManager taskManager;
    private final NewsItem newsItem;
    private final Tasks tasks;

    private LauncherUserUiConfig(Panels panels,
                                 Auth auth,
                                 Balance balance,
                                 ServerBox serverBox,
                                 Discord discord,
                                 Greet greet,
                                 Loaders loaders,
                                 HeadIcon headIcon,
                                 Badges badges,
                                 SkinHover skinHover,
                                 LoginNotification loginNotification,
                                 TaskManager taskManager,
                                 NewsItem newsItem,
                                 Tasks tasks) {
        this.panels = Objects.requireNonNull(panels, "panels");
        this.auth = Objects.requireNonNull(auth, "auth");
        this.balance = Objects.requireNonNull(balance, "balance");
        this.serverBox = Objects.requireNonNull(serverBox, "serverBox");
        this.discord = Objects.requireNonNull(discord, "discord");
        this.greet = Objects.requireNonNull(greet, "greet");
        this.loaders = Objects.requireNonNull(loaders, "loaders");
        this.headIcon = Objects.requireNonNull(headIcon, "headIcon");
        this.badges = Objects.requireNonNull(badges, "badges");
        this.skinHover = Objects.requireNonNull(skinHover, "skinHover");
        this.loginNotification = Objects.requireNonNull(loginNotification, "loginNotification");
        this.taskManager = Objects.requireNonNull(taskManager, "taskManager");
        this.newsItem = Objects.requireNonNull(newsItem, "newsItem");
        this.tasks = Objects.requireNonNull(tasks, "tasks");
    }

    public static String rootPanelId() {
        return FALLBACK.panels.userPane();
    }

    public static LauncherUserUiConfig load(Launcher launcher) {
        try {
            UiScriptContext context = launcher.getGuiBuilder()
                    .getComponentFactory()
                    .getLuaUiScriptEngine()
                    .getContext();
            String scriptPath = LauncherUiProvider.load().scripts().userUi();
            Map<String, Object> user = LuaConfigScript.load(context, scriptPath);
            return new LauncherUserUiConfig(
                    Panels.from(mapValue(user, "panels")),
                    Auth.from(mapValue(user, "auth")),
                    Balance.from(mapValue(user, "balance")),
                    ServerBox.from(mapValue(user, "serverBox")),
                    Discord.from(mapValue(user, "discord")),
                    Greet.from(mapValue(user, "greet")),
                    Loaders.from(mapValue(user, "loaders")),
                    HeadIcon.from(mapValue(user, "headIcon")),
                    Badges.from(mapValue(user, "badges")),
                    SkinHover.from(mapValue(user, "skinHover")),
                    LoginNotification.from(mapValue(user, "loginNotification")),
                    TaskManager.from(mapValue(user, "taskManager")),
                    NewsItem.from(mapValue(user, "newsItem")),
                    Tasks.from(mapValue(user, "tasks"))
            );
        } catch (Exception error) {
            Engine.getLOGGER().warn("Unable to load launcher user UI Lua config. Using fallback.", error);
            return FALLBACK;
        }
    }

    public Panels panels() { return panels; }
    public Auth auth() { return auth; }
    public Balance balance() { return balance; }
    public ServerBox serverBox() { return serverBox; }
    public Discord discord() { return discord; }
    public Greet greet() { return greet; }
    public Loaders loaders() { return loaders; }
    public HeadIcon headIcon() { return headIcon; }
    public Badges badges() { return badges; }
    public SkinHover skinHover() { return skinHover; }
    public LoginNotification loginNotification() { return loginNotification; }
    public TaskManager taskManager() { return taskManager; }
    public NewsItem newsItem() { return newsItem; }
    public Tasks tasks() { return tasks; }

    public record Panels(String userPane,
                         String loggedForm,
                         String newsForm,
                         String userBadges,
                         String authorisedSpec,
                         String pendingSpec,
                         String unauthorisedSpec) {
        private static Panels from(Map<String, Object> table) {
            Panels fallback = FALLBACK.panels;
            return new Panels(
                    stringValue(table, "userPane", fallback.userPane),
                    stringValue(table, "loggedForm", fallback.loggedForm),
                    stringValue(table, "newsForm", fallback.newsForm),
                    stringValue(table, "userBadges", fallback.userBadges),
                    stringValue(table, "authorisedSpec", fallback.authorisedSpec),
                    stringValue(table, "pendingSpec", fallback.pendingSpec),
                    stringValue(table, "unauthorisedSpec", fallback.unauthorisedSpec)
            );
        }
    }

    public record Auth(int waitIntervalMs) {
        private static Auth from(Map<String, Object> table) {
            return new Auth(positiveIntValue(table, "waitIntervalMs", FALLBACK.auth.waitIntervalMs));
        }
    }

    public record Balance(String crystalsKey, String unitsKey, String fallbackAmount) {
        private static Balance from(Map<String, Object> table) {
            Balance fallback = FALLBACK.balance;
            return new Balance(
                    stringValue(table, "crystalsKey", fallback.crystalsKey),
                    stringValue(table, "unitsKey", fallback.unitsKey),
                    stringValue(table, "fallbackAmount", fallback.fallbackAmount)
            );
        }
    }

    public record ServerBox(String valuesChangedEvent, String valuesChangedReason) {
        private static ServerBox from(Map<String, Object> table) {
            ServerBox fallback = FALLBACK.serverBox;
            return new ServerBox(
                    stringValue(table, "valuesChangedEvent", fallback.valuesChangedEvent),
                    stringValue(table, "valuesChangedReason", fallback.valuesChangedReason)
            );
        }
    }

    public record Discord(String launcherLocaleKey, String loginLocaleKey, String loginPlaceholder, String iconKey) {
        private static Discord from(Map<String, Object> table) {
            Discord fallback = FALLBACK.discord;
            return new Discord(
                    stringValue(table, "launcherLocaleKey", fallback.launcherLocaleKey),
                    stringValue(table, "loginLocaleKey", fallback.loginLocaleKey),
                    stringValue(table, "loginPlaceholder", fallback.loginPlaceholder),
                    stringValue(table, "iconKey", fallback.iconKey)
            );
        }
    }

    public record Greet(String localeKey, String loginPlaceholder) {
        private static Greet from(Map<String, Object> table) {
            Greet fallback = FALLBACK.greet;
            return new Greet(
                    stringValue(table, "localeKey", fallback.localeKey),
                    stringValue(table, "loginPlaceholder", fallback.loginPlaceholder)
            );
        }
    }

    public record Loaders(String headRequestMethod) {
        private static Loaders from(Map<String, Object> table) {
            return new Loaders(stringValue(table, "headRequestMethod", FALLBACK.loaders.headRequestMethod));
        }
    }

    public record HeadIcon(int size, int radius) {
        private static HeadIcon from(Map<String, Object> table) {
            return new HeadIcon(
                    positiveIntValue(table, "size", FALLBACK.headIcon.size),
                    positiveIntValue(table, "radius", FALLBACK.headIcon.radius)
            );
        }
    }

    public record Badges(int hgap,
                         int vgap,
                         int iconWidth,
                         int iconHeight,
                         String singleBadgeFailureKey,
                         String allBadgesFailureKey) {
        private static Badges from(Map<String, Object> table) {
            Badges fallback = FALLBACK.badges;
            return new Badges(
                    intValue(table, "hgap", fallback.hgap),
                    intValue(table, "vgap", fallback.vgap),
                    positiveIntValue(table, "iconWidth", fallback.iconWidth),
                    positiveIntValue(table, "iconHeight", fallback.iconHeight),
                    stringValue(table, "singleBadgeFailureKey", fallback.singleBadgeFailureKey),
                    stringValue(table, "allBadgesFailureKey", fallback.allBadgesFailureKey)
            );
        }
    }

    public record SkinHover(int durationMs, int steps) {
        private static SkinHover from(Map<String, Object> table) {
            return new SkinHover(
                    positiveIntValue(table, "durationMs", FALLBACK.skinHover.durationMs),
                    positiveIntValue(table, "steps", FALLBACK.skinHover.steps)
            );
        }

        public int frameDelayMs() {
            return Math.max(1, durationMs / Math.max(1, steps));
        }
    }

    public record LoginNotification(String localeKey,
                                    String loginPlaceholder,
                                    int x,
                                    int yOffset,
                                    int width,
                                    int height,
                                    int durationMs) {
        private static LoginNotification from(Map<String, Object> table) {
            LoginNotification fallback = FALLBACK.loginNotification;
            return new LoginNotification(
                    stringValue(table, "localeKey", fallback.localeKey),
                    stringValue(table, "loginPlaceholder", fallback.loginPlaceholder),
                    intValue(table, "x", fallback.x),
                    intValue(table, "yOffset", fallback.yOffset),
                    positiveIntValue(table, "width", fallback.width),
                    positiveIntValue(table, "height", fallback.height),
                    positiveIntValue(table, "durationMs", fallback.durationMs)
            );
        }
    }

    public record TaskManager(String adminGroup, String iconPath, boolean resizable) {
        private static TaskManager from(Map<String, Object> table) {
            TaskManager fallback = FALLBACK.taskManager;
            return new TaskManager(
                    stringValue(table, "adminGroup", fallback.adminGroup),
                    stringValue(table, "iconPath", fallback.iconPath),
                    booleanValue(table, "resizable", fallback.resizable)
            );
        }
    }

    public record NewsItem(int insetTop,
                           int insetLeft,
                           int insetBottom,
                           int insetRight,
                           String keyColor,
                           String valueColor,
                           String keyFont,
                           String valueFont,
                           int fontSize) {
        private static NewsItem from(Map<String, Object> table) {
            NewsItem fallback = FALLBACK.newsItem;
            return new NewsItem(
                    intValue(table, "insetTop", fallback.insetTop),
                    intValue(table, "insetLeft", fallback.insetLeft),
                    intValue(table, "insetBottom", fallback.insetBottom),
                    intValue(table, "insetRight", fallback.insetRight),
                    stringValue(table, "keyColor", fallback.keyColor),
                    stringValue(table, "valueColor", fallback.valueColor),
                    stringValue(table, "keyFont", fallback.keyFont),
                    stringValue(table, "valueFont", fallback.valueFont),
                    positiveIntValue(table, "fontSize", fallback.fontSize)
            );
        }
    }

    public record Tasks(String updateServerPrefix) {
        private static Tasks from(Map<String, Object> table) {
            return new Tasks(stringValue(table, "updateServerPrefix", FALLBACK.tasks.updateServerPrefix));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Map<String, Object> root, String key) {
        Object value = root == null ? null : root.get(key);
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static String stringValue(Map<String, Object> table, String key, String fallback) {
        Object value = table == null ? null : table.get(key);
        String resolved = value == null ? null : String.valueOf(value);
        return resolved == null || resolved.isBlank() ? fallback : resolved;
    }

    private static int intValue(Map<String, Object> table, String key, int fallback) {
        Object value = table == null ? null : table.get(key);
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

    private static int positiveIntValue(Map<String, Object> table, String key, int fallback) {
        return Math.max(1, intValue(table, key, fallback));
    }

    private static boolean booleanValue(Map<String, Object> table, String key, boolean fallback) {
        Object value = table == null ? null : table.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text.trim());
        }
        return fallback;
    }
}
