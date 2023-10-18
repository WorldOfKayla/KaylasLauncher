package org.foxesworld.newengine.action;

import org.foxesworld.newengine.AppFrame;
import org.foxesworld.newengine.gui.components.Components;

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
                System.out.println(appFrame.getConfig().getFullPath());
                appFrame.getDownload().download("https://foxescraft.ru/assets.zip", appFrame.getConfig().getFullPath()+"/assets.zip");
            }

            case "applySettings" -> {
               for(Component component: this.appFrame.getGuiBuilder().getComponentsMap().get("generalSettings")){
                   if(component instanceof JCheckBox){
                       this.appFrame.getConfig().setConfigValue(component.getName(), ((JCheckBox) component).isSelected());
                   }
               }
                this.appFrame.getConfig().writeCurrentConfig();
            }

            case "logOut" -> {
                System.out.println("LoggingOut...");
                this.appFrame.setAuthorised(false);
                this.appFrame.getConfig().clearConfigData(Arrays.asList("login", "password"), true);
                appFrame.displayPanel("logged->false|newsForm->true|authForm->true");
            }

            case "settings" -> {
                if(!appFrame.isAuthorised()) {
                    appFrame.displayPanel("authForm->false|newsForm->false|settings->true");
                } else {
                    appFrame.displayPanel("logged->false|newsForm->false|settings->true");
                }
            }

            case "back" -> {
                if(!appFrame.isAuthorised()) {
                    appFrame.displayPanel("authForm->true|newsForm->true|settings->false");
                } else {
                    appFrame.displayPanel("logged->true|newsForm->true|settings->false");
                }
            }

            case "closeButton" -> {
                System.exit(0);
            }

            case "hideButton" -> {
                appFrame.getFrame().getFrame().setExtendedState(1);
            }
        }
    }


}
