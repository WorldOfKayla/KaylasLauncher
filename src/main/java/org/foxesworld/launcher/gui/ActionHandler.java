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
import org.foxesworld.launcher.gui.command.DynamicCommandRegistry;
import org.foxesworld.notification.Notification;
import org.foxesworld.engine.gui.componentAccessor.Component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Класс для обработки действий, реализующий динамическую регистрацию команд.
 */
public class ActionHandler extends org.foxesworld.engine.gui.ActionHandler implements DynamicCommandRegistry {

    protected Launcher launcher;
    private Core core;
    protected ServerAttributes currentServer;

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

    private final Map<String, Consumer<ActionEvent>> commandRegistry = new HashMap<>();

    public ActionHandler(Launcher launcher) {
        super(launcher.getGuiBuilder(), "mainFrame", List.of(TextField.class, Checkbox.class, JProgressBar.class, PassField.class, Button.class, MultiButton.class, DropBox.class));
        this.launcher = launcher;
        this.engine = launcher.getEngine();
        registerCommands();
    }

    @Override
    public void registerCommand(String key, Consumer<ActionEvent> command) {
        Launcher.LOGGER.info("  - Registered command for {} action", key);
        commandRegistry.put(key, command);
    }

    @Override
    public void unregisterCommand(String key) {
        commandRegistry.remove(key);
    }

    @Override
    public void executeCommand(String key, ActionEvent event) {
        Consumer<ActionEvent> handler = commandRegistry.get(key);
        if (handler != null) {
            handler.accept(event);
        } else {
            Engine.getLOGGER().warn("No command registered for: " + key);
        }
    }

    /**
     * Метод для динамической регистрации команд.
     * Здесь для каждой команды используется метод registerCommand из интерфейса DynamicCommandRegistry.
     */
    private void registerCommands() {
        // Команда для отправки формы аутентификации
        registerCommand("authForm>submit", e -> {
            authSubmit.setEnabled(false);
            this.launcher.getExecutorServiceProvider().submitTask(() -> {
                this.launcher.getAuth().formAuth(authSubmit);
                if (this.launcher.getAuth().isAuthorised()) {
                    engine.getFrame().getRootPanel().removeAll();
                    engine.getPanelVisibility().displayPanel("authForm->false");
                    this.launcher.init();
                } else {
                    login.resetText();
                    password.resetText();
                }
            }, "auth");
        });

        // Тестовая команда для пользовательской информации
        registerCommand("userinfo>submit", e ->
                Engine.getLOGGER().warn("TEST"));

        // Переключение видимости загрузчика
        registerCommand("smallButton", e ->
                this.launcher.getLoadingManager().toggleVisibility());

        // Открытие папки игры
        registerCommand("gameDir-small", e ->
                this.launcher.getSettings().openGameFolder());

        // Отмена загрузки
        registerCommand("cancelDownload-small", e ->
                core.getFileLoader().cancel());

        // Применение настроек
        registerCommand("applySettings", e ->
                this.launcher.getSettings().applySettings("settingsFields"));

        // Выход из учётной записи
        registerCommand("logOut", e -> {
            this.launcher.getSOUND().playSound("other", "loggedOut");
            this.launcher.getGuiBuilder().getNotification().show(
                    Notification.Type.SUCCESS,
                    Notification.Location.BOTTOM_LEFT,
                    this.launcher.getUser().getLogin() + this.launcher.getLANG().getString("auth.loggedOut"));
            this.launcher.getAuth().logOut();
        });

        // Отображение информационной панели
        registerCommand("info-small", e -> {
            if (!launcher.getAuth().isAuthorised()) {
                engine.getPanelVisibility().displayPanel("authForm->false|newsForm->false|test->true");
            } else {
                engine.getPanelVisibility().displayPanel("loggedForm->false|newsForm->false|test->true");
            }
        });

        // Отображение панели настроек
        registerCommand("settings-small", e -> {
            if (!launcher.getAuth().isAuthorised()) {
                engine.getPanelVisibility().displayPanel("authForm->false|newsForm->false|settings->true");
            } else {
                engine.getPanelVisibility().displayPanel("loggedForm->false|newsForm->false|settings->true");
            }
        });

        // Отмена загрузки с дополнительными действиями
        registerCommand("loadCancel-small", e -> {
            this.core.getFileLoader().cancel();
            this.launcher.getLoadingManager().toggleVisibility();
            this.getComponent("toGame").setEnabled(false);
            this.getComponent("logOut").setEnabled(false);
        });

        // Возврат к предыдущему состоянию
        registerCommand("back", e -> {
            if (!launcher.getAuth().isAuthorised()) {
                engine.getPanelVisibility().displayPanel("authForm->true|newsForm->true|settings->false");
            } else {
                engine.getPanelVisibility().displayPanel("loggedForm->true|newsForm->true|settings->false");
            }
        });

        // Запуск игры
        registerCommand("toGame", e -> {
            this.launcher.getSOUND().playSound("other", "start");
            this.currentServer = launcher.getAuth().getUserDataLoader().getUserServersAttributes().get(serverBox.getSelectedIndex());
            Engine.getLOGGER().info("Launching " + this.currentServer.getServerName());
            this.launcher.getConfig().setConfigValue("selectedServer", serverBox.getSelectedIndex());
            this.launcher.getConfig().writeCurrentConfig();
            this.core = new Core(this, forceUpdate.isSelected());
        });

        // Оповещение о недоступности опциональных модов
        registerCommand("optionalMods", e ->
                launcher.showDialog("Optional mods will arrive later ;)", "Work In Progress", JOptionPane.WARNING_MESSAGE, false));

        // Экспериментальная команда: анимация и переключение пользовательской панели
        registerCommand("userPane", e -> {
            this.getEngine().getExecutorServiceProvider().submitTask(() -> {
                SwingUtilities.invokeLater(() -> {
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
                        String iconPath = isVisible ? "assets/ui/icons/menu.svg" : "assets/ui/icons/back.svg";
                        ImageIcon icon = this.launcher.getIconUtils().getVectorIcon(iconPath, 20, 24);

                        // Получаем компонент, вызвавший команду
                        JComponent pressedComponent = this.getComponent("userPane");
                        if (pressedComponent instanceof Button button) {
                            button.setIcon(icon);
                        } else {
                            Engine.getLOGGER().warn("Pressed component is not a Button.");
                        }

                        // Вычисление позиций для анимации
                        int startX = isVisible ? panel.getX() : -panel.getWidth();
                        int endX = isVisible ? -panel.getWidth() : 0;

                        // Настройка компонента, вызвавшего команду
                        panelParent.setComponentZOrder(pressedComponent, 0);
                        pressedComponent.setBounds(10, 40, 30, 30);

                        // Отображение панелей до начала анимации
                        if (!isVisible) {
                            panel.setVisible(true);
                        } else {
                            JPanel userActionsPanel = this.getEngine().getGuiBuilder().getPanelsMap().get("userActions");
                            if (userActionsPanel != null) {
                                userActionsPanel.setVisible(true);
                            }
                        }
                        panelParent.revalidate();
                        panelParent.repaint();

                        // Остановка предыдущей анимации, если она активна
                        Object currentAnimation = panel.getClientProperty("currentAnimation");
                        if (currentAnimation instanceof Timer oldTimer && oldTimer.isRunning()) {
                            oldTimer.stop();
                        }

                        // Создание таймера с задержкой 15 мс для плавной анимации
                        int timerDelay = 1;
                        Timer timer = new Timer(timerDelay, null);
                        panel.putClientProperty("currentAnimation", timer);

                        final long startTime = System.currentTimeMillis();
                        final int animationDuration = 250; // длительность анимации в мс

                        timer.addActionListener(animationEvent -> {
                            try {
                                long elapsed = System.currentTimeMillis() - startTime;
                                float progress = Math.min(1f, (float) elapsed / animationDuration);
                                // Плавное затухание (ease-out)
                                float easedProgress = 1 - (float) Math.pow(1 - progress, 2);
                                int newX = (int) (startX + (endX - startX) * easedProgress);
                                panel.setLocation(newX, panel.getY());
                                panelParent.setComponentZOrder(panel, 1);
                                panelParent.repaint();

                                if (progress >= 1f) {
                                    timer.stop();
                                    panel.putClientProperty("currentAnimation", null);

                                    // Переключение видимости панелей после завершения анимации
                                    if (isVisible) {
                                        panel.setVisible(false);
                                    } else {
                                        JPanel userActionsPanel = this.getEngine().getGuiBuilder().getPanelsMap().get("userActions");
                                        if (userActionsPanel != null) {
                                            userActionsPanel.setVisible(false);
                                        }
                                    }
                                    panelParent.revalidate();
                                }
                            } catch (Exception ex) {
                                Engine.getLOGGER().error("Error during animation", ex);
                                timer.stop();
                            }
                        });
                        timer.start();
                    } catch (Exception ex) {
                        Engine.getLOGGER().error("Exception in userPane command", ex);
                    }
                });
            }, "userPaneToggle");
        });

        // Завершение работы приложения
        registerCommand("closeButton", e -> {
            this.launcher.getFrame().setVisible(false);
            this.launcher.getExecutorServiceProvider().shutdown();
            this.launcher.getSOUND().getSoundPlayer().stopAllSounds(() -> {
                this.launcher.getExecutorServiceProvider().shutdown();
                System.exit(0);
            });
        });

        // Свернуть окно
        registerCommand("hideButton", e ->
                engine.getFrame().setExtendedState(Frame.ICONIFIED));
    }

    /**
     * Основной метод обработки событий.
     * Делегирует выполнение команды методу executeCommand.
     *
     * @param e событие, содержащее информацию о нажатой команде.
     */
    @Override
    public void handleAction(ActionEvent e) {
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

    public Map<String, Consumer<ActionEvent>> getCommandRegistry() {
        return commandRegistry;
    }
}
