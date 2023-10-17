package org.foxesworld.newengine.action;

import org.foxesworld.newengine.AppFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

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
                        Auth auth = new Auth(this.appFrame);
                        auth.authorize(appFrame.getGuiBuilder().getComponentsMap().get(parent));
                    }
                }
            }

            case "test" -> {
            }

            case "settings" -> {
                appFrame.displayPanel("[{\"panel\": \"authForm\", \"display\": false},{\"panel\": \"newsForm\", \"display\": false},{\"panel\": \"settings\", \"display\": true}]");
            }

            case "back" -> {
                appFrame.displayPanel("[{\"panel\": \"authForm\", \"display\": true},{\"panel\": \"newsForm\", \"display\": true},{\"panel\": \"settings\", \"display\": false}]");
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
