package org.foxesworld.launcher.gui;

import com.formdev.flatlaf.ui.FlatProgressBarUI;
import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.button.Button;
import org.foxesworld.engine.gui.components.checkbox.Checkbox;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.multiButton.MultiButton;
import org.foxesworld.engine.gui.components.passfield.PassField;
import org.foxesworld.engine.gui.components.textfield.TextField;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.sound.PlaybackStatusListener;
import org.foxesworld.engine.sound.SoundPlayer;
import org.foxesworld.launcher.Core;
import org.foxesworld.notification.Notification;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
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
                    switch (parent){
                        case "authForm" -> {
                            this.launcher.getAuth().formAuth();
                            if (this.launcher.getAuth().isAuthorised()) {
                                engine.getFrame().getRootPanel().removeAll();
                                engine.getPanelVisibility().displayPanel("authForm->false");
                                this.engine.init();
                            } else {
                                ((TextField)this.getComponent("login")).resetText();
                                ((PassField)this.getComponent("password")).resetText();
                            }
                        }

                        case "userinfo" -> Engine.getLOGGER().warn("TEST");
                    }
                }

                case "smallButton" -> {
                    launcher.getPanelVisibility().displayPanel("download->true|loggedForm->false|newsForm->false");
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
                    this.launcher.getNotification().display("Sound Test", sound, new ImageIcon(this.launcher.getImageUtils().getLocalImage("assets/ui/icons/logo.png")));//this.launcher.getIconUtils().getVectorIcon("assets/ui/icons/aidenfox.svg", 128, 128));
                */
                }


                case "gameDir-small" -> Settings.openGameFolder();
                case "cancelDownload-small" -> {
                    core.getFileLoader().cancel();
                    this.getComponent("logOut").setEnabled(true);
                    this.getComponent("toGame").setEnabled(true);
                }
                case "applySettings" -> {
                    this.getEngine().getGuiBuilder().getComponentFactory().getCustomTooltip().clearAllTooltips();
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
                    this.getLauncher().getLoadingManager().toggleLoader();
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
                    this.getComponent(key).setEnabled(false);
                    this.getComponent("logOut").setEnabled(false);
                    DropBox dropBox = (DropBox) this.getComponent("serverBox");
                    Checkbox forceUpdate = (Checkbox) this.getComponent("forceUpdate");
                    this.currentServer = launcher.getAuth().getUserServersAttributes().get(dropBox.getSelectedIndex());
                    Engine.getLOGGER().info("Launching " + this.currentServer.getServerName());
                    this.launcher.getConfig().setConfigValue("selectedServer", dropBox.getSelectedIndex());
                    this.launcher.getConfig().writeCurrentConfig();
                    this.core = new Core(this, forceUpdate.isSelected());
                }
                case "closeButton" -> System.exit(0);
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