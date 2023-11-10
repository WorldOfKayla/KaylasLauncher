package org.foxesworld.engine.action;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.scrollBox.ScrollBox;
import org.foxesworld.launcher.Game.Game;
import org.foxesworld.launcher.server.ServerAttributes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.regex.Pattern;

public class ActionHandler {
    private final Engine engine;
    private Game game;

    private ServerAttributes currentServer;
    public ActionHandler(Engine engine) {
        this.engine = engine;
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
                    this.engine.getAuth().formAuth(engine.getGuiBuilder().getComponentsMap().get(parent));
                    if (this.engine.getAuth().isAuthorised()) {
                        engine.getFrame().getRootPanel().removeAll();
                        engine.displayPanel("authForm->false");
                        this.engine.initialize(this.engine.getAuth().getAuthCredentials("login"));
                    }
                }
            }

            case "test" -> {}//this.engine.displayPanel("loggedForm->false|newsForm->false|download->true");

            case "gameDir" -> openGameFolder();

            case "applySettings" -> {
                for(JComponent component: this.engine.getGuiBuilder().getComponentsMap().get("settingsFields")){
                    if(component instanceof JCheckBox){
                        this.engine.getCONFIG().setConfigValue(component.getName(), ((JCheckBox) component).isSelected());
                        System.out.println(((JCheckBox) component).isSelected());
                    } else {
                        if(component instanceof JTextField) {
                            this.engine.getCONFIG().setConfigValue(component.getName(), ((JTextField) component).getText());
                        }
                    }
                }
                this.engine.getCONFIG().writeCurrentConfig();
            }

            case "logOut" -> {
                this.engine.getLOGGER().info("LoggingOut...");
                this.engine.getAuth().setAuthorised(false);
                engine.getFrame().getRootPanel().removeAll();
                this.engine.getCONFIG().clearConfigData(Arrays.asList("login", "password"), true);
                engine.initialize("");
            }

            case "settings" -> {
                if(!engine.getAuth().isAuthorised()) {
                    engine.displayPanel("authForm->false|newsForm->false|settings->true");
                } else {
                    engine.displayPanel("loggedForm->false|newsForm->false|settings->true");
                }
            }

            case "back" -> {
                if(!engine.getAuth().isAuthorised()) {
                    engine.displayPanel("authForm->true|newsForm->true|settings->false");
                } else {
                    engine.displayPanel("loggedForm->true|newsForm->true|settings->false");
                }
            }

            case "toGame" -> {
                this.currentServer = engine.getAuth().getUserServersAttributes().get(ScrollBox.getSelectedIndex());
                this.getEngine().getLOGGER().info("Launching "+this.currentServer.serverName);
                this.engine.getCONFIG().setConfigValue("selectedServer", ScrollBox.getSelectedIndex());
                this.engine.getCONFIG().writeCurrentConfig();
                game = new Game(this);
                game.start();
            }

            case "closeButton" -> System.exit(0);

            case "hideButton" ->  engine.getFrame().getFrame().setExtendedState(1);
        }
    }

    private void openGameFolder() {
        try {
            engine.getSOUND().playSound("openFolder.ogg");
            Desktop d = Desktop.getDesktop();
            d.browse(new URI(engine.getCONFIG().getFullPath().replaceAll(Pattern.quote("\\"), "/")));
        } catch (IOException | URISyntaxException ignored) {}
    }


    public ServerAttributes getCurrentServer() {
        return currentServer;
    }

    public Engine getEngine() {
        return engine;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }
}