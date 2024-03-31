package org.foxesworld.launcher.gui;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.textfield.TextField;
import org.foxesworld.launcher.config.Config;
import org.foxesworld.engine.gui.components.checkbox.Checkbox;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.launcher.Core;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ActionHandler extends org.foxesworld.engine.gui.ActionHandler {

    protected Launcher launcher;
    protected ServerAttributes currentServer;
    protected  UserInfo userInfo;
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
            switch (key) {
                case "submit" -> {
                    switch (parent){
                        case "authForm" -> {
                            this.launcher.getAuth().formAuth(engine.getGuiBuilder().getComponentsMap().get(parent));
                            if (this.launcher.getAuth().isAuthorised()) {
                                engine.getFrame().getRootPanel().removeAll();
                                engine.getPanelVisibility().displayPanel("authForm->false");
                                this.engine.init(this.launcher);
                            }
                        }

                        case "userinfo" ->{
                            TextField textField = new TextField("");
                            for(JComponent component : this.launcher.getGuiBuilder().getComponentsMap().get("test")){
                                if(component instanceof TextField) {
                                   textField = (TextField) component;
                                }
                            }
                            this.userInfo.sendRequest(textField.getText());
                        }
                    }
                }

                case "smallButton" -> {
                    if (!launcher.getAuth().isAuthorised()) {
                        engine.getPanelVisibility().displayPanel("authForm->false|newsForm->false|test->true");
                    } else {
                        engine.getPanelVisibility().displayPanel("loggedForm->false|newsForm->false|test->true");
                    }
                    //this.engine.getGuiBuilder().getComponentById(key).setEnabled(false);
                }

                case "gameDir-small" -> Settings.openGameFolder();

                case "applySettings" -> {
                    for (JComponent component : this.launcher.getGuiBuilder().getComponentsMap().get("settingsFields")) {
                        Class<Config> clazz = Config.class;
                        try {
                           clazz.getDeclaredField(component.getName());
                        if (component instanceof Checkbox) {
                            this.launcher.getConfig().setConfigValue(component.getName(), ((JCheckBox) component).isSelected());
                        } else if (component instanceof JTextField) {
                            this.launcher.getConfig().setConfigValue(component.getName(), ((JTextField) component).getText());
                        } else {
                            if (component instanceof JSlider) {
                                this.launcher.getConfig().setConfigValue(component.getName(), ((JSlider) component).getValue());
                            }
                        } if(component instanceof DropBox){
                                this.launcher.getConfig().setConfigValue(component.getName(), ((DropBox) component).getSelected());
                        }
                        } catch (NoSuchFieldException ignored) {
                        }
                    }
                    this.launcher.getConfig().writeCurrentConfig();
                    this.launcher.getSOUND().getSoundPlayer().stopAllSounds();
                    this.launcher.getEngine().getFrame().dispose();
                    this.launcher = new Launcher();
                }

                case "logOut" -> this.launcher.getAuth().logOut();

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
    public ServerAttributes getCurrentServer() {
        return currentServer;
    }
    public Launcher getLauncher() {
        return launcher;
    }
}