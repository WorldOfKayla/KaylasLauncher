package org.foxesworld.engine.action;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.components.game.Game;
import org.foxesworld.engine.gui.components.scrollBox.ScrollBox;
import org.foxesworld.launcher.server.ServerAttributes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;

public class ActionHandler {
    private Engine engine;

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
                switch(parent){
                    case "authForm" -> {
                        this.engine.getAuth().formAuth(engine.getGuiBuilder().getComponentsMap().get(parent));
                        if(this.engine.getAuth().isAuthorised()) {
                            this.engine = new Engine(this.engine.getAPP());
                        }
                    }
                }
            }

            case "test" -> {
                //engine.getFrame().getRootPanel().removeAll();
                //engine.getLoadingState().showLoadingState(60);
                //System.out.println(engine.getCONFIG().getFullPath());
                //engine.displayPanel("wait->true");
                engine.getDownload().download("https://foxescraft.ru/assets.zip", engine.getCONFIG().getFullPath()+"/assets.zip");
            }

            case "applySettings" -> {
               for(Component component: this.engine.getGuiBuilder().getComponentsMap().get("generalSettings")){
                   if(component instanceof JCheckBox){
                       this.engine.getCONFIG().setConfigValue(component.getName(), ((JCheckBox) component).isSelected());
                   } else {
                       if(component instanceof JTextField) {
                           this.engine.getCONFIG().setConfigValue(component.getName(), ((JTextField) component).getText());
                       }
                   }
               }
                this.engine.getCONFIG().writeCurrentConfig();
            }

            case "logOut" -> {
                System.out.println("LoggingOut...");
                this.engine.getAuth().setAuthorised(false);
                this.engine.getCONFIG().clearConfigData(Arrays.asList("login", "password"), true);
                engine.displayPanel("loggedForm->false|newsForm->true|authForm->true");
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
                Component component = engine.getGuiBuilder().getComponentById("serverBox");
                this.currentServer = engine.getAuth().getUserServersAttributes().get(((ScrollBox) component).getSelectedIndex());
                Game game = new Game(this);
                //game.launchGame();
            }

            case "closeButton" -> System.exit(0);


            case "hideButton" ->  engine.getFrame().getFrame().setExtendedState(1);
        }
    }

    public ServerAttributes getCurrentServer() {
        return currentServer;
    }

    public Engine getEngine() {
        return engine;
    }
}
