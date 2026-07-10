package org.takesome.launcher.gui;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.gui.components.button.Button;
import org.takesome.kaylasEngine.gui.components.checkbox.Checkbox;
import org.takesome.kaylasEngine.gui.components.combobox.Combobox;
import org.takesome.kaylasEngine.gui.components.multiButton.MultiButton;
import org.takesome.kaylasEngine.gui.components.passfield.PassField;
import org.takesome.kaylasEngine.gui.components.progressBar.ProgressBar;
import org.takesome.kaylasEngine.gui.components.textfield.TextField;
import org.takesome.kaylasEngine.server.ServerAttributes;
import org.takesome.launcher.Core;
import org.takesome.launcher.auth.AuthStatus;
import org.foxesworld.notification.Notification;
import org.takesome.kaylasEngine.gui.componentAccessor.Component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * Launcher command dispatcher.
 *
 * <p>Commands are registered in a thread-safe map and are executed on the EDT. Expensive work must
 * be explicitly offloaded to the executor. Authentication refreshes already-built UI state instead
 * of rebuilding the whole launcher GUI.</p>
 */
public class ActionHandler extends org.takesome.kaylasEngine.gui.ActionHandler {

    private static final LauncherUiProvider UI_PROVIDER = LauncherUiProvider.load();

    protected final Launcher launcher;
    private volatile Core core;
    protected volatile ServerAttributes currentServer;
    private final LauncherUiProvider ui;
    private final UserPaneAnimator userPaneAnimator;
    private final LauncherLuaUiBridge luaUiBridge;

    @Component
    private TextField login;

    @Component
    private PassField password;


    @Component
    private Checkbox forceUpdate;

    @Component
    private Combobox serverBox;

    private final ConcurrentMap<String, Consumer<ActionEvent>> commandRegistry = new ConcurrentHashMap<>();

    public ActionHandler(Launcher launcher) {
        super(Objects.requireNonNull(launcher, "launcher").getGuiBuilder(), UI_PROVIDER.scopes().mainFrame(),
                List.of(TextField.class, Checkbox.class, ProgressBar.class, PassField.class, Button.class, MultiButton.class, Combobox.class));
        this.launcher = launcher;
        this.engine = launcher.getEngine();
        this.ui = UI_PROVIDER;
        this.ui.validate();
        this.userPaneAnimator = new UserPaneAnimator(launcher, ui, () -> this.getComponent(ui.components().userPane()));
        this.luaUiBridge = new LauncherLuaUiBridge(launcher, ui, userPaneAnimator);
        this.luaUiBridge.register();
        registerCommands();
    }

    @Override
    public void registerCommand(String key, Consumer<ActionEvent> command) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(command);
        Launcher.LOGGER.info("  - Registered command for {} action", key);
        Consumer<ActionEvent> prev = commandRegistry.put(key, command);
        if (prev != null) {
            Launcher.LOGGER.warn("Command for key '{}' was overwritten", key);
        }
    }

    @Override
    public void unregisterCommand(String key) {
        if (key == null) return;
        commandRegistry.remove(key);
    }

    @Override
    public void executeCommand(String key, ActionEvent event) {
        Consumer<ActionEvent> handler = commandRegistry.get(key);
        if (handler == null) {
            Engine.getLOGGER().warn("No command registered for: {}", key);
            return;
        }

        Runnable runHandler = () -> {
            try {
                handler.accept(event);
            } catch (Throwable ex) {
                Engine.getLOGGER().error("Exception while executing command '{}':", key, ex);
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            runHandler.run();
        } else {
            SwingUtilities.invokeLater(runHandler);
        }
    }

    /** Registers launcher commands and keeps EDT work bounded. */
    private void registerCommands() {
        registerCommand(ui.components().authSubmit(), e -> {
            Button submitButton = authSubmitButton();
            if (submitButton != null) {
                submitButton.setEnabled(false);
            }

            launcher.getExecutorServiceProvider().submitTask(() -> {
                try {
                    launcher.getAuth().formAuth(submitButton, () -> { });
                    AuthStatus status = launcher.getAuth().getAuthStatus();

                    SwingUtilities.invokeLater(() -> {
                        if (submitButton != null) {
                            submitButton.setEnabled(true);
                        }

                        if (status == AuthStatus.AUTHORISED) {
                            refreshAuthenticatedUi();
                        } else {
                            if (login != null) login.resetText();
                            if (password != null) password.resetText();
                        }
                    });
                } catch (Throwable ex) {
                    Engine.getLOGGER().error("Exception during auth task", ex);
                    SwingUtilities.invokeLater(() -> {
                        if (submitButton != null) {
                            submitButton.setEnabled(true);
                        }
                    });
                }
            }, ui.tasks().auth());
        });

        registerCommand(ui.components().userinfoSubmit(), e -> Engine.getLOGGER().warn("TEST"));

        registerCommand(ui.components().smallButton(), this::scriptOwnedUiAction);
        registerCommand(ui.components().gameDirSmall(), this::scriptOwnedUiAction);
        registerCommand(ui.components().cancelDownloadSmall(), e -> {
            Core localCore = this.core;
            if (localCore != null) localCore.getFileLoader().cancel();
        });
        registerCommand(ui.components().applySettings(), this::scriptOwnedUiAction);

        registerCommand(ui.components().logOut(), e -> {
            this.launcher.getSOUND().playSound(ui.sounds().other(), ui.sounds().loggedOut());
            this.launcher.getGuiBuilder().getNotification().show(
                    Notification.Type.SUCCESS,
                    Notification.Location.BOTTOM_LEFT,
                    this.launcher.getUser().getLogin() + this.launcher.getLANG().getString(ui.localeKeys().authLoggedOut()));
            this.launcher.getAuth().logOut();
        });

        registerCommand(ui.components().infoSmall(), this::scriptOwnedUiAction);

        registerCommand(ui.components().settingsSmall(), this::scriptOwnedUiAction);

        registerCommand(ui.components().loadCancelSmall(), e -> {
            this.core.getFileLoader().cancel();
            this.launcher.getLoadingManager().toggleVisibility();
            this.getComponent(ui.components().toGame()).setEnabled(false);
            this.getComponent(ui.components().logOut()).setEnabled(false);
        });

        registerCommand(ui.components().back(), this::scriptOwnedUiAction);

        registerCommand(ui.components().toGame(), e -> {
            this.launcher.getSOUND().playSound(ui.sounds().other(), ui.sounds().start());

            int selectedIndex = 0;
            if (serverBox != null) {
                selectedIndex = serverBox.getSelectedIndex();
            }

            this.launcher.getConfig().setConfigValue(ui.configKeys().selectedServer(), selectedIndex);
            this.launcher.getConfig().writeCurrentConfig();

            List<ServerAttributes> servers = launcher.getAuth().getUserDataLoader().getUserServersAttributes();
            if (servers == null || servers.isEmpty()) {
                Engine.getLOGGER().warn("Cannot start game: user has no available servers.");
                launcher.showDialog(ui.dialogs().noServersMessage(), ui.dialogs().errorTitle(), JOptionPane.WARNING_MESSAGE, false);
                return;
            }
            if (selectedIndex < 0 || selectedIndex >= servers.size()) {
                Engine.getLOGGER().warn("Selected server index {} is out of bounds for {} servers. Falling back to 0.", selectedIndex, servers.size());
                selectedIndex = 0;
                this.launcher.getConfig().setConfigValue(ui.configKeys().selectedServer(), selectedIndex);
                this.launcher.getConfig().writeCurrentConfig();
            }

            ServerAttributes serverAttr = servers.get(selectedIndex);
            this.currentServer = serverAttr;
            this.launcher.getDiscordPresence().showPreparing(
                    serverAttr,
                    this.launcher.getUser().getLogin()
            );

            launcher.getExecutorServiceProvider().submitTask(() -> {
                try {
                    Core created = new Core(this, forceUpdate != null && forceUpdate.isSelected());
                    this.core = created;
                } catch (Throwable ex) {
                    Engine.getLOGGER().error("Failed to create Core", ex);
                    SwingUtilities.invokeLater(() -> launcher.showDialog(failedToStartGameMessage(ex), ui.dialogs().errorTitle(), JOptionPane.ERROR_MESSAGE, false));
                }
            }, ui.tasks().createCore());
        });

        registerCommand(ui.components().optionalMods(), this::scriptOwnedUiAction);

        registerCommand(ui.components().userPane(), this::scriptOwnedUiAction);

        registerCommand(ui.components().closeButton(), e -> {
            try {
                this.launcher.getExecutorServiceProvider().shutdown();
            } catch (Exception ex) {
                Engine.getLOGGER().warn("Error shutting down executor provider", ex);
            }
            try {
                SwingUtilities.invokeLater(() -> System.exit(0));
            } catch (Exception ignored) {
                System.exit(0);
            }
        });

        registerCommand(ui.components().hideButton(), e -> engine.getFrame().setExtendedState(Frame.ICONIFIED));
    }

    /**
     * Switches from the authentication view to the already-built authorised user UI.
     *
     * <p>This deliberately avoids {@code launcher.init()}, which rebuilds the whole GUI and causes
     * visible stalls after a successful login.</p>
     */
    private void refreshAuthenticatedUi() {
        try {
            engine.getPanelVisibility().displayPanel(ui.panelSpecs().hideAuth());
            if (launcher.getUser() != null) {
                launcher.getUser().initializeUser();
            } else {
                Engine.getLOGGER().warn("Cannot refresh authenticated UI because launcher user is not initialized.");
            }
        } catch (Exception ex) {
            Engine.getLOGGER().error("Error while switching panels after auth", ex);
        }
    }

    private String failedToStartGameMessage(Throwable error) {
        return launcher.getLANG().getStringWithKey(
                ui.dialogs().failedToStartGame(),
                new String[]{"error"},
                new String[]{error == null ? "Unknown error" : String.valueOf(error.getMessage())}
        );
    }

    private Button authSubmitButton() {
        java.awt.Component component = getComponent(ui.components().authSubmit());
        return component instanceof Button button ? button : null;
    }

    private void scriptOwnedUiAction(ActionEvent event) {
        Engine.getLOGGER().debug("UI command '{}' is handled by Lua script bridge.", event == null ? null : event.getActionCommand());
    }

    @Override
    public void handleAction(ActionEvent e) {
        if (e == null) return;
        executeCommand(e.getActionCommand(), e);
    }

    @Override
    public Engine getEngine() {
        return engine;
    }

    @Override
    public ServerAttributes getCurrentServer() {
        return currentServer;
    }

    public Launcher getLauncher() {
        return launcher;
    }

    /** Returns a read-only view of the registered launcher commands. */
    public Map<String, Consumer<ActionEvent>> getCommandRegistry() {
        return Collections.unmodifiableMap(commandRegistry);
    }
}
