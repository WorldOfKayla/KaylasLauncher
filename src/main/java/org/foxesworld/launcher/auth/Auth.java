package org.foxesworld.launcher.auth;

import com.google.gson.Gson;
import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.sound.PlaybackStatusListener;
import org.foxesworld.engine.utils.Crypt.CryptUtils;
import org.foxesworld.launcher.config.Config;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Auth {
    private final Launcher launcher;
    private final UserDataLoader userDataLoader;
    private final Engine engine;
    private final Config config;
    private final CryptUtils cryptUtils;
    private final EncryptionKeyManager encryptionKeyManager;
    private Map<String, Object> authCredentials = new HashMap<>();
    private AuthStatus authStatus = AuthStatus.UNAUTHORISED;
    private static final Gson GSON = new Gson();
    private AuthResponse authResponse;
    private AuthRequest authRequest;

    public Auth(Launcher launcher) {
        this.launcher = launcher;
        this.engine = launcher.getEngine();
        this.config = launcher.getConfig();
        this.cryptUtils = launcher.getCRYPTO();
        this.encryptionKeyManager = new EncryptionKeyManager(this.engine);
        this.userDataLoader = new UserDataLoader(engine);
        attemptAutoLogin();
    }

    /**
     * Выполняет задачу авторизации с заданной задачей после завершения.
     */
    public void authTask(final Map<String, Object> authCredentials, Runnable onCompletion) {
        launcher.getExecutorServiceProvider().submitTask(() -> {
            setAuthCredentials(authCredentials);
            try {
                if (!authorizeAsync(onCompletion).get()) {
                    config.clearConfigData(Arrays.asList("login", "password"), true);
                }
            } catch (InterruptedException | ExecutionException e) {
                Engine.getLOGGER().error("Ошибка выполнения задачи авторизации", e);
                Thread.currentThread().interrupt();
            }
        }, "auth");
    }

    /**
     * Выполняет авторизацию через UI и выполняет переданную задачу после завершения.
     */
    public void formAuth(final JComponent component, Runnable onCompletion) {
        FormAuth formAuth = new FormAuth(this);
        this.authCredentials = formAuth.getFormCredentials();
        try {
            if (!authorizeAsync(onCompletion).get()) {
                component.setEnabled(true);
            }
        } catch (InterruptedException | ExecutionException e) {
            Engine.getLOGGER().error("Ошибка формы авторизации", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Пытается выполнить автоматический вход с использованием сохранённых учётных данных.
     */
    public void attemptAutoLogin() {
        final String login = config.getLogin();
        final String encryptedPassword = config.getPassword();
        if (login != null && encryptedPassword != null) {
            this.setAuthStatus(AuthStatus.PENDING);
            authCredentials.put("login", login);
            final String decryptedPassword = cryptUtils.decrypt(encryptedPassword, encryptionKeyManager.getEncryptionKey(16));
            if (decryptedPassword != null) {
                authCredentials.put("password", decryptedPassword);
                Engine.getLOGGER().debug("Attempting auto login with saved credentials for: " + login);
                authTask(authCredentials, () -> {});
            } else {
                clearCredentials();
            }
        }
    }

    /**
     * Выполняет асинхронную авторизацию, используя sendAsyncCF для получения CompletableFuture.
     *
     * @param onCompletion Действие, выполняемое после завершения аутентификации (опционально).
     * @return CompletableFuture с результатом авторизации.
     */
    public CompletableFuture<Boolean> authorizeAsync(Runnable onCompletion) {
        final String login = (String) authCredentials.get("login");
        final String password = (String) authCredentials.get("password");
        authRequest = new AuthRequest(engine, login, password);

        return authRequest.sendAsyncCF(Collections.emptyMap())
                .thenCompose(response -> {
                    try {
                        authResponse = GSON.fromJson(String.valueOf(response), AuthResponse.class);
                        if ("success".equals(authResponse.getType())) {
                            return loadUserDataCF(authResponse.getLogin(), authResponse.getBalance())
                                    .thenApply(v -> {
                                        handleSuccessfulAuth(authResponse);
                                        if (onCompletion != null) {
                                            onCompletion.run();
                                        }
                                        return true;
                                    });
                        } else {
                            launcher.getSOUND().playSound("other", "alert");
                            launcher.showDialog(authResponse.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE, false);
                            if (onCompletion != null) {
                                onCompletion.run();
                            }
                            return CompletableFuture.completedFuture(false);
                        }
                    } catch (Exception e) {
                        Engine.getLOGGER().error("Ошибка обработки ответа авторизации", e);
                        if (onCompletion != null) {
                            onCompletion.run();
                        }
                        return CompletableFuture.completedFuture(false);
                    }
                })
                .exceptionally(error -> {
                    Engine.getLOGGER().error("Ошибка авторизации: ", error);
                    launcher.getSOUND().playSound("other", "alert");
                    if (onCompletion != null) {
                        onCompletion.run();
                    }
                    return false;
                });
    }

    /**
     * Вспомогательный метод для обёртки loadUserData с callback в CompletableFuture.
     */
    private CompletableFuture<Void> loadUserDataCF(String login, List<Map<String, Integer>> balance) {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        userDataLoader.loadUserData(login, balance, () -> cf.complete(null));
        return cf;
    }

    /**
     * Обработка успешной авторизации.
     */
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

        // Сохранение дополнительных данных авторизации
        authCredentials.put("uuid", authResponse.getUuid());
        authCredentials.put("token", authResponse.getToken());
        authCredentials.put("group", String.valueOf(authResponse.getGroup()));
        authCredentials.put("colorScheme", String.valueOf(authResponse.getColorScheme()));
        authCredentials.put("userFullName", String.valueOf(authResponse.getUserFullName()));

        Engine.getLOGGER().info(authResponse.getLogin() + " authorized!");
        if ("true".equals(authCredentials.get("rememberMe"))) {
            saveAuthCredentials(authCredentials);
        }
        launcher.getFrame().repaint();
    }

    /**
     * Выполняет выход из системы.
     */
    public void logOut() {
        Engine.getLOGGER().info("Logging out...");
        setAuthStatus(AuthStatus.UNAUTHORISED);
        engine.getFrame().getRootPanel().removeAll();
        clearCredentials();
        config.writeCurrentConfig();
        engine.init();
    }

    /**
     * Сохраняет зашифрованные учётные данные пользователя.
     */
    private void saveAuthCredentials(final Map<String, Object> credentials) {
        Map<String, Object> encryptedCredentials = new HashMap<>(credentials);
        final String encryptedPassword = cryptUtils.encrypt(
                String.valueOf(credentials.get("password")),
                encryptionKeyManager.getEncryptionKey(16)
        );
        encryptedCredentials.put("password", encryptedPassword);
        config.addToConfig(encryptedCredentials, Arrays.asList("login", "password"));
        config.writeCurrentConfig();
    }

    /**
     * Очищает учётные данные пользователя.
     */
    private void clearCredentials() {
        Arrays.asList("login", "password").forEach(key -> {
            authCredentials.remove(key);
            config.clearConfigData(key, true);
        });
    }

    public String getAuthCredentials(final String key) {
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
        this.authCredentials = authCredentials;
    }

    public AuthRequest getAuthRequest() {
        return authRequest;
    }
}
