package org.foxesworld.newengine.action;

import org.foxesworld.newengine.AppFrame;
import org.foxesworld.newengine.utils.HTTP.HTTPrequest;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Auth {

    private AppFrame appFrame;
    private HTTPrequest POSTrequest;
    private Map<String, String> inputData = new HashMap<>();

    public Auth(AppFrame appFrame){
        this.appFrame = appFrame;
        this.POSTrequest = appFrame.getPOSTrequest();
    }

    public void authorize(List<Component> authCredentials){
        this.collectData(authCredentials);
        inputData.put("userAction", "auth");
        String response = this.POSTrequest.send("https://foxescraft.ru", inputData);
    }

    private void collectData(List<Component> authCredentials) {
        for(Component component: authCredentials){
            if(component instanceof JTextField) {
                inputData.put(component.getName(), ((JTextField) component).getText());
            }
        }
    }
}
