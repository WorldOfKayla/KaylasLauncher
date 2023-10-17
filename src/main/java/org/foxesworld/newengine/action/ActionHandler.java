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
                //this.appFrame.getDownload().download("https://cdimage.debian.org/cdimage/archive/11.7.0/amd64/iso-cd/debian-11.7.0-amd64-netinst.iso", "");
                //for(Component component:appFrame.getGuiBuilder().getComponentsMap("authForm")){
                //    if(component instanceof JTextField){
                //        System.out.println(((JTextField) component).getText());
                 //   }

                //}
                //JOptionPane.showMessageDialog(null, "");
                //appFrame.displayPanel("wait", true);
                appFrame.displayPanel("[{\"panel\": \"authForm\", \"display\": false},{\"panel\": \"newsForm\", \"display\": false},{\"panel\": \"wait\", \"display\": true}]");
            }

            case "settings" -> {
                appFrame.displayPanel("[{\"panel\": \"authForm\", \"display\": false},{\"panel\": \"newsForm\", \"display\": false},{\"panel\": \"settings\", \"display\": true}]");
            }

            case "back" -> {
                appFrame.displayPanel("[{\"panel\": \"authForm\", \"display\": true},{\"panel\": \"newsForm\", \"display\": true},{\"panel\": \"settings\", \"display\": false}]");
            }
        }
    }


}
