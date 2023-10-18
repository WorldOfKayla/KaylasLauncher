package org.foxesworld.newengine.action;

import org.foxesworld.newengine.AppFrame;

import java.awt.event.ActionEvent;

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
