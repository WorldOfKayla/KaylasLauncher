package org.foxesworld.newengine.action;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.foxesworld.newengine.AppFrame;
import org.foxesworld.newengine.utils.HTTP.HTTPrequest;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Auth {

    private AppFrame appFrame;

    private Map<String, Object> CONFIG;
    private HTTPrequest POSTrequest;
    private Map<String, String> inputData = new HashMap<>();

    public Auth(AppFrame appFrame) {
        this.appFrame = appFrame;
        this.POSTrequest = appFrame.getPOSTrequest();
        this.CONFIG = appFrame.getCONFIG();
        if (CONFIG.get("login") != null && CONFIG.get("password") != null) {
            this.appFrame.getLOGGER().debug("Authorising with existing login " + CONFIG.get("login"));
            Map<String, String> authCredentials = new HashMap<>();
            authCredentials.put("login", (String) CONFIG.get("login"));
            authCredentials.put("password", (String) CONFIG.get("password"));
            if(!this.authorize(authCredentials)){
                System.out.println("Wiping authData!");
            }
        }
    }

    protected void formAuth(List<Component> authCredentials) {
        for (Component component : authCredentials) {
            if (component instanceof JTextField) {
                inputData.put(component.getName(), ((JTextField) component).getText());
            }
        }
        this.authorize(inputData);
    }

    public boolean authorize(Map<String, String> authCredentials) {
        authCredentials.put("userAction", "auth");
        AuthResponse response = new Gson().fromJson(this.POSTrequest.send("https://foxescraft.ru", authCredentials), AuthResponse.class);
        boolean success = response.type.equals("success");
        if (success) {
            appFrame.setAuthorised(success);
            appFrame.displayPanel("authForm->false|logged->true|newsForm->true|settings->false");
            appFrame.getConfigReader().addToConfig(authCredentials, Arrays.asList("login", "password"));
            System.out.println(appFrame.getConfigReader().getConfigWriter().configToJSON());
        } else {

        }
        return success;
    }

    private class AuthResponse {
        @SerializedName("message")
        private String message;

        @SerializedName("type")
        private String type;
    }
}
