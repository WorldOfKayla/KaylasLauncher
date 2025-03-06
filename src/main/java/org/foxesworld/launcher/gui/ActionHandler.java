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
import org.foxesworld.notification.Notification;
import org.foxesworld.test.command.CommandRegistrable;
import org.foxesworld.test.command.CommandRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class ActionHandler extends org.foxesworld.engine.gui.ActionHandler implements CommandRegistrable {

    protected final Launcher launcher;
    private Core core;
    protected ServerAttributes currentServer;

    public ActionHandler(Launcher launcher) {
        super(launcher.getGuiBuilder(), "mainFrame", List.of(
                TextField.class, Checkbox.class, JProgressBar.class,
                PassField.class, Button.class, MultiButton.class, DropBox.class));
        this.launcher = launcher;
        this.engine = launcher.getEngine();
        registerCommands();
    }

    /**
     * При возникновении события делегируем выполнение команде из реестра.
     */
    @Override
    public void handleAction(ActionEvent e) {
        // Преобразуем команду: "authForm>submit" -> "authForm.submit"
        String commandKey = e.getActionCommand().replace(">", ".");
        CommandRegistry.execute(commandKey, e);
    }

    /**
     * Регистрирует команды, обрабатываемые этим классом.
     */
    @Override
    public void registerCommands() {
        // Команды формы авторизации
        CommandRegistry.register("authForm.submit", this::handleAuthSubmit);
        CommandRegistry.register("userinfo.submit", this::handleUserInfoSubmit);

        // Остальные команды
        CommandRegistry.register("smallButton", e -> launcher.getLoadingManager().toggleVisibility());
        CommandRegistry.register("gameDir-small", e -> launcher.getSettings().openGameFolder());
        CommandRegistry.register("cancelDownload-small", e -> {
            if (core != null) {
                core.getFileLoader().cancel();
            }
        });
        CommandRegistry.register("applySettings", e -> launcher.getSettings().applySettings("settingsFields"));
        CommandRegistry.register("logOut", this::handleLogOut);
        CommandRegistry.register("info-small", this::handleInfoSmall);
        CommandRegistry.register("settings-small", this::handleSettingsSmall);
        CommandRegistry.register("loadCancel-small", this::handleLoadCancelSmall);
        CommandRegistry.register("back", this::handleBack);
        CommandRegistry.register("toGame", this::handleToGame);
        CommandRegistry.register("optionalMods", e -> launcher.showDialog(
                "Optional mods will arrive later ;)",
                "Work In Progress",
                JOptionPane.WARNING_MESSAGE,
                false));
        CommandRegistry.register("userPane", this::handleUserPane);
        CommandRegistry.register("closeButton", this::handleCloseButton);
        CommandRegistry.register("hideButton", e -> engine.getFrame().setExtendedState(Frame.ICONIFIED));
    }

    private void handleAuthSubmit(ActionEvent e) {
        getComponent("authForm>submit").setEnabled(false);
        launcher.getExecutorServiceProvider().submitTask(() -> {
            launcher.getAuth().formAuth(getComponent("authForm>submit"));
            if (launcher.getAuth().isAuthorised()) {
                engine.getFrame().getRootPanel().removeAll();
                engine.getPanelVisibility().displayPanel("authForm->false");
                launcher.init();
            } else {
                ((TextField) getComponent("login")).resetText();
                ((PassField) getComponent("password")).resetText();
            }
        }, "auth");
    }

    private void handleUserInfoSubmit(ActionEvent e) {
        Engine.getLOGGER().warn("TEST");
    }

    private void handleLogOut(ActionEvent e) {
        launcher.getSOUND().playSound("other", "loggedOut");
        launcher.getGuiBuilder().getNotification().show(
                Notification.Type.SUCCESS,
                Notification.Location.BOTTOM_LEFT,
                launcher.getUser().getLogin() + launcher.getLANG().getString("auth.loggedOut"));
        launcher.getAuth().logOut();
    }

    private void handleInfoSmall(ActionEvent e) {
        String panelCommand = launcher.getAuth().isAuthorised()
                ? "loggedForm->false|newsForm->false|test->true"
                : "authForm->false|newsForm->false|test->true";
        engine.getPanelVisibility().displayPanel(panelCommand);
    }

    private void handleSettingsSmall(ActionEvent e) {
        String panelCommand = launcher.getAuth().isAuthorised()
                ? "loggedForm->false|newsForm->false|settings->true"
                : "authForm->false|newsForm->false|settings->true";
        engine.getPanelVisibility().displayPanel(panelCommand);
    }

    private void handleLoadCancelSmall(ActionEvent e) {
        if (core != null) {
            core.getFileLoader().cancel();
        }
        launcher.getLoadingManager().toggleVisibility();
        getComponent("toGame").setEnabled(false);
        getComponent("logOut").setEnabled(false);
    }

    private void handleBack(ActionEvent e) {
        String panelCommand = launcher.getAuth().isAuthorised()
                ? "loggedForm->true|newsForm->true|settings->false"
                : "authForm->true|newsForm->true|settings->false";
        engine.getPanelVisibility().displayPanel(panelCommand);
    }

    private void handleToGame(ActionEvent e) {
        launcher.getSOUND().playSound("other", "start");
        DropBox dropBox = (DropBox) getComponent("serverBox");
        Checkbox forceUpdate = (Checkbox) getComponent("forceUpdate");
        currentServer = launcher.getAuth().getUserServersAttributes().get(dropBox.getSelectedIndex());
        Engine.getLOGGER().info("Launching " + currentServer.getServerName());
        launcher.getConfig().setConfigValue("selectedServer", dropBox.getSelectedIndex());
        launcher.getConfig().writeCurrentConfig();
        core = new Core(this, forceUpdate.isSelected());
    }

    private void handleUserPane(ActionEvent e) {
        engine.getExecutorServiceProvider().submitTask(() ->
                SwingUtilities.invokeLater(() -> {
                    JPanel panel = launcher.getUser().getPanel();
                    boolean isVisible = panel.isVisible();
                    String iconPath = isVisible ? "assets/ui/icons/menu.svg" : "assets/ui/icons/back.svg";
                    ImageIcon icon = launcher.getIconUtils().getVectorIcon(iconPath, 20, 24);
                    ((Button) getComponent("userPane")).setIcon(icon);

                    int startX = isVisible ? panel.getX() : -panel.getWidth();
                    int endX = isVisible ? -panel.getWidth() : 0;
                    Container panelParent = panel.getParent();
                    panelParent.setComponentZOrder(getComponent("userPane"), 0);

                    if (!isVisible) {
                        panel.setVisible(true);
                    } else {
                        engine.getGuiBuilder().getPanelsMap().get("userActions").setVisible(true);
                    }
                    panelParent.revalidate();
                    panelParent.repaint();

                    Object prop = panel.getClientProperty("currentAnimation");
                    if (prop instanceof Timer oldTimer && oldTimer.isRunning()) {
                        oldTimer.stop();
                    }

                    Timer timer = new Timer(1, null);
                    timer.setInitialDelay(0);
                    panel.putClientProperty("currentAnimation", timer);

                    final long startTime = System.currentTimeMillis();
                    final int animationDuration = 250;

                    timer.addActionListener(ex -> {
                        long elapsed = System.currentTimeMillis() - startTime;
                        float progress = Math.min(1f, (float) elapsed / animationDuration);
                        float easedProgress = 1 - (float) Math.pow(1 - progress, 3);
                        int newX = (int) (startX + (endX - startX) * easedProgress);
                        panel.setLocation(newX, panel.getY());
                        panelParent.setComponentZOrder(panel, 1);
                        panelParent.repaint();
                        if (progress >= 1f) {
                            timer.stop();
                            panel.putClientProperty("currentAnimation", null);
                            if (isVisible) {
                                panel.setVisible(false);
                            } else {
                                engine.getGuiBuilder().getPanelsMap().get("userActions").setVisible(false);
                            }
                            getComponent("userPane").setBounds(10, 40, 30, 30);
                            panelParent.revalidate();
                        }
                    });
                    timer.start();
                }), "userPaneToggle");
    }

    private void handleCloseButton(ActionEvent e) {
        launcher.getFrame().setVisible(false);
        launcher.getExecutorServiceProvider().shutdown();
        launcher.getSOUND().getSoundPlayer().stopAllSounds(() -> {
            launcher.getExecutorServiceProvider().shutdown();
            System.exit(0);
        });
    }

    public Launcher getLauncher() {
        return launcher;
    }
}
