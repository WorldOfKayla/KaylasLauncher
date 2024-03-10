package org.foxesworld.launcher.auth;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.config.Config;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.launcher.server.ServerParser;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Auth {
    private final Launcher launcher;
    private AuthListener authListener;
    private final Engine engine;
    private List<ServerAttributes> userServersAttributes;
    private String[] userServersArray;
    private final Map<String, String> authCredentials = new HashMap<>();
    private final Config CONFIG;
    private final HTTPrequest POSTrequest;
    private final Map<String, String> inputData = new HashMap<>();
    private boolean authorised = false;

    public Auth(Launcher launcher) {
        this.launcher = launcher;
        this.engine = launcher.getEngine();
        this.POSTrequest = engine.getPOSTrequest();
        this.CONFIG = engine.getCONFIG();
        setAuthListener(launcher);
        attemptAutoLogin();
    }

    private void attemptAutoLogin() {
        if (CONFIG.getLogin() != null && CONFIG.getPassword() != null) {
            authCredentials.put("login", CONFIG.getLogin());
            authCredentials.put("password", CONFIG.getPassword());
            Engine.getLOGGER().debug("Attempting auto login with saved credentials for: " + CONFIG.getLogin());
            authListener.onLoad(this, authCredentials);
        }
    }

    public void formAuth(List<JComponent> authCredentials) {
        for (Component component : authCredentials) {
            if (component instanceof JTextField) {
                inputData.put(component.getName(), ((JTextField) component).getText());
            } else if (component instanceof JCheckBox) {
                inputData.put(component.getName(), String.valueOf(((JCheckBox) component).isSelected()));
            }
        }
        if (authorize(inputData)) {
            engine.getSOUND().playSound("other", "loggedIn");
        }
    }

    public boolean authorize(Map<String, String> authCredentials) {
        authCredentials.put("userAction", "auth");
        String response = POSTrequest.send(engine.getEngineData().getBindUrl(), authCredentials);
        Map<String, Object> responseMap = new Gson().fromJson(response, new TypeToken<Map<String, Object>>(){}.getType());
        boolean status = "success".equals(responseMap.get("type"));

        if (status) {
            handleSuccessfulAuth(responseMap, authCredentials);
        } else {
            handleFailedAuth(responseMap);
        }
        return status;
    }

    private void handleSuccessfulAuth(Map<String, Object> responseMap, Map<String, String> authCredentials) {
        setAuthorised(true);
        this.authCredentials.putAll(authCredentials);
        for (Map.Entry<String, Object> entry : responseMap.entrySet()) {
            authCredentials.put(entry.getKey(), entry.getValue().toString());
        }
        Engine.getLOGGER().info(authCredentials.get("login") + " authorised!");
        loadUserServers(authCredentials.get("login"));
        if (CONFIG.getLogin() == null && "true".equals(authCredentials.get("rememberMe"))) {
            saveAuthCredentials(authCredentials);
        }
        authListener.onLogin(authCredentials);
    }

    private void handleFailedAuth(Map<String, Object> responseMap) {
        Engine.getLOGGER().info("Incorrect password for " + authCredentials.get("login") + "!");
        JOptionPane.showMessageDialog(engine.getFrame(), responseMap.get("message"));
    }

    private void loadUserServers(String login) {
        ServerParser serverParser = new ServerParser(getEngine());
        userServersAttributes = serverParser.parseServers(login);
        userServersArray = userServersAttributes.stream()
                .map(serverAttributes -> serverAttributes.getServerName() + ' ' + serverAttributes.getServerVersion()).toArray(String[]::new);
    }

    public void logOut() {
        Engine.getLOGGER().info("Logging out...");
        setAuthorised(false);
        engine.getFrame().getRootPanel().removeAll();
        Arrays.asList("login", "password").forEach(clear -> {
            authCredentials.remove(clear);
            engine.getCONFIG().clearConfigData(clear, true);
        });
        engine.initialize(launcher);
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
        return authorised ? userServersArray : new String[0];
    }

    public List<ServerAttributes> getUserServersAttributes() {
        return userServersAttributes;
    }

    public boolean isAuthorised() {
        return authorised;
    }

    public void setAuthListener(AuthListener authListener) {
        this.authListener = authListener;
    }

    public void setAuthorised(boolean authorised) {
        this.authorised = authorised;
    }
}
