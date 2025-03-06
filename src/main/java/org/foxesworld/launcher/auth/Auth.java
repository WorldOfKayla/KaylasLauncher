package org.foxesworld.launcher.auth;

import com.google.gson.Gson;
import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.sound.PlaybackStatusListener;
import org.foxesworld.engine.utils.Crypt.CryptUtils;
import org.foxesworld.launcher.config.Config;
import org.foxesworld.launcher.server.ServerParser;
import org.foxesworld.test.DataInjector;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Класс Auth отвечает за процесс аутентификации, управление сессией пользователя и загрузку связанных данных.
 * Используются универсальные инжекторы (DataInjector) для асинхронной установки и получения:
 * <ul>
 *     <li>Списка серверов (DataInjector<String[]>)</li>
 *     <li>Балансовых данных (DataInjector<ConcurrentHashMap<String, AtomicInteger>>)</li>
 * </ul>
 * Это позволяет подписанным компонентам получать актуальные данные сразу после их загрузки,
 * что особенно важно в многопоточной среде.
 */
public class Auth {
    private final Launcher launcher;
    private final Engine engine;
    private final Config config;
    private final CryptUtils cryptUtils;
    private final EncryptionKeyManager encryptionKeyManager;
    private Map<String, Object> authCredentials = new HashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> balanceMap = new ConcurrentHashMap<>();
    private List<ServerAttributes> userServersAttributes;
    private String[] userServersArray;
    private boolean authorised = false;

    // Инжекторы для уведомления подписчиков о загрузке данных
    private final DataInjector<String[]> userServersInjector = new DataInjector<>();
    private final DataInjector<ConcurrentHashMap<String, AtomicInteger>> balanceInjector = new DataInjector<>();

    private static final Gson GSON = new Gson();

    public Auth(Launcher launcher) {
        this.launcher = launcher;
        this.engine = launcher.getEngine();
        this.config = launcher.getConfig();
        this.cryptUtils = launcher.getCRYPTO();
        this.encryptionKeyManager = new EncryptionKeyManager(this.engine);
        attemptAutoLogin();
    }

    /**
     * Запускает асинхронную задачу аутентификации.
     *
     * @param authCredentials Учётные данные пользователя.
     */
    public void authTask(Map<String, Object> authCredentials) {
        launcher.getExecutorServiceProvider().submitTask(() -> {
            setAuthCredentials(authCredentials);
            try {
                if (!authorizeAsync().get()) {
                    config.clearConfigData(Arrays.asList("login", "password"), true);
                }
            } catch (InterruptedException | ExecutionException e) {
                Engine.getLOGGER().error("Authentication task interrupted", e);
                Thread.currentThread().interrupt();
            }
        }, "auth");
    }

    /**
     * Запускает аутентификацию через UI-компонент.
     *
     * @param component UI-компонент, который будет задействован.
     */
    public void formAuth(JComponent component) {
        FormAuth formAuth = new FormAuth(this);
        this.authCredentials = formAuth.getFormCredentials();
        try {
            if (!authorizeAsync().get()) {
                component.setEnabled(true);
            }
        } catch (InterruptedException | ExecutionException e) {
            Engine.getLOGGER().error("Error during form authorization", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Пытается выполнить автоматический вход, используя сохранённые учётные данные.
     */
    public void attemptAutoLogin() {
        String login = config.getLogin();
        String encryptedPassword = config.getPassword();
        if (login != null && encryptedPassword != null) {
            authCredentials.put("login", login);
            String decryptedPassword = cryptUtils.decrypt(encryptedPassword, encryptionKeyManager.getEncryptionKey(16));
            if (decryptedPassword != null) {
                authCredentials.put("password", decryptedPassword);
                Engine.getLOGGER().debug("Attempting auto login with saved credentials for: " + login);
                launcher.logStartupTime(launcher.getStartTime());
                authTask(authCredentials);
            } else {
                clearCredentials();
            }
        }
    }

    /**
     * Асинхронная авторизация с использованием CompletableFuture.
     *
     * @return CompletableFuture с результатом авторизации.
     */
    public CompletableFuture<Boolean> authorizeAsync() {
        String login = (String) authCredentials.get("login");
        String password = (String) authCredentials.get("password");
        AuthRequest authRequest = new AuthRequest(engine, login, password);
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        authRequest.sendAsync(Collections.emptyMap(),
                response -> handleAuthResponse(response, future),
                error -> handleAuthError(error, future)
        );
        return future;
    }

    /**
     * Обрабатывает успешный ответ аутентификации.
     *
     * @param response Ответ сервера.
     * @param future   CompletableFuture для завершения операции.
     */
    private void handleAuthResponse(Object response, CompletableFuture<Boolean> future) {
        try {
            AuthResponse authResponse = GSON.fromJson(String.valueOf(response), AuthResponse.class);
            if ("success".equals(authResponse.getType())) {
                handleSuccessfulAuth(authResponse);
                future.complete(true);
            } else {
                future.complete(false);
            }
        } catch (Exception e) {
            Engine.getLOGGER().error("Error processing auth response", e);
            future.completeExceptionally(e);
        }
    }

    /**
     * Обрабатывает ошибку при авторизации.
     *
     * @param error  Ошибка аутентификации.
     * @param future CompletableFuture для завершения операции.
     */
    private void handleAuthError(Throwable error, CompletableFuture<Boolean> future) {
        Engine.getLOGGER().error("Authorization request failed: ", error);
        launcher.getSOUND().playSound("other", "alert");
        future.completeExceptionally(error);
    }

    /**
     * Обрабатывает успешную авторизацию.
     *
     * @param authResponse Ответ сервера при успешной авторизации.
     */
    private void handleSuccessfulAuth(AuthResponse authResponse) {
        setAuthorised(true);
        launcher.getSOUND().playSound("other", "login", new PlaybackStatusListener() {
            @Override
            public void onPlaybackStarted(String s) {
                // No-op
            }

            @Override
            public void onPlaybackStopped(String s) {
                if (launcher.getConfig().isBackgroundMusic()) {
                    launcher.getSOUND().getSoundPlayer().onAllSoundsFinished(() ->
                            launcher.getSOUND().playSound("music", "launcherTheme", true));
                }
            }

            @Override
            public void onPlaybackProgress(String s, long l, long l1) {
                // No-op
            }
        });
        authCredentials.put("uuid", authResponse.getUuid());
        authCredentials.put("token", authResponse.getToken());
        authCredentials.put("group", String.valueOf(authResponse.getGroup()));
        authCredentials.put("colorScheme", String.valueOf(authResponse.getColorScheme()));
        authCredentials.put("userFullName", String.valueOf(authResponse.getUserFullName()));

        // Асинхронная загрузка серверов и обновление баланса
        loadUserServers(authResponse.getLogin());
        updateBalance(authResponse.getBalance());

        Engine.getLOGGER().info(authResponse.getLogin() + " authorized!");
        if ("true".equals(authCredentials.get("rememberMe"))) {
            saveAuthCredentials(authCredentials);
        }
    }

    /**
     * Обновляет баланс, используя данные с сервера.
     * После обновления balanceMap вызывается инжектор, уведомляющий подписчиков о готовности актуальных данных.
     *
     * @param balance Список балансов для обновления.
     */
    public void updateBalance(List<Map<String, Integer>> balance) {
        launcher.getExecutorServiceProvider().submitTask(() -> {
            try {
                balance.forEach(entry ->
                        entry.forEach((key, value) ->
                                balanceMap.compute(key, (k, v) -> v == null ? new AtomicInteger(value) : new AtomicInteger(v.addAndGet(value)))
                        )
                );
                Engine.getLOGGER().info("Balance updated: " + balanceMap);
                balanceInjector.setContent(balanceMap);
            } catch (Exception e) {
                Engine.getLOGGER().error("Error updating balance", e);
            }
        }, "updateBalance");
    }

    /**
     * Загружает список серверов для пользователя.
     * После загрузки серверов вызывается инжектор, уведомляющий подписчиков о готовности данных.
     *
     * @param login Логин пользователя.
     */
    public void loadUserServers(String login) {
        if (login == null || login.isEmpty()) {
            Engine.getLOGGER().warn("Empty login provided, aborting loadUserServers.");
            return;
        }
        ServerParser serverParser = new ServerParser(engine);
        userServersAttributes = serverParser.parseServers(login);
        userServersArray = userServersAttributes.stream()
                .map(sa -> sa.getServerName() + " " + sa.getServerVersion())
                .toArray(String[]::new);
        Engine.getLOGGER().info("Loaded {} servers", serverParser.getServersNum());
        // Уведомляем подписчиков через инжектор
        userServersInjector.setContent(userServersArray);
    }

    /**
     * Альтернативный метод загрузки серверов с использованием DataInjector для списка ServerAttributes.
     *
     * @param login           Логин пользователя.
     * @param serversInjector DataInjector, который будет уведомлён после загрузки серверов.
     */
    public void loadUserServers(String login, DataInjector<List<ServerAttributes>> serversInjector) {
        if (login == null || login.isEmpty()) {
            Engine.getLOGGER().warn("Empty login provided, aborting loadUserServers.");
            return;
        }
        ServerParser serverParser = new ServerParser(engine);
        List<ServerAttributes> loadedServers = serverParser.parseServers(login);
        Engine.getLOGGER().info("Loaded {} servers", loadedServers.size());
        // Уведомляем подписчиков через переданный инжектор
        serversInjector.setContent(loadedServers);
    }

    /**
     * Метод для выхода из системы.
     */
    public void logOut() {
        Engine.getLOGGER().info("Logging out...");
        setAuthorised(false);
        engine.getFrame().getRootPanel().removeAll();
        clearCredentials();
        config.writeCurrentConfig();
        //Engine.LOGGER.debug("LOGOUT");
        engine.init();
    }

    /**
     * Сохраняет зашифрованные учётные данные.
     *
     * @param credentials Учётные данные пользователя.
     */
    private void saveAuthCredentials(Map<String, Object> credentials) {
        Map<String, Object> encryptedCredentials = new HashMap<>(credentials);
        String encryptedPassword = cryptUtils.encrypt(
                String.valueOf(credentials.get("password")),
                encryptionKeyManager.getEncryptionKey(16)
        );
        encryptedCredentials.put("password", encryptedPassword);
        config.addToConfig(encryptedCredentials, Arrays.asList("login", "password"));
        config.writeCurrentConfig();
    }

    /**
     * Очищает учётные данные.
     */
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

    /**
     * Возвращает массив серверов, если пользователь авторизован, иначе – пустой массив.
     *
     * @return Массив серверов или пустой массив.
     */
    public String[] getUserServersArray() {
        return authorised ? userServersArray : new String[0];
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

    public Map<String, AtomicInteger> getBalanceMap() {
        return balanceMap;
    }

    public void setAuthCredentials(Map<String, Object> authCredentials) {
        this.authCredentials = authCredentials;
    }

    /**
     * Геттер для универсального инжектора серверов.
     *
     * @return DataInjector для массива серверов.
     */
    public DataInjector<String[]> getUserServersInjector() {
        return userServersInjector;
    }

    /**
     * Геттер для универсального инжектора баланса.
     *
     * @return DataInjector для данных баланса.
     */
    public DataInjector<ConcurrentHashMap<String, AtomicInteger>> getBalanceInjector() {
        return balanceInjector;
    }
}
