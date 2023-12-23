package org.foxesworld.launcher.action;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.scrollBox.ScrollBox;
import org.foxesworld.launcher.Game.Game;
import org.foxesworld.launcher.Launcher;
import org.foxesworld.launcher.Server.ServerAttributes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public class ActionHandler {
    private final Engine engine;
    private final Launcher launcher;
    private ServerAttributes currentServer;

    public ActionHandler(Launcher launcher) {
        this.launcher = launcher;
        this.engine = launcher.getEngine();
    }

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
                engine.getSOUND().playSound("exit.ogg", false);
            }

            case "gameDir" -> openGameFolder();

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
                    }
                }
                this.engine.getCONFIG().writeCurrentConfig();
            }

            case "logOut" -> {
                this.launcher.getAuth().logOut();
            }

            case "settings" -> {
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
                this.currentServer = launcher.getAuth().getUserServersAttributes().get(ScrollBox.getSelectedIndex());
                this.getEngine().getLOGGER().info("Launching " + this.currentServer.getServerName());
                this.engine.getCONFIG().setConfigValue("selectedServer", ScrollBox.getSelectedIndex());
                this.engine.getCONFIG().writeCurrentConfig();
                new Game(this);
            }

            case "closeButton" -> System.exit(0);

            case "hideButton" -> engine.getFrame().getFrame().setExtendedState(1);
        }
    }

    private void openGameFolder() {
        try {
            Desktop d = Desktop.getDesktop();
            d.browse(new URI(engine.getCONFIG().getFullPath().replaceAll(Pattern.quote("\\"), "/")));
        } catch (IOException | URISyntaxException ignored) {
        }
    }

    public ServerAttributes getCurrentServer() {
        return currentServer;
    }

    public Engine getEngine() {
        return engine;
    }

    public Launcher getLauncher() {
        return launcher;
    }
}