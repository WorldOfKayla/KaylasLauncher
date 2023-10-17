package org.foxesworld.newengine.action;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.foxesworld.newengine.APP;
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
        AuthResponse response = new Gson().fromJson(this.POSTrequest.send("https://foxescraft.ru", inputData), AuthResponse.class);
        switch (response.type){
            case "success" -> {
                appFrame.displayPanel("[{\"panel\": \"authForm\", \"display\": false},{\"panel\": \"newsForm\", \"display\": true},{\"panel\": \"settings\", \"display\": false}]");
            }

            case "error" -> {

            }

        }
    }

    private void collectData(List<Component> authCredentials) {
        for(Component component: authCredentials){
            if(component instanceof JTextField) {
                inputData.put(component.getName(), ((JTextField) component).getText());
            }
        }
    }

    private class AuthResponse {
        @SerializedName("message")
        private  String message;

        @SerializedName("type")
        private String type;
    }
}
