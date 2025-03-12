package org.foxesworld.launcher.auth;

import com.google.gson.Gson;
import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.sound.PlaybackStatusListener;
import org.foxesworld.engine.utils.Crypt.CryptUtils;
import org.foxesworld.engine.utils.DataInjector;
import org.foxesworld.launcher.config.Config;
import org.foxesworld.launcher.server.ServerParser;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Auth class is responsible for the authentication process, managing the user's session,
 * and loading related data.
 * Universal injectors (DataInjector) are used for asynchronous setting and retrieval of:
 * <ul>
 *     <li>List of servers (DataInjector&lt;String[]&gt;)</li>
 *     <li>Balance data (DataInjector&lt;ConcurrentHashMap&lt;String, AtomicInteger&gt;&gt;)</li>
 * </ul>
 * This allows subscribed components to receive up-to-date data immediately after loading,
 * which is especially important in a multi-threaded environment.
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
     * Starts an asynchronous authentication task.
     *
     * @param authCredentials The user's credentials.
     */
    public void authTask(final Map<String, Object> authCredentials) {
        launcher.getExecutorServiceProvider().submitTask(() -> {
            setAuthCredentials(authCredentials);
            try {
                // Execute asynchronous authentication
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
     * Initiates authentication via a UI component.
     *
     * @param component The UI component to be used.
     */
    public void formAuth(final JComponent component) {
        FormAuth formAuth = new FormAuth(this.launcher);
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
     * Attempts to perform an automatic login using stored credentials.
     */
    public void attemptAutoLogin() {
        final String login = config.getLogin();
        final String encryptedPassword = config.getPassword();
        if (login != null && encryptedPassword != null) {
            authCredentials.put("login", login);
            final String decryptedPassword = cryptUtils.decrypt(encryptedPassword, encryptionKeyManager.getEncryptionKey(16));
            if (decryptedPassword != null) {
                authCredentials.put("password", decryptedPassword);
                Engine.getLOGGER().debug("Attempting auto login with saved credentials for: " + login);
                authTask(authCredentials);
            } else {
                clearCredentials();
            }
        }
    }

    /**
     * Performs asynchronous authentication using CompletableFuture.
     *
     * @return CompletableFuture with the authentication result.
     */
    public CompletableFuture<Boolean> authorizeAsync() {
        final String login = (String) authCredentials.get("login");
        final String password = (String) authCredentials.get("password");
        final AuthRequest authRequest = new AuthRequest(engine, login, password);
        final CompletableFuture<Boolean> future = new CompletableFuture<>();

        authRequest.sendAsync(Collections.emptyMap(),
                response -> handleAuthResponse(response, future),
                error -> handleAuthError(error, future)
        );
        return future;
    }

    /**
     * Handles a successful authentication response.
     *
     * @param response The server's response.
     * @param future   CompletableFuture to complete the operation.
     */
    private void handleAuthResponse(final Object response, final CompletableFuture<Boolean> future) {
        try {
            AuthResponse authResponse = GSON.fromJson(String.valueOf(response), AuthResponse.class);
            if ("success".equals(authResponse.getType())) {
                handleSuccessfulAuth(authResponse);
                future.complete(true);
            } else {
                launcher.getSOUND().playSound("other", "alert");
                launcher.showDialog(authResponse.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, false);
                future.complete(false);
            }
        } catch (Exception e) {
            Engine.getLOGGER().error("Error processing auth response", e);
            future.completeExceptionally(e);
        }
    }

    /**
     * Handles an error during authentication.
     *
     * @param error  The authentication error.
     * @param future CompletableFuture to complete the operation.
     */
    private void handleAuthError(final Throwable error, final CompletableFuture<Boolean> future) {
        Engine.getLOGGER().error("Authorization request failed: ", error);
        launcher.getSOUND().playSound("other", "alert");
        future.completeExceptionally(error);
    }

    /**
     * Handles successful authentication.
     *
     * @param authResponse The server response upon successful authentication.
     */
    private void handleSuccessfulAuth(final AuthResponse authResponse) {
        setAuthorised(true);
        launcher.getSOUND().playSound("other", "login", new PlaybackStatusListener() {
            @Override
            public void onPlaybackStarted(String s) {
                // No actions
            }

            @Override
            public void onPlaybackStopped(String s) {
                if (launcher.getConfig().isBackgroundMusic()) {
                    launcher.getSOUND().getSoundPlayer().onAllSoundsFinished(() -> {
                        launcher.getSOUND().getSoundPlayer().stopAllSounds();
                        launcher.getSOUND().playSound("music", "launcherTheme", true);
                    });
                }
            }

            @Override
            public void onPlaybackProgress(String s, long l, long l1) {
                // No actions
            }
        });

        // Save additional authentication data
        authCredentials.put("uuid", authResponse.getUuid());
        authCredentials.put("token", authResponse.getToken());
        authCredentials.put("group", String.valueOf(authResponse.getGroup()));
        authCredentials.put("colorScheme", String.valueOf(authResponse.getColorScheme()));
        authCredentials.put("userFullName", String.valueOf(authResponse.getUserFullName()));

        // Asynchronously load servers and update balance
        loadUserServers(authResponse.getLogin());
        updateBalance(authResponse.getBalance());

        Engine.getLOGGER().info(authResponse.getLogin() + " authorized!");
        if ("true".equals(authCredentials.get("rememberMe"))) {
            saveAuthCredentials(authCredentials);
        }
    }

    /**
     * Updates the balance using data from the server.
     * After updating the balanceMap, the injector notifies subscribers that up-to-date data is available.
     *
     * @param balance The list of balances to update.
     */
    public void updateBalance(final List<Map<String, Integer>> balance) {
        launcher.getExecutorServiceProvider().submitTask(() -> {
            try {
                // Update balance using merge to accumulate values
                for (Map<String, Integer> entry : balance) {
                    for (Map.Entry<String, Integer> e : entry.entrySet()) {
                        balanceMap.merge(e.getKey(), new AtomicInteger(e.getValue()),
                                (existing, newValue) -> {
                                    existing.addAndGet(e.getValue());
                                    return existing;
                                });
                    }
                }
                Engine.getLOGGER().info("Balance updated: " + balanceMap);
                balanceInjector.setContent(balanceMap);
            } catch (Exception ex) {
                Engine.getLOGGER().error("Error updating balance", ex);
            }
        }, "updateBalance");
    }

    /**
     * Loads the list of servers for the user.
     * After loading the servers, the injector notifies subscribers that the data is ready.
     *
     * @param login The user's login.
     */
    public void loadUserServers(final String login) {
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
        // Notify subscribers via the injector
        userServersInjector.setContent(userServersArray);
    }

    /**
     * Alternative method for loading servers using a DataInjector for a list of ServerAttributes.
     *
     * @param login           The user's login.
     * @param serversInjector The DataInjector that will be notified once the servers are loaded.
     */
    public void loadUserServers(final String login, final DataInjector<List<ServerAttributes>> serversInjector) {
        if (login == null || login.isEmpty()) {
            Engine.getLOGGER().warn("Empty login provided, aborting loadUserServers.");
            return;
        }
        ServerParser serverParser = new ServerParser(engine);
        List<ServerAttributes> loadedServers = serverParser.parseServers(login);
        Engine.getLOGGER().info("Loaded {} servers", loadedServers.size());
        // Notify subscribers via the provided injector
        serversInjector.setContent(loadedServers);
    }

    /**
     * Logs out of the system.
     */
    public void logOut() {
        Engine.getLOGGER().info("Logging out...");
        setAuthorised(false);
        engine.getFrame().getRootPanel().removeAll();
        clearCredentials();
        config.writeCurrentConfig();
        engine.init();
    }

    /**
     * Saves the encrypted user credentials.
     *
     * @param credentials The user's credentials.
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
     * Clears the user credentials.
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

    /**
     * Returns an array of servers if the user is authenticated; otherwise, returns an empty array.
     *
     * @return An array of servers or an empty array.
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

    public void setAuthorised(final boolean authorised) {
        this.authorised = authorised;
    }

    public Map<String, AtomicInteger> getBalanceMap() {
        return balanceMap;
    }

    public void setAuthCredentials(final Map<String, Object> authCredentials) {
        this.authCredentials = authCredentials;
    }

    /**
     * Getter for the universal server injector.
     *
     * @return A DataInjector for the array of servers.
     */
    public DataInjector<String[]> getUserServersInjector() {
        return userServersInjector;
    }

    /**
     * Getter for the universal balance injector.
     *
     * @return A DataInjector for the balance data.
     */
    public DataInjector<ConcurrentHashMap<String, AtomicInteger>> getBalanceInjector() {
        return balanceInjector;
    }
}
