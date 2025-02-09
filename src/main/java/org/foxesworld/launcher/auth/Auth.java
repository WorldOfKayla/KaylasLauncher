package org.foxesworld.launcher.auth;

import com.google.gson.Gson;
import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.utils.Crypt.CryptUtils;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.launcher.config.Config;
import org.foxesworld.launcher.server.ServerParser;
import org.foxesworld.launcher.user.User;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Auth {
    private final Launcher launcher;
    private final Engine engine;
    private final Config config;
    private final CryptUtils cryptUtils;
    private final EncryptionKeyManager encryptionKeyManager;
    private AuthListener authListener;
    private Map<String, Object> authCredentials = new HashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> balanceMap = new ConcurrentHashMap<>();
    private List<ServerAttributes> userServersAttributes;
    private String[] userServersArray;
    private boolean authorised = false;

    public Auth(Launcher launcher) {
        this.launcher = launcher;
        this.engine = launcher.getEngine();
        this.config = launcher.getConfig();
        this.cryptUtils = launcher.getCRYPTO();
        this.encryptionKeyManager = new EncryptionKeyManager(this.engine);
        setAuthListener(new AuthListenerAdapter(this));
        attemptAutoLogin();
    }

    public void authTask(Map<String, Object> authCredentials) {
        this.launcher.getExecutorServiceProvider().submitTask(() -> {
            setAuthCredentials(authCredentials);
            try {
                if (!authorizeAsync().get()) {
                    config.clearConfigData(Arrays.asList("login", "password"), true);
                }
            } catch (InterruptedException | ExecutionException ignored) {}
        }, "auth");
    }

    public void formAuth(JComponent component) {
        FormAuth formAuth = new FormAuth(this);
        this.authCredentials = formAuth.getFormCredentials();
        try {
            if (authorizeAsync().get()) {

            } else {
                component.setEnabled(true);
            }
        } catch (InterruptedException | ExecutionException e) {
            Engine.getLOGGER().error("Error during form authorization", e);
            throw new RuntimeException(e);
        }

    }

    private void attemptAutoLogin() {
        String login = config.getLogin();
        String encryptedPassword = config.getPassword();

        if (login != null && encryptedPassword != null) {
            authCredentials.put("login", login);
            String decryptedPassword = cryptUtils.decrypt(encryptedPassword, encryptionKeyManager.getEncryptionKey(16));
            if (decryptedPassword != null) {
                authCredentials.put("password", decryptedPassword);
                Engine.getLOGGER().debug("Attempting auto login with saved credentials for: " + login);
                authListener.onAuthAttempt(this, authCredentials);
            } else {
                clearCredentials();
            }
        }
    }

    public CompletableFuture<Boolean> authorizeAsync() {
        String login = (String) authCredentials.get("login");
        String password = (String) authCredentials.get("password");

        AuthRequest authRequest = new AuthRequest(engine, login, password);
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        authRequest.sendAsync(
                Map.of(),
                response -> handleAuthResponse(response, future),
                error -> handleAuthError(error, future)
        );

        return future;
    }

    private void handleAuthResponse(Object response, CompletableFuture<Boolean> future) {
        try {
            AuthResponse authResponse = new Gson().fromJson(String.valueOf(response), AuthResponse.class);
            if ("success".equals(authResponse.getType())) {
                handleSuccessfulAuth(authResponse);
                //invokeHook("onAuthSuccess", authResponse);
                future.complete(true);
            } else {
                invokeHook("onAuthFailure", authResponse);
                future.complete(false);
            }
        } catch (Exception e) {
            Engine.getLOGGER().error("Error processing auth response", e);
            future.completeExceptionally(e);
        }
    }

    private void handleAuthError(Throwable error, CompletableFuture<Boolean> future) {
        Engine.getLOGGER().error("Authorization request failed: ", error);
        invokeHook("onAuthError", error);
        future.completeExceptionally(error);
    }

    private void handleSuccessfulAuth(AuthResponse authResponse) {
        setAuthorised(true);
        launcher.getSOUND().playSound("other", "loggedIn");
        authCredentials.put("uuid", authResponse.getUuid());
        authCredentials.put("token", authResponse.getToken());
        authCredentials.put("group", String.valueOf(authResponse.getGroup()));
        authCredentials.put("colorScheme", String.valueOf(authResponse.getColorScheme()));
        authCredentials.put("userFullName", String.valueOf(authResponse.getUserFullName()));
        updateBalance((authResponse).getBalance());

        Engine.getLOGGER().info(authResponse.getLogin() + " authorized!");
        loadUserServers(authResponse.getLogin());

        if ("true".equals(authCredentials.get("rememberMe"))) {
            saveAuthCredentials(authCredentials);
        }
        launcher.setUser(new User(launcher));
    }


    public void updateBalance(List<Map<String, Integer>> balance) {

        CompletableFuture.runAsync(() -> {
            try {
                balance.forEach(entry ->
                        entry.forEach((key, value) ->
                                balanceMap.compute(key, (k, v) -> {
                                    if (v == null) return new AtomicInteger(value);
                                    v.addAndGet(value);
                                    return v;
                                })
                        )
                );
                Engine.getLOGGER().info("Balance updated: " + balanceMap);
            } catch (Exception e) {
                Engine.getLOGGER().error("Error updating balance", e);
            } finally {
            }
        });
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
        config.writeCurrentConfig();
        invokeHook("onLogOut", null);
        engine.init();
    }

    private void saveAuthCredentials(Map<String, Object> credentials) {
        Map<String, Object> encryptedCredentials = new HashMap<>(credentials);
        String encryptedPassword = cryptUtils.encrypt(String.valueOf(credentials.get("password")), encryptionKeyManager.getEncryptionKey(16));
        encryptedCredentials.put("password", encryptedPassword);
        config.addToConfig(encryptedCredentials, Arrays.asList("login", "password"));
        config.writeCurrentConfig();
    }

    private void clearCredentials() {
        Arrays.asList("login", "password").forEach(key -> {
            authCredentials.remove(key);
            config.clearConfigData(key, true);
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

    public Map<String, AtomicInteger> getBalanceMap() {
        return balanceMap;
    }

    public void setAuthCredentials(Map<String, Object> authCredentials) {
        this.authCredentials = authCredentials;
    }

    private void invokeHook(String hookName, Object data) {
        if (authListener != null) {
            switch (hookName) {
                case "onAuthSuccess" -> authListener.onAuthSuccess(data);
                case "onAuthFailure" -> authListener.onAuthFailure(data);
                case "onAuthError" -> authListener.onAuthError(data);
                case "onLogOut" -> authListener.onLogOut(data);
                default -> Engine.getLOGGER().warn("Unknown hook: " + hookName);
            }
        }
    }
}
