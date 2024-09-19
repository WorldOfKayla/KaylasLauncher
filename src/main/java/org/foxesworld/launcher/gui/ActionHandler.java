package org.foxesworld.launcher.gui;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.launcher.Core;
import org.foxesworld.notification.Notification;

import javax.swing.*;
import java.awt.event.ActionEvent;
public class ActionHandler extends org.foxesworld.engine.gui.ActionHandler {

    protected Launcher launcher;
    protected ServerAttributes currentServer;
    protected  final UserInfo userInfo;
    public ActionHandler(Launcher launcher) {
        this.launcher = launcher;
        this.engine = launcher.getEngine();
        this.userInfo = new UserInfo(launcher);
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
        JComponent component = this.getEngine().getGuiBuilder().getComponentById(key);
            switch (key) {
                case "submit" -> {
                    switch (parent){
                        case "authForm" -> {
                            this.launcher.getAuth().formAuth();
                            if (this.launcher.getAuth().isAuthorised()) {
                                engine.getFrame().getRootPanel().removeAll();
                                engine.getPanelVisibility().displayPanel("authForm->false");
                                this.engine.init();
                            }
                        }

                        case "userinfo" -> this.userInfo.sendRequest();
                    }
                }

                case "smallButton" -> {
                    //engine.getPanelVisibility().displayPanel("loggedForm->false|newsForm->false|download->true");
                    this.launcher.getLoadingManager().toggleLoader();
                }

                case "gameDir-small" -> Settings.openGameFolder();
                case "applySettings" -> this.launcher.getSettings().applySettings();
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
                    this.engine.getGuiBuilder().getComponentById(key).setEnabled(false);
                    this.engine.getGuiBuilder().getComponentById("logOut").setEnabled(false);
                    DropBox dropBox = (DropBox) engine.getGuiBuilder().getComponentById("serverBox");
                    this.currentServer = launcher.getAuth().getUserServersAttributes().get(dropBox.getSelectedIndex());
                    Engine.getLOGGER().info("Launching " + this.currentServer.getServerName());
                    this.launcher.getConfig().setConfigValue("selectedServer", dropBox.getSelectedIndex());
                    this.launcher.getConfig().writeCurrentConfig();
                    new Core(this);
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