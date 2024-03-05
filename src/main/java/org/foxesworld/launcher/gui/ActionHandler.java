package org.foxesworld.launcher.gui;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.launcher.Schedule;
import org.foxesworld.Launcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public class ActionHandler extends org.foxesworld.engine.gui.ActionHandler {

    protected final Launcher launcher;
    protected ServerAttributes currentServer;
    public ActionHandler(Launcher launcher) {
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
        //if(!engine.getLoadingManager().getLoadingTimer().isRunning()) {
            switch (key) {
                case "submit" -> {
                    if ("authForm".equals(parent)) {
                        this.launcher.getAuth().formAuth(engine.getGuiBuilder().getComponentsMap().get(parent));
                        if (this.launcher.getAuth().isAuthorised()) {
                            engine.getFrame().getRootPanel().removeAll();
                            engine.getPanelVisibility().displayPanel("authForm->false");
                            this.engine.initialize(this.launcher);
                        }
                    }
                }

                case "smallButton" -> {
                    this.engine.getGuiBuilder().getComponentById(key).setEnabled(false);
                    /*
                    if(!this.engine.getLoadingManager().getLoadingTimer().isRunning()) {
                        this.engine.getLoadingManager().startLoading();
                    } else {
                        this.engine.getLoadingManager().stopLoading();
                    } */
                    engine.getLoadingManager().toggleLoader();

                    engine.getSOUND().playSound("exit.ogg", false);
                }

                case "gameDir-small" -> openGameFolder();

                case "applySettings" -> {
                    for (JComponent component : this.engine.getGuiBuilder().getComponentsMap().get("settingsFields")) {
                        if (component instanceof JCheckBox) {
                            this.engine.getCONFIG().setConfigValue(component.getName(), ((JCheckBox) component).isSelected());
                        } else if (component instanceof JTextField) {
                            this.engine.getCONFIG().setConfigValue(component.getName(), ((JTextField) component).getText());
                        } else {
                            if (component instanceof JSlider) {
                                this.engine.getCONFIG().setConfigValue(component.getName(), ((JSlider) component).getValue());
                            }
                        } if(component instanceof DropBox){
                            System.out.println(((DropBox) component).getSelected());
                            this.engine.getCONFIG().setConfigValue(component.getName(), ((DropBox) component).getSelected());
                        }
                    }
                    this.engine.getCONFIG().writeCurrentConfig();
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

                case "toGame" -> {
                    this.engine.getGuiBuilder().getComponentById(key).setEnabled(false);
                    this.engine.getGuiBuilder().getComponentById("logOut").setEnabled(false);
                    DropBox dropBox = (DropBox) engine.getGuiBuilder().getComponentById("serverBox");
                    this.currentServer = launcher.getAuth().getUserServersAttributes().get(dropBox.getSelectedIndex());
                    this.getEngine().getLOGGER().info("Launching " + this.currentServer.getServerName());
                    this.engine.getCONFIG().setConfigValue("selectedServer", dropBox.getSelectedIndex());
                    this.engine.getCONFIG().writeCurrentConfig();
                    new Schedule(this);
                }

                case "closeButton" -> System.exit(0);

                case "hideButton" -> engine.getFrame().setExtendedState(1);
            }
        //}
    }

    private void openGameFolder() {
        try {
            Desktop d = Desktop.getDesktop();
            d.browse(new URI(engine.getCONFIG().getFullPath().replaceAll(Pattern.quote("\\"), "/")));
        } catch (IOException | URISyntaxException ignored) {
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