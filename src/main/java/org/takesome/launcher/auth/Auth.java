package org.takesome.launcher.auth;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.sound.PlaybackStatusListener;
import org.takesome.kaylasEngine.utils.Crypt.CryptUtils;
import org.takesome.launcher.config.Config;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Auth {
    private final Launcher launcher;
    private final UserDataLoader userDataLoader;
    private final Engine engine;
    private final Config config;
    private final CryptUtils cryptUtils;
    private final EncryptionKeyManager encryptionKeyManager;
    private final Map<AuthProviderType, AuthProvider> authProviders = new EnumMap<>(AuthProviderType.class);

    private Map<String, Object> authCredentials = new HashMap<>();
    private AuthStatus authStatus = AuthStatus.UNAUTHORISED;
    private AuthResponse authResponse;
    private AuthRequest authRequest;

    public Auth(Launcher launcher) {
        this.launcher = launcher;
        this.engine = launcher.getEngine();
        this.config = launcher.getConfig();
        this.cryptUtils = launcher.getCRYPTO();
        this.encryptionKeyManager = new EncryptionKeyManager(this.engine);
        this.userDataLoader = new UserDataLoader(engine);
        registerDefaultProviders();
        attemptAutoLogin();
    }

    private void registerDefaultProviders() {
        registerProvider(new NoPasswordAuthProvider());
        registerProvider(new WebApiAuthProvider());
        registerProvider(new WsAuthProvider());
    }

    public void registerProvider(AuthProvider provider) {
        authProviders.put(provider.type(), provider);
    }

    public void authTask(final Map<String, Object> authCredentials, Runnable onCompletion) {
        launcher.getExecutorServiceProvider().submitTask(() -> {
            setAuthCredentials(new HashMap<>(authCredentials));
            AuthProviderType providerType = resolveProviderType(this.authCredentials);
            try {
                boolean authorised = authorizeAsync(onCompletion).get();
                if (!authorised && shouldClearCredentialsAfterFailure(providerType)) {
                    config.clearConfigData(Arrays.asList("login", "password"), true);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Engine.getLOGGER().error("Authorization task was interrupted", e);
            } catch (ExecutionException e) {
                Engine.getLOGGER().error("Authorization task failed", e);
            }
        }, "auth");
    }

    public void formAuth(final JComponent component, Runnable onCompletion) {
        FormAuth formAuth = new FormAuth(this);
        this.authCredentials = new HashMap<>(formAuth.getFormCredentials());
        try {
            if (!authorizeAsync(onCompletion).get()) {
                if (component != null) {
                    if (component != null) {
                component.setEnabled(true);
            }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Engine.getLOGGER().error("Authorization form was interrupted", e);
            if (component != null) {
                component.setEnabled(true);
            }
        } catch (ExecutionException e) {
            Engine.getLOGGER().error("Authorization form failed", e);
            if (component != null) {
                component.setEnabled(true);
            }
        }
    }

    public void attemptAutoLogin() {
        final String login = config.getLogin();
        final String encryptedPassword = config.getPassword();
        AuthProviderType providerType = resolveProviderType(Map.of("authProvider", config.getAuthProvider()));

        if (login == null || login.isBlank()) {
            return;
        }

        this.setAuthStatus(AuthStatus.PENDING);
        authCredentials.put("login", login);
        authCredentials.put("authProvider", providerType.name());
        authCredentials.put("autoLogin", true);

        if (providerType == AuthProviderType.NO_PASSWORD) {
            Engine.getLOGGER().debug("Attempting no-password auto login for: {}", login);
            authTask(authCredentials, () -> {});
            return;
        }

        if (encryptedPassword == null || encryptedPassword.isBlank()) {
            clearCredentials();
            return;
        }

        final String decryptedPassword = cryptUtils.decrypt(encryptedPassword, encryptionKeyManager.getEncryptionKey(16));
        if (decryptedPassword != null) {
            authCredentials.put("password", decryptedPassword);
            Engine.getLOGGER().debug("Attempting {} auto login for: {}", providerType, login);
            authTask(authCredentials, () -> {});
        } else {
            clearCredentials();
        }
    }

    public CompletableFuture<Boolean> authorizeAsync(Runnable onCompletion) {
        AuthProviderType providerType = resolveProviderType(authCredentials);
        AuthProvider provider = authProviders.get(providerType);
        if (provider == null) {
            return CompletableFuture.completedFuture(false);
        }

        authCredentials.put("authProvider", providerType.name());
        setAuthStatus(AuthStatus.PENDING);
        AuthProviderContext context = new AuthProviderContext(this, launcher, engine, config, authCredentials);

        Engine.getLOGGER().info("Authorizing via {} provider", providerType);
        return provider.authorize(context)
                .thenCompose(response -> handleProviderResponse(response, onCompletion))
                .exceptionally(error -> {
                    setAuthStatus(AuthStatus.UNAUTHORISED);
                    Engine.getLOGGER().error("Authorization error via {} provider", providerType, error);
                    launcher.getSOUND().playSound("other", "alert");
                    if (onCompletion != null) {
                        onCompletion.run();
                    }
                    return false;
                });
    }

    private CompletableFuture<Boolean> handleProviderResponse(AuthResponse response, Runnable onCompletion) {
        this.authResponse = response;
        if (response != null && "success".equals(response.getType())) {
            return loadUserDataCF(response)
                    .thenApply(v -> {
                        handleSuccessfulAuth(response);
                        if (onCompletion != null) {
                            onCompletion.run();
                        }
                        return true;
                    });
        }

        setAuthStatus(AuthStatus.UNAUTHORISED);
        String message = response == null ? "Authorization provider returned empty response." : response.getMessage();
        if (isSilentTransientAutoLoginFailure(response)) {
            Engine.getLOGGER().warn("Auto login postponed/failed without clearing stored credentials: {}", message);
            if (onCompletion != null) {
                onCompletion.run();
            }
            return CompletableFuture.completedFuture(false);
        }

        launcher.getSOUND().playSound("other", "alert");
        launcher.showDialog(message, "Ошибка", JOptionPane.ERROR_MESSAGE, false);
        if (onCompletion != null) {
            onCompletion.run();
        }
        return CompletableFuture.completedFuture(false);
    }

    private CompletableFuture<Void> loadUserDataCF(AuthResponse response) {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        if (response.isBackendManaged()) {
            userDataLoader.loadBackendUserData(response.getLogin(), () -> cf.complete(null));
            return cf;
        }
        userDataLoader.loadUserData(response.getLogin(), response.getBalance(), () -> cf.complete(null));
        return cf;
    }

    private void handleSuccessfulAuth(final AuthResponse authResponse) {
        setAuthStatus(AuthStatus.AUTHORISED);
        this.launcher.getFrame().repaint();
        launcher.getSOUND().playSound("other", "login", new PlaybackStatusListener() {
            @Override
            public void onPlaybackStarted(String s) { }

            @Override
            public void onPlaybackStopped(String s) {
                if (launcher.getConfig().isBackgroundMusic()) {
                    launcher.getSOUND().getSoundPlayer().onAllSoundsFinished(() -> launcher.getSOUND().playSound("music", "launcherTheme", true));
                }
            }

            @Override
            public void onPlaybackProgress(String s, long l, long l1) { }
        });

        authCredentials.put("uuid", authResponse.getUuid());
        authCredentials.put("token", authResponse.getToken());
        authCredentials.put("login", authResponse.getLogin());
        authCredentials.put("group", String.valueOf(authResponse.getGroup()));
        authCredentials.put("colorScheme", String.valueOf(authResponse.getColorScheme()));
        authCredentials.put("userFullName", String.valueOf(authResponse.getUserFullName()));

        Engine.getLOGGER().info(authResponse.getLogin() + " authorized!");
        if ("true".equals(String.valueOf(authCredentials.get("rememberMe")))) {
            saveAuthCredentials(authCredentials);
        }
        launcher.getFrame().repaint();
    }

    public void logOut() {
        Engine.getLOGGER().info("Logging out...");
        setAuthStatus(AuthStatus.UNAUTHORISED);
        engine.getFrame().getRootPanel().removeAll();
        clearCredentials();
        config.writeCurrentConfig();
        engine.init();
    }

    private void saveAuthCredentials(final Map<String, Object> credentials) {
        Map<String, Object> storedCredentials = new HashMap<>(credentials);
        Object password = credentials.get("password");
        if (password != null && !String.valueOf(password).isBlank()) {
            final String encryptedPassword = cryptUtils.encrypt(
                    String.valueOf(password),
                    encryptionKeyManager.getEncryptionKey(16)
            );
            storedCredentials.put("password", encryptedPassword);
        } else {
            storedCredentials.remove("password");
        }
        config.addToConfig(storedCredentials, Arrays.asList("login", "password", "authProvider"));
        config.writeCurrentConfig();
    }

    private void clearCredentials() {
        Arrays.asList("login", "password").forEach(key -> {
            authCredentials.remove(key);
            config.clearConfigData(key, true);
        });
    }

    private boolean shouldClearCredentialsAfterFailure(AuthProviderType providerType) {
        if (providerType == AuthProviderType.NO_PASSWORD) {
            return false;
        }
        return authResponse == null || authResponse.shouldClearStoredCredentialsOnFailure();
    }

    private boolean isSilentTransientAutoLoginFailure(AuthResponse response) {
        return Boolean.parseBoolean(String.valueOf(authCredentials.get("autoLogin")))
                && response != null
                && !response.shouldClearStoredCredentialsOnFailure();
    }

    private AuthProviderType resolveProviderType(Map<String, Object> credentials) {
        Object provider = credentials.get("authProvider");
        if (provider == null) {
            provider = credentials.get("authProviderType");
        }
        if (provider == null) {
            provider = credentials.get("provider");
        }
        if (provider == null) {
            provider = config.getAuthProvider();
        }

        AuthProviderType resolved = AuthProviderType.from(provider);
        if (resolved != AuthProviderType.NO_PASSWORD && config.isBackendBinding()) {
            return AuthProviderType.WS;
        }
        return resolved;
    }

    public String getAuthCredentials(final String key) {
        Object value = authCredentials.get(key);
        return value == null ? null : String.valueOf(value);
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

    public UserDataLoader getUserDataLoader() {
        return userDataLoader;
    }

    public AuthStatus getAuthStatus() {
        return authStatus;
    }

    public void setAuthStatus(final AuthStatus status) {
        this.authStatus = status;
    }

    public void setAuthCredentials(final Map<String, Object> authCredentials) {
        this.authCredentials = authCredentials == null ? new HashMap<>() : authCredentials;
    }

    public AuthResponse getAuthResponse() {
        return authResponse;
    }

    public AuthRequest getAuthRequest() {
        return authRequest;
    }

    public void setAuthRequest(AuthRequest authRequest) {
        this.authRequest = authRequest;
    }
}
