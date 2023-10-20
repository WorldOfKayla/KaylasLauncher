package org.foxesworld.engine.action;

import org.foxesworld.engine.AppFrame;
import org.foxesworld.engine.gui.components.game.Game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;

public class ActionHandler {
    private AppFrame appFrame;

    public ActionHandler(AppFrame appFrame) {
        this.appFrame = appFrame;
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
                        this.appFrame.getAuth().formAuth(appFrame.getGuiBuilder().getComponentsMap().get(parent));
                    }
                }
            }

            case "test" -> {
                //appFrame.getFrame().getRootPanel().removeAll();
                //appFrame.getLoadingState().showLoadingState(60);
                //System.out.println(appFrame.getConfig().getFullPath());
                //appFrame.displayPanel("wait->true");
                appFrame.getDownload().download("https://foxescraft.ru/assets.zip", appFrame.getConfig().getFullPath()+"/assets.zip");
            }

            case "applySettings" -> {
               for(Component component: this.appFrame.getGuiBuilder().getComponentsMap().get("generalSettings")){
                   if(component instanceof JCheckBox){
                       this.appFrame.getConfig().setConfigValue(component.getName(), ((JCheckBox) component).isSelected());
                   } else {
                       if(component instanceof JTextField) {
                           this.appFrame.getConfig().setConfigValue(component.getName(), ((JTextField) component).getText());
                       }
                   }
               }
                this.appFrame.getConfig().writeCurrentConfig();
            }

            case "logOut" -> {
                System.out.println("LoggingOut...");
                this.appFrame.setAuthorised(false);
                this.appFrame.getConfig().clearConfigData(Arrays.asList("login", "password"), true);
                appFrame.displayPanel("loggedForm->false|newsForm->true|authForm->true");
            }

            case "settings" -> {
                if(!appFrame.isAuthorised()) {
                    appFrame.displayPanel("authForm->false|newsForm->false|settings->true");
                } else {
                    appFrame.displayPanel("loggedForm->false|newsForm->false|settings->true");
                }
            }

            case "back" -> {
                if(!appFrame.isAuthorised()) {
                    appFrame.displayPanel("authForm->true|newsForm->true|settings->false");
                } else {
                    appFrame.displayPanel("loggedForm->true|newsForm->true|settings->false");
                }
            }

            case "toGame" -> {
                System.out.println("GG");
                Game game = new Game(appFrame);
                game.testLaunch();
            }

            case "closeButton" -> System.exit(0);


            case "hideButton" ->  appFrame.getFrame().getFrame().setExtendedState(1);
        }
    }
}
