package org.takesome.launcher.gui;

import org.luaj.vm2.LuaValue;
import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.gui.components.combobox.Combobox;
import org.takesome.kaylasEngine.gui.components.constructor.ConstructedCompositeComponent;
import org.takesome.kaylasEngine.gui.components.textfield.TextField;
import org.takesome.kaylasEngine.gui.scripting.UiScriptEvent;
import org.takesome.kaylasEngine.server.ServerAttributes;
import org.takesome.kaylasEngine.server.ServerIdentity;
import org.takesome.launcher.auth.AuthStatus;
import org.takesome.launcher.gui.components.LauncherComponentLibrary;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Java runtime handlers for Lua-emitted launcher UI events.
 *
 * Lua owns simple UI intent declarations. Java remains responsible for runtime operations that need
 * launcher services, auth state, dialogs, filesystem or game startup.
 */
final class LauncherLuaUiBridge {
    private static final String SERVER_CORE_ICONS_EVENT = "launcher.serverBox.coreIcons.apply";
    private static final String DEFAULT_SERVER_BOX_ID = "serverBox";
    private static final String CORE_ICON_PROPERTY_PREFIX = "launcher.serverBox.coreIcons.";
    private static final String DEFAULT_ICON_ROOT = "assets/ui/icons/srvIcons/";
    private static final String DEFAULT_ICON_EXT = ".png";
    private static final String DEFAULT_CORE_ICON = "Vanilla";

    private final Launcher launcher;
    private final Engine engine;
    private final LauncherUiProvider ui;
    private final UserPaneAnimator userPaneAnimator;

    LauncherLuaUiBridge(Launcher launcher, LauncherUiProvider ui, UserPaneAnimator userPaneAnimator) {
        this.launcher = Objects.requireNonNull(launcher, "launcher");
        this.engine = launcher.getEngine();
        this.ui = Objects.requireNonNull(ui, "ui");
        this.userPaneAnimator = Objects.requireNonNull(userPaneAnimator, "userPaneAnimator");
    }

    void register() {
        try {
            var context = launcher.getGuiBuilder()
                    .getComponentFactory()
                    .getLuaUiScriptEngine()
                    .getContext();

            context.on(ui.events().userPaneToggle(),
                    ui.listeners().userPaneToggle(),
                    event -> userPaneAnimator.toggle());
            context.on(ui.events().openInfo(),
                    ui.listeners().openInfo(),
                    event -> openInfo());
            context.on(ui.events().openSettings(),
                    ui.listeners().openSettings(),
                    event -> openSettings());
            context.on(ui.events().back(),
                    ui.listeners().back(),
                    event -> back());
            context.on(ui.events().optionalMods(),
                    ui.listeners().optionalMods(),
                    event -> optionalMods());
            context.on(ui.events().loadingToggle(),
                    ui.listeners().loadingToggle(),
                    event -> launcher.getLoadingManager().toggleVisibility());
            context.on(ui.events().openGameDir(),
                    ui.listeners().openGameDir(),
                    event -> launcher.getSettings().openGameFolder());
            context.on(ui.events().applySettings(),
                    ui.listeners().applySettings(),
                    event -> launcher.getSettings().applySettings());
            context.on(SERVER_CORE_ICONS_EVENT,
                    "launcher.serverBox.coreIcons",
                    event -> applyServerBoxCoreIcons(DEFAULT_SERVER_BOX_ID));
            context.on(
                    LauncherComponentLibrary.LUA_VALUE_CHANGED_EVENT,
                    "launcher.settings.constructor.valueChanged",
                    this::applyConstructedSettingValue
            );
            context.on(
                    LauncherComponentLibrary.LUA_DIRECTORY_REQUESTED_EVENT,
                    "launcher.settings.constructor.directoryRequested",
                    this::openConstructedDirectoryChooser
            );
        } catch (Exception error) {
            Engine.getLOGGER().warn("Unable to register launcher Lua UI bridge: {}", error.getMessage());
        }
    }

    private void openInfo() {
        if (launcher.getAuth().getAuthStatus() == AuthStatus.UNAUTHORISED) {
            engine.getPanelVisibility().displayPanel(ui.panelSpecs().authToTest());
        } else {
            engine.getPanelVisibility().displayPanel(ui.panelSpecs().loggedToTest());
        }
    }

    private void openSettings() {
        launcher.getSettings().prepareForDisplay();
        if (launcher.getAuth().getAuthStatus() == AuthStatus.UNAUTHORISED) {
            engine.getPanelVisibility().displayPanel(ui.panelSpecs().authToSettings());
        } else {
            engine.getPanelVisibility().displayPanel(ui.panelSpecs().loggedToSettings());
        }
    }

    private void back() {
        if (launcher.getAuth().getAuthStatus() == AuthStatus.UNAUTHORISED) {
            engine.getPanelVisibility().displayPanel(ui.panelSpecs().backToAuth());
        } else {
            engine.getPanelVisibility().displayPanel(ui.panelSpecs().backToLogged());
        }
    }

    private void optionalMods() {
        launcher.showDialog(
                ui.dialogs().optionalModsMessage(),
                ui.dialogs().optionalModsTitle(),
                JOptionPane.WARNING_MESSAGE,
                false
        );
    }

    private void applyConstructedSettingValue(UiScriptEvent event) {
        if (event == null || !event.hasPayload() || !event.payload().istable()) {
            Engine.getLOGGER().warn("Constructor setting event ignored because payload is not a table.");
            return;
        }

        LuaValue payload = event.payload();
        String key = payload.get("key").optjstring("").trim();
        Object value = luaValue(payload.get("value"));
        if (key.isBlank() || value == null) {
            Engine.getLOGGER().warn(
                    "Constructor setting event ignored: key='{}', value='{}'",
                    key,
                    value
            );
            return;
        }

        if (event.source() != null
                && event.source().component() instanceof ConstructedCompositeComponent root) {
            root.setValue(value);
        }

        launcher.getConfig().setConfigValue(key, value);
        if ("volume".equals(key)) {
            int volume = intValue(value, (int) Math.round(launcher.getConfig().getVolume()));
            launcher.getConfig().setVolume(volume);
            launcher.getConfig().getConfig().put("volume", volume);
            launcher.getSOUND().getSoundPlayer().changeActiveVolume(volume / 100.0f - 0.15F);
        }
        Engine.getLOGGER().debug(
                "Constructor setting updated: key='{}', value='{}', source='{}'",
                key,
                value,
                event.sourceId()
        );
    }

    private void openConstructedDirectoryChooser(UiScriptEvent event) {
        if (event == null
                || event.source() == null
                || !(event.source().component() instanceof ConstructedCompositeComponent root)) {
            Engine.getLOGGER().warn("Directory constructor event has no composite source.");
            return;
        }

        String key = event.hasPayload() && event.payload().istable()
                ? event.payload().get("key").optjstring("homeDir").trim()
                : "homeDir";
        SwingUtilities.invokeLater(() -> chooseDirectory(root, key));
    }

    private void chooseDirectory(ConstructedCompositeComponent root, String settingKey) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle(engine.getLANG().getString("settings.homeDir"));

        File currentDirectory = directoryFrom(root.getValue());
        if (currentDirectory != null) {
            chooser.setCurrentDirectory(currentDirectory);
        }

        int result = chooser.showOpenDialog(engine.getFrame());
        if (result != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
            return;
        }

        String selectedPath = chooser.getSelectedFile().toPath()
                .toAbsolutePath()
                .normalize()
                .toString();
        JComponent pathNode = root.getNode(LauncherComponentLibrary.NODE_PATH);
        if (pathNode instanceof TextField textField) {
            // setText emits textChanged; the constructor route updates the root and configuration.
            textField.setText(selectedPath);
        } else {
            root.setValue(selectedPath);
            launcher.getConfig().setConfigValue(settingKey, selectedPath);
        }
        Engine.getLOGGER().info(
                "Launcher directory selected through constructor control: key='{}', path='{}'",
                settingKey,
                selectedPath
        );
    }

    private File directoryFrom(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            Path path = Path.of(String.valueOf(value)).toAbsolutePath().normalize();
            File file = path.toFile();
            return file.isDirectory() ? file : file.getParentFile();
        } catch (InvalidPathException error) {
            Engine.getLOGGER().debug("Ignoring invalid directory value '{}'.", value);
            return null;
        }
    }

    private Object luaValue(LuaValue value) {
        if (value == null || value.isnil()) {
            return null;
        }
        if (value.isboolean()) {
            return value.toboolean();
        }
        if (value.isnumber()) {
            double numeric = value.todouble();
            if (numeric == Math.rint(numeric)) {
                return (int) numeric;
            }
            return numeric;
        }
        return value.tojstring();
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    private void applyServerBoxCoreIcons(String componentId) {
        String resolvedComponentId = componentId == null || componentId.isBlank() ? DEFAULT_SERVER_BOX_ID : componentId;
        JComponent component = launcher.getGuiBuilder()
                .getComponentFactory()
                .getLuaUiScriptEngine()
                .findComponent(resolvedComponentId);
        if (!(component instanceof Combobox combobox)) {
            Engine.getLOGGER().warn("Lua requested server core icons for non-combobox component '{}'.", resolvedComponentId);
            return;
        }

        String[] values = combobox.getValues();
        if (values == null || values.length == 0) {
            combobox.setIcons(new BufferedImage[0]);
            return;
        }

        List<ServerAttributes> servers = launcher.getAuth() != null
                && launcher.getAuth().getUserDataLoader() != null
                && launcher.getAuth().getUserDataLoader().getUserServersAttributes() != null
                ? launcher.getAuth().getUserDataLoader().getUserServersAttributes()
                : Collections.emptyList();

        String iconRoot = componentStringProperty(combobox, "iconRoot", DEFAULT_ICON_ROOT);
        String iconExt = componentStringProperty(combobox, "iconExt", DEFAULT_ICON_EXT);
        String fallback = componentStringProperty(combobox, "fallback", DEFAULT_CORE_ICON);

        BufferedImage[] icons = new BufferedImage[values.length];
        for (int i = 0; i < values.length; i++) {
            ServerAttributes server = i < servers.size() ? servers.get(i) : null;
            String coreName = resolveCoreName(server, values[i], fallback);
            String iconName = mappedIconName(combobox, coreName, fallback);
            String iconPath = resolveIconPath(iconRoot, iconExt, iconName);
            icons[i] = launcher.getImageUtils().getLocalImage(iconPath);
            Engine.getLOGGER().debug("Server combobox icon resolved: index={} value='{}' coreType='{}' icon='{}' path='{}'",
                    i,
                    values[i],
                    coreName,
                    iconName,
                    iconPath);
        }

        SwingUtilities.invokeLater(() -> {
            combobox.setIcons(icons);
            combobox.repaint();
        });
        Engine.getLOGGER().debug("Applied {} server core icons from launcher Lua policy.", icons.length);
    }

    private String resolveCoreName(ServerAttributes server, String displayValue, String fallback) {
        if (server != null) {
            String coreType = ServerIdentity.safe(server.getCoreType());
            if (!coreType.isBlank()) {
                return coreType;
            }

            String versionCore = ServerIdentity.suffixAfterDash(server.getServerVersion());
            if (!versionCore.isBlank()) {
                return versionCore;
            }

            String client = ServerIdentity.safe(server.getClient());
            if (ServerIdentity.isKnownCoreType(client)) {
                return client;
            }
        }

        String displayCore = ServerIdentity.suffixAfterDash(displayValue);
        if (!displayCore.isBlank()) {
            return displayCore;
        }
        return fallback;
    }

    private String mappedIconName(JComponent component, String coreName, String fallback) {
        String normalized = ServerIdentity.normalizeCoreKey(coreName);
        String mapped = componentStringProperty(component, "map." + normalized, "");
        return mapped.isBlank() ? fallback : mapped;
    }

    private String resolveIconPath(String iconRoot, String iconExt, String iconName) {
        String safeIconName = ServerIdentity.safe(iconName).isBlank() ? DEFAULT_CORE_ICON : iconName.trim();
        if (safeIconName.contains("/") || safeIconName.contains("\\")) {
            return safeIconName;
        }
        if (!iconExt.isBlank() && safeIconName.endsWith(iconExt)) {
            return iconRoot + safeIconName;
        }
        return iconRoot + safeIconName + iconExt;
    }

    private String componentStringProperty(JComponent component, String key, String fallback) {
        if (component == null || key == null || key.isBlank()) {
            return fallback;
        }
        Object value = component.getClientProperty(CORE_ICON_PROPERTY_PREFIX + key);
        String resolved = value == null ? "" : String.valueOf(value).trim();
        return resolved.isBlank() ? fallback : resolved;
    }
}
