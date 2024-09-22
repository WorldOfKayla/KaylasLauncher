package org.foxesworld.launcher.auth;

import com.google.gson.Gson;
import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.componentAccessor.ComponentsAccessor;
import org.foxesworld.engine.gui.components.passfield.PassField;
import org.foxesworld.engine.gui.components.textfield.TextField;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.utils.Crypt.CryptUtils;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.launcher.config.Config;
import org.foxesworld.launcher.server.ServerParser;

import javax.swing.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Auth {
    private final Launcher launcher;
    private AuthListener authListener;
    private final EncryptionKeyManager encryptionKeyManager;
    private final Map<String, Integer> balanceMap = new HashMap<>();
    private final Engine engine;
    private List<ServerAttributes> userServersAttributes;
    private String[] userServersArray;
    private Map<String, String> authCredentials = new HashMap<>();
    private final Config CONFIG;
    private final HTTPrequest POSTrequest;
    private final CryptUtils cryptUtils;
    private boolean authorised = false;

    public Auth(Launcher launcher) {
        this.launcher = launcher;
        this.engine = launcher.getEngine();
        this.encryptionKeyManager = new EncryptionKeyManager(this.engine);
        this.POSTrequest = engine.getPOSTrequest();
        this.CONFIG = launcher.getConfig();
        this.cryptUtils = launcher.getCRYPTO();
        setAuthListener(launcher);
        attemptAutoLogin();
    }

    private void attemptAutoLogin() {
        if (CONFIG.getLogin() != null && CONFIG.getPassword() != null) {
            authCredentials.put("login", CONFIG.getLogin());
            String encryptedPassword = CONFIG.getPassword();
            String decryptedPassword = cryptUtils.decrypt(encryptedPassword, encryptionKeyManager.getEncryptionKey(16));
            if (decryptedPassword != null) {
                authCredentials.put("password", decryptedPassword);
                Engine.getLOGGER().debug("Attempting auto login with saved credentials for: " + CONFIG.getLogin());
                authListener.onLoad(this, authCredentials);
            } else {
                clearCredentials();
            }
        }
    }
    public void formAuth() {
        FormAuth formAuth = new FormAuth(this);
        this.authCredentials = formAuth.getFormCredentials();
        if (authorize()) {
            engine.getSOUND().playSound("other", "loggedIn");
        }
    }
    public boolean authorize() {
        this.authCredentials.put("userAction", "auth");
        String response = POSTrequest.send(this.authCredentials);
        AuthResponse authResponse = new Gson().fromJson(response, AuthResponse.class);
        if ("success".equals(authResponse.getType())) {
            handleSuccessfulAuth(authResponse);
            return true;
        } else {
            handleFailedAuth(authResponse);
            return false;
        }
    }
    private void handleSuccessfulAuth(AuthResponse authResponse) {
        setAuthorised(true);
        //this.authCredentials.putAll(authCredentials);
        this.authCredentials.put("uuid", authResponse.getUuid());
        this.authCredentials.put("token", authResponse.getToken());
        this.authCredentials.put("group", String.valueOf(authResponse.getGroup()));

        List<Map<String, Integer>> balance = authResponse.getBalance();
        if (balance != null) {
            for (Map<String, Integer> balanceEntry : balance) {
                balanceMap.putAll(balanceEntry);
            }
        }

        Engine.getLOGGER().info(authResponse.getLogin() + " authorised!");
        loadUserServers(authResponse.getLogin());

        if ("true".equals(authCredentials.get("rememberMe"))) {
            saveAuthCredentials(authCredentials);
        }
        authListener.onLogin(authCredentials);
    }

    private void handleFailedAuth(AuthResponse authResponse) {
        Engine.getLOGGER().info("Incorrect password for " + authCredentials.get("login") + "!");
        this.launcher.getSOUND().playSound("other", "alert");
        this.launcher.showDialog(authResponse.getMessage(), this.launcher.getLANG().getString("auth.authTitle"), JOptionPane.WARNING_MESSAGE, false);
    }

    private void loadUserServers(String login) {
        ServerParser serverParser = new ServerParser(engine);
        userServersAttributes = serverParser.parseServers(login);
        userServersArray = userServersAttributes.stream()
                .map(serverAttributes -> serverAttributes.getServerName() + ' ' + serverAttributes.getServerVersion())
                .toArray(String[]::new);
    }

    public void logOut() {
        Engine.getLOGGER().info("Logging out...");
        setAuthorised(false);
        engine.getFrame().getRootPanel().removeAll();
        clearCredentials();
        engine.init();
    }

    static class FormAuth extends ComponentsAccessor {
        public FormAuth(Auth auth) {
            super(auth.getEngine().getGuiBuilder(), "authForm", Arrays.asList(TextField.class, PassField.class, JCheckBox.class));
        }
    }

    private void saveAuthCredentials(Map<String, String> authCredentials) {
        Map<String, String> encryptedCredentials = new HashMap<>(authCredentials);
        String encryptedPassword = cryptUtils.encrypt(authCredentials.get("password"), encryptionKeyManager.getEncryptionKey(16));
        encryptedCredentials.put("password", encryptedPassword);
        launcher.getConfig().addToConfig(encryptedCredentials, Arrays.asList("login", "password"));
        launcher.getConfig().writeCurrentConfig();
    }

    private void clearCredentials() {
        Arrays.asList("login", "password").forEach(clear -> {
            authCredentials.remove(clear);
            launcher.getConfig().clearConfigData(clear, true);
        });
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
    public Launcher getLauncher() {
        return launcher;
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

    public Map<String, Integer> getBalanceMap() {
        return balanceMap;
    }

    public void setAuthCredentials(Map<String, String> authCredentials) {
        this.authCredentials = authCredentials;
    }
}