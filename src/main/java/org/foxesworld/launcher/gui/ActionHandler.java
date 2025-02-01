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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.List;

public class ActionHandler extends org.foxesworld.engine.gui.ActionHandler {

    protected Launcher launcher;
    private Core core;
    protected ServerAttributes currentServer;

    public ActionHandler(Launcher launcher) {
        super(launcher.getGuiBuilder(), "mainFrame", List.of(TextField.class, Checkbox.class, JProgressBar.class, PassField.class, Button.class, MultiButton.class, DropBox.class));
        this.launcher = launcher;
        this.engine = launcher.getEngine();
    }

    @Override
    public void handleAction(ActionEvent e) {
        String key = e.getActionCommand();
        String parent = "";
        if (e.getActionCommand().contains(">")) {
            String[] command = e.getActionCommand().split(">");
            key = command[1];
            parent = command[0];
        }
        Component pressedComponent = this.getComponent(key);

        switch (key) {
            case "submit" -> {

                switch (parent) {
                    case "authForm" -> {
                        this.getComponent("authForm>submit").setEnabled(false);
                        this.launcher.getExecutorServiceProvider().submitTask(() -> {
                            this.launcher.getAuth().formAuth(this.getComponent("authForm>submit"));
                            if (this.launcher.getAuth().isAuthorised()) {
                                engine.getFrame().getRootPanel().removeAll();
                                engine.getPanelVisibility().displayPanel("authForm->false");
                                this.launcher.init();
                            } else {
                                ((TextField) this.getComponent("login")).resetText();
                                ((PassField) this.getComponent("password")).resetText();
                            }
                        }, "auth");
                    }

                    case "userinfo" -> Engine.getLOGGER().warn("TEST");
                }
            }

            case "smallButton" -> {
                this.launcher.getLoadingManager().toggleVisibility();
                //launcher.getOptionalModsWindow().toggleVisibility();
                // launcher.init();
                    /*
                    SoundPlayer.setUPDATE_RATE(10);
                    JProgressBar sndBar = (JProgressBar) this.getComponent("sndBar");
                    sndBar.setUI(new FlatProgressBarUI());
                    PlaybackStatusListener listener = new PlaybackStatusListener() {
                        @Override
                        public void onPlaybackStarted(String path) {
                            pressedComponent.setEnabled(false);
                            sndBar.setVisible(true);
                            launcher.getLoadingManager().setLoadingText(launcher.getLANG().getString("playback.started"), "Test");
                        }

                        @Override
                        public void onPlaybackStopped(String path) {
                            pressedComponent.setEnabled(true);
                            sndBar.setValue(0);
                            sndBar.setVisible(false);
                            launcher.getLoadingManager().setLoadingText(launcher.getLANG().getString("playback.finished"), "Test");
                        }

                        @Override
                        public void onPlaybackProgress(String path, long microsecondPosition, long microsecondLength) {
                            int progress = (int) ((double) microsecondPosition / microsecondLength * 100);
                            SwingUtilities.invokeLater(() -> sndBar.setValue(progress));
                        }
                    };
                    String sound = this.launcher.getSOUND().playSound("other", "ogo", listener);
                    //this.launcher.getLoadingManager().toggleLoader();
                    //this.launcher.getNotification().display("Sound Test", sound, new ImageIcon(this.launcher.getImageUtils().getLocalImage("assets/ui/icons/logo.png")));//this.launcher.getIconUtils().getVectorIcon("assets/ui/icons/aidenfox.svg", 128, 128));
                     */
            }


            case "gameDir-small" -> this.launcher.getSettings().openGameFolder();
            case "cancelDownload-small" -> {
                core.getFileLoader().cancel();
            }
            case "applySettings" -> {
                //this.getEngine().getGuiBuilder().getComponentFactory().ggetCustomTooltip().clearAllTooltips();
                this.launcher.getSettings().applySettings("settingsFields");
            }
            case "logOut" -> {
                this.launcher.getSOUND().playSound("other", "loggedOut");
                this.launcher.getGuiBuilder().getNotification().show(Notification.Type.SUCCESS, Notification.Location.BOTTOM_LEFT, this.launcher.getUser().getLogin() + this.launcher.getLANG().getString("auth.loggedOut"));
                this.launcher.getAuth().logOut();
            }
            case "info-small" -> {
                if (!launcher.getAuth().isAuthorised()) {
                    engine.getPanelVisibility().displayPanel("authForm->false|newsForm->false|test->true");
                } else {
                    engine.getPanelVisibility().displayPanel("loggedForm->false|newsForm->false|test->true");
                }
            }

            case "settings-small" -> {
                if (!launcher.getAuth().isAuthorised()) {
                    engine.getPanelVisibility().displayPanel("authForm->false|newsForm->false|settings->true");
                } else {
                    engine.getPanelVisibility().displayPanel("loggedForm->false|newsForm->false|settings->true");
                }
            }

            case "loadCancel-small" -> {
                this.core.getFileLoader().cancel();
                this.getLauncher().getLoadingManager().toggleVisibility();
                this.getComponent("toGame").setEnabled(false);
                this.getComponent("logOut").setEnabled(false);
            }

            case "back" -> {
                if (!launcher.getAuth().isAuthorised()) {
                    engine.getPanelVisibility().displayPanel("authForm->true|newsForm->true|settings->false");
                } else {
                    engine.getPanelVisibility().displayPanel("loggedForm->true|newsForm->true|settings->false");
                }
            }

            case "back-test" -> {
                if (!launcher.getAuth().isAuthorised()) {
                    engine.getPanelVisibility().displayPanel("authForm->true|newsForm->true|test->false");
                } else {
                    engine.getPanelVisibility().displayPanel("loggedForm->true|newsForm->true|test->false");
                }
            }

            case "toGame" -> {
                this.launcher.getSOUND().playSound("other", "start");
                DropBox dropBox = (DropBox) this.getComponent("serverBox");
                Checkbox forceUpdate = (Checkbox) this.getComponent("forceUpdate");
                this.currentServer = launcher.getAuth().getUserServersAttributes().get(dropBox.getSelectedIndex());
                Engine.getLOGGER().info("Launching " + this.currentServer.getServerName());
                this.launcher.getConfig().setConfigValue("selectedServer", dropBox.getSelectedIndex());
                this.launcher.getConfig().writeCurrentConfig();
                this.core = new Core(this, forceUpdate.isSelected());
            }

            case "optionalMods" -> {
                launcher.showDialog("Опциональные моды будут позже", "Work In Progress", JOptionPane.WARNING_MESSAGE, false);
            }

            case "userPane" -> {
                this.getEngine().getExecutorServiceProvider().submitTask(() -> {
                    JPanel panel = this.launcher.getUser().getPanel();
                    boolean isVisible = panel.isVisible();
                    String iconPath = isVisible
                            ? "assets/ui/icons/menu.svg"
                            : "assets/ui/icons/back.svg";
                    ImageIcon icon = this.launcher.getIconUtils().getVectorIcon(iconPath, 20, 24);
                    ((Button) pressedComponent).setIcon(icon);

                    int startX = isVisible ? panel.getX() : -panel.getWidth();
                    int endX = isVisible ? -panel.getWidth() : 0;
                    Container panelParent = panel.getParent();
                    if (panelParent != null) {
                        panelParent.setComponentZOrder(pressedComponent, 0);
                        panelParent.setComponentZOrder(panel, 1);
                        if (!isVisible) {
                            panel.setVisible(true);
                        } else {
                            this.getEngine().getGuiBuilder().getPanelsMap().get("userActions").setVisible(true);
                        }
                        panelParent.revalidate();
                        panelParent.repaint();
                    }

                    Object prop = panel.getClientProperty("currentAnimation");
                    if (prop instanceof Timer oldTimer && oldTimer.isRunning()) {
                        oldTimer.stop();
                    }

                    Timer timer = new Timer(15, null);
                    timer.setInitialDelay(0);
                    panel.putClientProperty("currentAnimation", timer);

                    final long[] startTime = {-1};
                    timer.addActionListener(ex -> {
                        long currentTime = System.currentTimeMillis();
                        if (startTime[0] < 0) {
                            startTime[0] = currentTime;
                        }

                        float progress = Math.min(1f, (currentTime - startTime[0]) / 300f);
                        float interpolated = 1 - (float) Math.pow(1 - progress, 3);
                        int newX = (int) (startX + (endX - startX) * interpolated);
                        panel.setLocation(newX, panel.getY());

                        if (panelParent != null) {
                            panelParent.setComponentZOrder(pressedComponent, 0);
                            panelParent.setComponentZOrder(panel, 1);
                        }
                        panelParent.repaint();

                        if (progress >= 1f) {
                            timer.stop();
                            panel.putClientProperty("currentAnimation", null);
                            if (isVisible) {
                                panel.setVisible(false);
                            } else {
                                this.getEngine().getGuiBuilder().getPanelsMap().get("userActions").setVisible(false);
                            }
                            panelParent.setComponentZOrder(pressedComponent, 0);
                            panelParent.revalidate();
                        }
                    });
                    timer.start();
                }, "userPaneToggle");
            }

            case "closeButton" -> {
                this.launcher.getFrame().setVisible(false);
                this.launcher.getExecutorServiceProvider().shutdown();
                this.launcher.getSOUND().getSoundPlayer().stopAllSounds(() -> {
                    this.launcher.getExecutorServiceProvider().shutdown();
                    System.exit(0);
                });
            }
            case "hideButton" -> engine.getFrame().setExtendedState(1);
        }
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
}