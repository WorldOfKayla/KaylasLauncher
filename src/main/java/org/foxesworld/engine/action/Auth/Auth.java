package org.foxesworld.engine.action.Auth;

import com.google.gson.Gson;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.action.server.ServerAttributes;
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

    private List<ServerAttributes> userServers;
    private Map<String, String> authCredentials = new HashMap<>();
    private Map<String, Object> CONFIG;
    private HTTPrequest POSTrequest;
    private Map<String, String> inputData = new HashMap<>();

    public Auth(Engine engine) {
        this.engine = engine;
        this.POSTrequest = engine.getPOSTrequest();
        this.CONFIG = engine.getCONFIG();
        //If we just initialised and are not sending a form
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

    public void formAuth(List<Component> authCredentials) {
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
            engine.setAuthorised(true);
            this.authCredentials = authCredentials;
            engine.getLOGGER().info(authCredentials.get("login") + " authorised!");
            this.loadUserServers();
            if (CONFIG.get("login") == null) {
                saveAuthCredentials(authCredentials);
            }
        } else {
            JOptionPane.showMessageDialog(engine.getFrame().getFrame(), response.message);
        }

        return status;
    }

    private void loadUserServers(){
        ServerParser serverParser = new ServerParser(getEngine());
        userServers = serverParser.parseServers(getAuthCredentials("login"));
        for(ServerAttributes serverAttributes: userServers){
            System.out.println(serverAttributes.serverName);
        }
    }

    private void saveAuthCredentials(Map<String, String> authCredentials) {
        engine.getConfig().addToConfig(authCredentials, Arrays.asList("login", "password"));
        engine.getConfig().writeCurrentConfig();
    }

    public String getAuthCredentials(String key) {
        return authCredentials.get(key);
    }

    public Engine getEngine() {
        return engine;
    }

    public List<ServerAttributes> getUserServers() {
        return userServers;
    }
}
