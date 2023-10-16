package org.foxesworld.newengine.action;

import org.foxesworld.newengine.AppFrame;

import java.awt.event.ActionEvent;

public class ActionHandler {
    private AppFrame appFrame;

    public ActionHandler(AppFrame appFrame) {
        this.appFrame = appFrame;
    }

    public void handleAction(ActionEvent e){
        switch (e.getActionCommand()){
            case "submit" -> {
                this.appFrame.getDownload().download("https://cdimage.debian.org/cdimage/archive/11.7.0/amd64/iso-cd/debian-11.7.0-amd64-netinst.iso", "");
            }

            case "settings" -> {
                appFrame.displayPanel("authForm", false);
                appFrame.displayPanel("newsForm", false);
                appFrame.displayPanel("settings", true);
            }

            case "back" -> {
                appFrame.displayPanel("settings", false);
                appFrame.displayPanel("authForm", true);
                appFrame.displayPanel("newsForm", true);
            }
        }
    }


}
