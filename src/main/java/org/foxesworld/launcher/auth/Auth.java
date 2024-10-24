package org.foxesworld.launcher.auth;

import com.google.gson.Gson;
import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.utils.Crypt.CryptUtils;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.launcher.config.Config;
import org.foxesworld.launcher.server.ServerParser;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.*;

public class Auth {
    private final Launcher launcher;
    private AuthListener authListener;
    private final EncryptionKeyManager encryptionKeyManager;
    private final Map<String, Integer> balanceMap = new ConcurrentHashMap<>();
    private final Engine engine;
    private List<ServerAttributes> userServersAttributes;
    private String[] userServersArray;
    private Map<String, Object> authCredentials = new ConcurrentHashMap<>();
    private final Config CONFIG;
    private final HTTPrequest POSTrequest;
    private final CryptUtils cryptUtils;
    private boolean authorised = false;
    private final ExecutorService executorService;

    public Auth(Launcher launcher) {
        this.launcher = launcher;
        this.engine = launcher.getEngine();
        this.encryptionKeyManager = new EncryptionKeyManager(this.engine);
        this.POSTrequest = engine.getPOSTrequest();
        this.CONFIG = launcher.getConfig();
        this.cryptUtils = launcher.getCRYPTO();
        this.executorService = Executors.newFixedThreadPool(4); // Adjust thread pool size as needed
        setAuthListener(launcher);
        attemptAutoLogin();
    }

    private void attemptAutoLogin() {
        executorService.submit(() -> {
            try {
                String login = CONFIG.getLogin();
                String encryptedPassword = CONFIG.getPassword();

                if (login != null && encryptedPassword != null) {
                    authCredentials.put("login", login);
                    String decryptedPassword = cryptUtils.decrypt(encryptedPassword, encryptionKeyManager.getEncryptionKey(16));

                    if (decryptedPassword != null) {
                        authCredentials.put("password", decryptedPassword);
                        Engine.getLOGGER().debug("Attempting auto login with saved credentials for: " + login);

                        // Ensure authListener.onLoad is called before starting authorization
                        authListener.onLoad(this, authCredentials);

                        // Perform async authorization
                        authorizeAsync().thenAccept(success -> {
                            if (success) {
                                Engine.getLOGGER().info("Auto login successful for: " + login);
                            } else {
                                Engine.getLOGGER().warn("Auto login failed for: " + login);
                                clearCredentials();
                            }
                        }).exceptionally(ex -> {
                            Engine.getLOGGER().error("Auto login encountered an error: ", ex);
                            clearCredentials();
                            return null;
                        });
                    } else {
                        Engine.getLOGGER().warn("Decryption of password failed for auto login.");
                        clearCredentials();
                    }
                }
            } catch (Exception e) {
                Engine.getLOGGER().error("Exception during auto login: ", e);
                clearCredentials();
            }
        });
    }

    public void formAuth() {
        FormAuth formAuth = new FormAuth(this);
        this.authCredentials = formAuth.getFormCredentials();
        authorizeAsync().thenAcceptAsync(success -> {
            if (success) {
                engine.getSOUND().playSound("other", "loggedIn");
            }
        }, executorService).exceptionally(ex -> {
            Engine.getLOGGER().error("Form auth encountered an error: ", ex);
            throw new RuntimeException(ex);
        });
    }

    public CompletableFuture<Boolean> authorizeAsync() {
        this.authCredentials.put("userAction", "auth");
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        POSTrequest.sendAsync(this.authCredentials,
                response -> {
                    try {
                        AuthResponse authResponse = new Gson().fromJson(String.valueOf(response), AuthResponse.class);
                        if ("success".equals(authResponse.getType())) {
                            handleSuccessfulAuth(authResponse);
                            future.complete(true);
                        } else {
                            handleFailedAuth(authResponse);
                            future.complete(false);
                        }
                    } catch (Exception e) {
                        Engine.getLOGGER().error("Exception during authorization response handling: ", e);
                        future.completeExceptionally(e);
                    }
                },
                error -> {
                    Engine.getLOGGER().error("Authorization request failed: ", error);
                    future.completeExceptionally(error);
                }
        );

        return future;
    }

    private void handleSuccessfulAuth(AuthResponse authResponse) {
        setAuthorised(true);
        this.authCredentials.put("uuid", authResponse.getUuid());
        this.authCredentials.put("token", authResponse.getToken());
        this.authCredentials.put("group", String.valueOf(authResponse.getGroup()));
        this.authCredentials.put("groupName", String.valueOf(authResponse.getGroupName()));

        List<Map<String, Integer>> balance = authResponse.getBalance();
        if (balance != null) {
            balance.forEach(balanceMap::putAll);
        }

        Engine.getLOGGER().info(authResponse.getLogin() + " authorised!");
        loadUserServersAsync(authResponse.getLogin());

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

    private void loadUserServersAsync(String login) {
        CompletableFuture.runAsync(() -> {
            try {
                ServerParser serverParser = new ServerParser(engine);
                userServersAttributes = serverParser.parseServers(login);
                userServersArray = userServersAttributes.stream()
                        .map(serverAttributes -> serverAttributes.getServerName() + ' ' + serverAttributes.getServerVersion())
                        .toArray(String[]::new);
            } catch (Exception e) {
                Engine.getLOGGER().error("Error loading user servers: ", e);
            }
        }, executorService).thenRun(() -> {
            // Handle post server loading tasks if needed
        });
    }

    public void logOut() {
        Engine.getLOGGER().info("Logging out...");
        setAuthorised(false);
        executorService.submit(() -> {
            try {
                engine.getFrame().getRootPanel().removeAll();
                clearCredentials();
                launcher.getConfig().writeCurrentConfig();
                engine.init();
            } catch (Exception e) {
                Engine.getLOGGER().error("Error during logout: ", e);
            }
        });
    }

    private void saveAuthCredentials(Map<String, Object> authCredentials) {
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> encryptedCredentials = new HashMap<>(authCredentials);
                String encryptedPassword = cryptUtils.encrypt(String.valueOf(authCredentials.get("password")), encryptionKeyManager.getEncryptionKey(16));
                encryptedCredentials.put("password", encryptedPassword);
                launcher.getConfig().addToConfig(encryptedCredentials, Arrays.asList("login", "password"));
                launcher.getConfig().writeCurrentConfig();
            } catch (Exception e) {
                Engine.getLOGGER().error("Error saving auth credentials: ", e);
            }
        }, executorService);
    }

    private void clearCredentials() {
        Arrays.asList("login", "password").forEach(clear -> {
            authCredentials.remove(clear);
            launcher.getConfig().clearConfigData(clear, true);
        });
    }

    public String getAuthCredentials(String key) {
        return String.valueOf(authCredentials.get(key));
    }

    public Map<String, Object> getAuthCredentials() {
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

    public void setAuthCredentials(Map<String, Object> authCredentials) {
        this.authCredentials = authCredentials;
    }
}