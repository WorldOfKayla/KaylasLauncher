package org.foxesworld.newengine.action;

import org.foxesworld.newengine.AppFrame;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Auth {

    private AppFrame appFrame;
    private Map<String, String> inputData = new HashMap<>();

    public Auth(AppFrame appFrame){
        this.appFrame = appFrame;
    }

    public void authorize(List<Component> authCredentials){
        this.collectData(authCredentials);
        JOptionPane.showMessageDialog(null, "Authorising with \n LOGIN: "+inputData.get("inputLogin") + " \n PASSWORD: "+inputData.get("inputPass"));
    }

    private void collectData(List<Component> authCredentials) {
        for(Component component: authCredentials){
            if(component instanceof JTextField) {
                inputData.put(component.getName(), ((JTextField) component).getText());
            }
        }
    }
}
