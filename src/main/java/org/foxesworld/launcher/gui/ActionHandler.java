package org.foxesworld.launcher.gui;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.button.Button;
import org.foxesworld.engine.gui.components.checkbox.Checkbox;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.multiButton.MultiButton;
import org.foxesworld.engine.gui.components.passfield.PassField;
import org.foxesworld.engine.gui.components.textfield.TextField;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.launcher.Core;
import org.foxesworld.launcher.auth.AuthStatus;
import org.foxesworld.notification.Notification;
import org.foxesworld.engine.gui.componentAccessor.Component;

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
 * Thread-safe and improved ActionHandler.
 * Основные изменения:
 * - потокобезопасный реестр команд (ConcurrentHashMap)
 * - защищённый запуск обработчиков команд в EDT (SwingUtilities.invokeLater)
 * - избегание долгих/блокирующих операций в EDT (вынесены в executor)
 * - volatile для полей, которые могут меняться из разных потоков
 * - getter для реестра возвращает неизменяемое представление
 */
public class ActionHandler extends org.foxesworld.engine.gui.ActionHandler {

    protected final Launcher launcher;
    private volatile Core core;
    protected volatile ServerAttributes currentServer;

    @Component
    private TextField login;

    @Component
    private PassField password;

    @Component("authForm>submit")
    private Button authSubmit;

    @Component
    private Checkbox forceUpdate;

    @Component
    private DropBox serverBox;

    // Thread-safe map for commands
    private final ConcurrentMap<String, Consumer<ActionEvent>> commandRegistry = new ConcurrentHashMap<>();

    public ActionHandler(Launcher launcher) {
        super(Objects.requireNonNull(launcher, "launcher").getGuiBuilder(), "mainFrame",
                List.of(TextField.class, Checkbox.class, JProgressBar.class, PassField.class, Button.class, MultiButton.class, DropBox.class));
        this.launcher = launcher;
        this.engine = launcher.getEngine();
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

        // Ensure handlers execute on EDT. Handlers should offload heavy work to the executor.
        if (SwingUtilities.isEventDispatchThread()) {
            runHandler.run();
        } else {
            SwingUtilities.invokeLater(runHandler);
        }
    }

    /**
     * Регистрация команд. Внутри команд — минимальная работа на EDT, тяжёлые операции выносятся в executor.
     */
    private void registerCommands() {
        // Авторизация: выключаем кнопку на EDT, выполняем авторизацию в executor, затем обновляем UI в EDT
        registerCommand("authForm>submit", e -> {
            // disable UI immediately (we are running handlers on EDT via executeCommand)
            if (authSubmit != null) authSubmit.setEnabled(false);

            launcher.getExecutorServiceProvider().submitTask(() -> {
                try {
                    // Выполнение авторизации (может взаимодействовать с UI только через безопасные колбеки)
                    launcher.getAuth().formAuth(authSubmit, () -> {
                        launcher.setInit(false);
                        launcher.LambdaInit();
                    });

                    AuthStatus status = launcher.getAuth().getAuthStatus();

                    SwingUtilities.invokeLater(() -> {
                        if (authSubmit != null) authSubmit.setEnabled(true);

                        if (status == AuthStatus.AUTHORISED) {
                            try {
                                engine.getFrame().getRootPanel().removeAll();
                                engine.getPanelVisibility().displayPanel("authForm->false");
                                // launcher.init() может создавать UI компоненты — вызываем на EDT
                                launcher.init();
                            } catch (Exception ex) {
                                Engine.getLOGGER().error("Error while switching panels after auth", ex);
                            }
                        } else {
                            if (login != null) login.resetText();
                            if (password != null) password.resetText();
                        }
                    });
                } catch (Throwable ex) {
                    Engine.getLOGGER().error("Exception during auth task", ex);
                    SwingUtilities.invokeLater(() -> {
                        if (authSubmit != null) authSubmit.setEnabled(true);
                    });
                }
            }, "auth");
        });

        // Тестовая команда
        registerCommand("userinfo>submit", e -> Engine.getLOGGER().warn("TEST"));

        registerCommand("smallButton", e -> this.launcher.getLoadingManager().toggleVisibility());
        registerCommand("gameDir-small", e -> this.launcher.getSettings().openGameFolder());
        registerCommand("cancelDownload-small", e -> {
            Core localCore = this.core; // volatile read
            if (localCore != null) localCore.getFileLoader().cancel();
        });
        registerCommand("applySettings", e -> this.launcher.getSettings().applySettings("settingsFields"));

        registerCommand("logOut", e -> {
            this.launcher.getSOUND().playSound("other", "loggedOut");
            this.launcher.getGuiBuilder().getNotification().show(
                    Notification.Type.SUCCESS,
                    Notification.Location.BOTTOM_LEFT,
                    this.launcher.getUser().getLogin() + this.launcher.getLANG().getString("auth.loggedOut"));
            this.launcher.getAuth().logOut();
        });

        registerCommand("info-small", e -> {
            if (launcher.getAuth().getAuthStatus() == AuthStatus.UNAUTHORISED) {
                engine.getPanelVisibility().displayPanel("authForm->false|newsForm->false|test->true");
            } else {
                engine.getPanelVisibility().displayPanel("loggedForm->false|newsForm->false|test->true");
            }
        });

        registerCommand("settings-small", e -> {
            if (launcher.getAuth().getAuthStatus() == AuthStatus.UNAUTHORISED) {
                engine.getPanelVisibility().displayPanel("authForm->false|newsForm->false|settings->true");
            } else {
                engine.getPanelVisibility().displayPanel("loggedForm->false|newsForm->false|settings->true");
            }
        });

        registerCommand("loadCancel-small", e -> {
            this.core.getFileLoader().cancel();
            this.launcher.getLoadingManager().toggleVisibility();
            this.getComponent("toGame").setEnabled(false);
            this.getComponent("logOut").setEnabled(false);
        });

        registerCommand("back", e -> {
            if (launcher.getAuth().getAuthStatus() == AuthStatus.UNAUTHORISED) {
                engine.getPanelVisibility().displayPanel("authForm->true|newsForm->true|settings->false");
            } else {
                engine.getPanelVisibility().displayPanel("loggedForm->true|newsForm->true|settings->false");
            }
        });

        // Запуск игры — подготовка и создание Core в background-потоке
        registerCommand("toGame", e -> {
            this.launcher.getSOUND().playSound("other", "start");

            // Считываем выбранный сервер и сохраняем конфиг на EDT
            int selectedIndex = 0;
            if (serverBox != null) {
                selectedIndex = serverBox.getSelectedIndex();
            }

            this.launcher.getConfig().setConfigValue("selectedServer", selectedIndex);
            this.launcher.getConfig().writeCurrentConfig();

            // Получаем объект сервера (читаем volatile-safe)
            ServerAttributes serverAttr = launcher.getAuth().getUserDataLoader().getUserServersAttributes().get(selectedIndex);
            this.currentServer = serverAttr;

            // Создание Core — долгое действие, делаем в executor
            launcher.getExecutorServiceProvider().submitTask(() -> {
                try {
                    Core created = new Core(this, forceUpdate != null && forceUpdate.isSelected());
                    this.core = created; // volatile write
                } catch (Throwable ex) {
                    Engine.getLOGGER().error("Failed to create Core", ex);
                    SwingUtilities.invokeLater(() -> launcher.showDialog("Failed to start game: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, false));
                }
            }, "createCore");
        });

        registerCommand("optionalMods", e -> launcher.showDialog("Optional mods will arrive later ;)", "Work In Progress", JOptionPane.WARNING_MESSAGE, false));

        // userPane — анимация и UI-манипуляции выполняются на EDT (используем встроенные Timer'ы)
        registerCommand("userPane", e -> {
            // Ensure we're on EDT
            Runnable uiTask = () -> {
                try {
                    JPanel panel = this.launcher.getUser().getPanel();
                    if (panel == null) {
                        Engine.getLOGGER().error("User panel is null.");
                        return;
                    }

                    Container panelParent = panel.getParent();
                    if (panelParent == null) {
                        Engine.getLOGGER().error("User panel parent is null.");
                        return;
                    }

                    boolean isVisible = panel.isVisible();
                    String oldIconPath = isVisible ? "assets/ui/icons/back.svg" : "assets/ui/icons/menu.svg";
                    String newIconPath = isVisible ? "assets/ui/icons/menu.svg" : "assets/ui/icons/back.svg";

                    ImageIcon oldIcon = this.launcher.getIconUtils().getVectorIcon(oldIconPath, 20, 24);
                    ImageIcon newIcon = this.launcher.getIconUtils().getVectorIcon(newIconPath, 20, 24);

                    JComponent pressedComponent = this.getComponent("userPane");
                    if (!(pressedComponent instanceof Button button)) {
                        Engine.getLOGGER().warn("Pressed component is not a Button.");
                        return;
                    }

                    final int iconAnimationDuration = 250; // ms
                    final long iconStart = System.currentTimeMillis();
                    Timer iconTimer = new Timer(15, null);
                    iconTimer.addActionListener(event -> {
                        long elapsed = System.currentTimeMillis() - iconStart;
                        float progress = Math.min(1f, (float) elapsed / iconAnimationDuration);
                        float easedProgress = 1 - (float) Math.pow(1 - progress, 2);

                        BlendedImageIcon blendedIcon = new BlendedImageIcon(oldIcon.getImage(), newIcon.getImage(), easedProgress);
                        button.setIcon(blendedIcon);
                        button.repaint();

                        if (progress >= 1f) {
                            iconTimer.stop();
                            button.setIcon(newIcon);
                        }
                    });
                    iconTimer.start();

                    int startX = isVisible ? panel.getX() : -panel.getWidth();
                    int endX = isVisible ? -panel.getWidth() : 0;

                    panelParent.setComponentZOrder(pressedComponent, 0);
                    pressedComponent.setBounds(10, 40, 30, 30);

                    if (!isVisible) {
                        panel.setVisible(true);
                    } else {
                        JPanel userActionsPanel = this.getEngine().getGuiBuilder().getPanelsMap().get("userActions");
                        if (userActionsPanel != null) userActionsPanel.setVisible(true);
                    }
                    panelParent.revalidate();
                    panelParent.repaint();

                    Object currentAnimation = panel.getClientProperty("currentAnimation");
                    if (currentAnimation instanceof Timer oldTimer && oldTimer.isRunning()) {
                        oldTimer.stop();
                    }

                    final int panelAnimationDuration = 220;
                    final long panelStartTime = System.currentTimeMillis();

                    Timer panelTimer = new Timer(0, null);
                    panel.putClientProperty("currentAnimation", panelTimer);
                    panelTimer.addActionListener(animationEvent -> {
                        try {
                            long elapsed = System.currentTimeMillis() - panelStartTime;
                            float progress = Math.min(1f, (float) elapsed / panelAnimationDuration);
                            float easedProgress = 1 - (float) Math.pow(1 - progress, 2);
                            int newX = (int) (startX + (endX - startX) * easedProgress);

                            panel.setLocation(newX, panel.getY());
                            panelParent.setComponentZOrder(panel, 1);
                            panelParent.repaint();

                            if (progress >= 1f) {
                                panelTimer.stop();
                                panel.putClientProperty("currentAnimation", null);

                                if (isVisible) {
                                    panel.setVisible(false);
                                } else {
                                    JPanel userActionsPanel = this.getEngine().getGuiBuilder().getPanelsMap().get("userActions");
                                    if (userActionsPanel != null) userActionsPanel.setVisible(false);
                                }
                                panelParent.revalidate();
                            }
                        } catch (Exception ex) {
                            Engine.getLOGGER().error("Error during animation", ex);
                            panelTimer.stop();
                        }
                    });
                    panelTimer.start();

                } catch (Exception ex) {
                    Engine.getLOGGER().error("Exception in userPane command", ex);
                }
            };

            if (SwingUtilities.isEventDispatchThread()) uiTask.run();
            else SwingUtilities.invokeLater(uiTask);
        });

        registerCommand("closeButton", e -> {
            // Плавный shutdown: разрешаем звуку остановиться, затем завершаем executor и выходим
            try {
                this.launcher.getExecutorServiceProvider().shutdown();
            } catch (Exception ex) {
                Engine.getLOGGER().warn("Error shutting down executor provider", ex);
            }
            try {
                // Вызов System.exit удобнее делать в EDT
                SwingUtilities.invokeLater(() -> System.exit(0));
            } catch (Exception ignored) {
                System.exit(0);
            }
        });

        registerCommand("hideButton", e -> engine.getFrame().setExtendedState(Frame.ICONIFIED));
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

    /**
     * Возвращаем неизменяемое отображение реестра команд, чтобы внешние клиенты не могли модифицировать его.
     */
    public Map<String, Consumer<ActionEvent>> getCommandRegistry() {
        return Collections.unmodifiableMap(commandRegistry);
    }
}
