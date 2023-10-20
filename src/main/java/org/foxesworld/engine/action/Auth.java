package org.foxesworld.engine.action;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.action.server.ServerParser;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Auth {
    private Engine engine;
    private ServerParser serverParser;
    private Map<String, Object> CONFIG;
    private HTTPrequest POSTrequest;
    private Map<String, String> inputData = new HashMap<>();

    public Auth(Engine engine) {
        this.engine = engine;
        this.POSTrequest = engine.getPOSTrequest();
        this.CONFIG = engine.getCONFIG();
        if (CONFIG.get("login") != null && CONFIG.get("password") != null) {
            Map<String, String> authCredentials = new HashMap<>();
            authCredentials.put("login", (String) CONFIG.get("login"));
            authCredentials.put("password", (String) CONFIG.get("password"));
            this.engine.getLOGGER().debug("Authorising with existing login " + CONFIG.get("login"));
            if(!this.authorize(authCredentials)){
                engine.getConfig().clearConfigData(Arrays.asList("login", "password"), true);
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
        AuthResponse response = new Gson().fromJson(this.POSTrequest.send(authCredentials), AuthResponse.class);
        boolean status = response.type.equals("success");

        if (status) {
            engine.setAuthorised(status);
            engine.displayPanel("authForm->false|loggedForm->true");
            engine.getLOGGER().info(CONFIG.get("login") + " authorised!");
            serverParser = new ServerParser(engine);

            if(CONFIG.get("login") == null){
                engine.getConfig().addToConfig(authCredentials, Arrays.asList("login", "password"));
                engine.getConfig().writeCurrentConfig();
            }
        } else {
            System.out.println("Screw you!");
        }
        return status;
    }

    private class AuthResponse {
        @SerializedName("message")
        private String message;

        @SerializedName("type")
        private String type;
    }
}
