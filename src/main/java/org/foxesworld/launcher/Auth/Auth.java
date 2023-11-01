package org.foxesworld.launcher.Auth;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.foxesworld.engine.Engine;
import org.foxesworld.launcher.server.ServerAttributes;
import org.foxesworld.launcher.server.ServerParser;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class Auth {
    private final Engine engine;
    private List<ServerAttributes> userServersAttributes;
    private String[] userServersArray;
    private Map<String, String> authCredentials = new HashMap<>();
    private final Map<String, Object> CONFIG;
    private final HTTPrequest POSTrequest;
    private final Map<String, String> inputData = new HashMap<>();
    private boolean authorised = false;

    public Auth(Engine engine) {
        this.engine = engine;
        this.POSTrequest = engine.getPOSTrequest();
        this.CONFIG = engine.getCONFIG().getCONFIG();
        //If we just initialised and are not sending a form
        if (CONFIG.get("login") != null && CONFIG.get("password") != null) {
            Map<String, String> authCredentials = new HashMap<>();
            authCredentials.put("login", (String) CONFIG.get("login"));
            authCredentials.put("password", (String) CONFIG.get("password"));
            this.engine.getLOGGER().debug("Authorising with existing login " + CONFIG.get("login"));
            //Writing login data if it's not present
            if (!this.authorize(authCredentials)) {
                engine.getCONFIG().clearConfigData(Arrays.asList("login", "password"), true);
            }
        }
    }

    public void formAuth(List<Component> authCredentials) {
        for (Component component : authCredentials) {
            if (component instanceof JTextField) {
                inputData.put(component.getName(), ((JTextField) component).getText());
            } else {
                if(component instanceof JCheckBox) {
                    inputData.put(component.getName(), String.valueOf(((JCheckBox) component).isSelected()));
                }
            }

        }
        if(this.authorize(inputData)) {
            engine.getSOUND().playSound("auth.ogg");
        }
    }

    public boolean authorize(Map<String, String> authCredentials) {
        authCredentials.put("userAction", "auth");
        Map<String, Object> responseMap = new Gson().fromJson(this.POSTrequest.send(authCredentials), new TypeToken<Map<String, Object>>(){}.getType());
        boolean status = "success".equals(responseMap.get("type"));

        if (status) {
            setAuthorised(true);
            this.authCredentials = authCredentials;

            // Adding all sent data to MAP
            for (Map.Entry<String, Object> entry : responseMap.entrySet()) {
                authCredentials.put(entry.getKey(), entry.getValue().toString());
            }

            engine.getLOGGER().info(authCredentials.get("login") + " authorised!");
            this.loadUserServers();
            if (CONFIG.get("login") == null && "true".equals(authCredentials.get("rememberMe"))) {
                saveAuthCredentials(authCredentials);
            }
        } else {
            JOptionPane.showMessageDialog(engine.getFrame().getFrame(), responseMap.get("message"));
        }
        return status;
    }

    private void loadUserServers() {
        int i = 0;
        ServerParser serverParser = new ServerParser(getEngine());
        userServersAttributes = serverParser.parseServers(getAuthCredentials("login"));
        userServersArray = new String[serverParser.getServersNum()];
        for (ServerAttributes serverAttributes : userServersAttributes) {
            userServersArray[i] = serverAttributes.serverName;
            i++;
        }
    }

    private void saveAuthCredentials(Map<String, String> authCredentials) {
        engine.getCONFIG().addToConfig(authCredentials, Arrays.asList("login", "password"));
        engine.getCONFIG().writeCurrentConfig();
    }

    public String getAuthCredentials(String key) {
        return authCredentials.get(key);
    }

    public Map<String, String> getAuthCredentials() {
        return authCredentials;
    }

    public Engine getEngine() {
        return engine;
    }

    public String[] getUserServersArray() {
        if(isAuthorised()) {
            return userServersArray;
        } else {
            return new String[0];
        }
    }

    public List<ServerAttributes> getUserServersAttributes() {
        return userServersAttributes;
    }
    public boolean isAuthorised() {
        return authorised;
    }
    public void setAuthorised(boolean authorised) {
        this.authorised = authorised;
    }
}
