package org.foxesworld.launcher.auth;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.utils.DataInjector;
import org.foxesworld.launcher.server.ServerParser;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The UserDataLoader class is responsible for loading and managing user-related data,
 * including the list of servers and balance information.
 */
public class UserDataLoader {
    private final Engine engine;
    private final DataInjector<String[]> serversInjector;
    private final DataInjector<ConcurrentHashMap<String, AtomicInteger>> balanceInjector;
    private String[] userServersArray;
    private List<ServerAttributes> userServersAttributes;
    private ConcurrentHashMap<String, AtomicInteger> balanceMap = new ConcurrentHashMap<>();

    /**
     * Constructs a new UserDataLoader with the specified engine.
     *
     * @param engine the engine instance used for server parsing and logging.
     */
    public UserDataLoader(Engine engine) {
        this.engine = engine;
        this.serversInjector = new DataInjector<>();
        this.balanceInjector = new DataInjector<>();
    }

    /**
     * Loads the user's servers based on the provided login.
     *
     * @param login the user's login.
     */
    public void loadUserServers(final String login) {
        if (login == null || login.isEmpty()) {
            Engine.getLOGGER().warn("Empty login provided, aborting loadUserServers.");
            return;
        }
        ServerParser serverParser = new ServerParser(engine);
        try {
            userServersAttributes = (List<ServerAttributes>) serverParser.parseServers(login).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        userServersArray = userServersAttributes.stream()
                .map(sa -> sa.getServerName() + " " + sa.getServerVersion())
                .toArray(String[]::new);
        Engine.getLOGGER().info("Loaded {} servers", serverParser.getServerNum());

        serversInjector.setContent(userServersArray);
    }

    /**
     * Updates the balance using data from the server.
     *
     * @param balance the list of balance maps to update.
     */
    public void updateBalance(final List<Map<String, Integer>> balance) {
        try {
            balanceMap = new ConcurrentHashMap<>();
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

            // Уведомляем подписчиков через DataInjector
            balanceInjector.setContent(balanceMap);
        } catch (Exception ex) {
            Engine.getLOGGER().error("Error updating balance", ex);
        }
    }

    /**
     * Loads both user servers and balance data, then executes the provided callback once loading is complete.
     *
     * @param login        the user's login.
     * @param balance      the list of balance maps.
     * @param onDataLoaded a Runnable to be executed after both datasets have been loaded.
     */
    public void loadUserData(final String login, final List<Map<String, Integer>> balance, final Runnable onDataLoaded) {
        Launcher.LOGGER.info("Loading userdata for {}", login);
        if (onDataLoaded == null) {
            loadUserServers(login);
            updateBalance(balance);
            return;
        }

        AtomicBoolean serversLoaded = new AtomicBoolean(false);
        AtomicBoolean balanceLoaded = new AtomicBoolean(false);

        Runnable checkCompletion = () -> {
            if (serversLoaded.get() && balanceLoaded.get()) {
                onDataLoaded.run();
            }
        };

        serversInjector.addListener(servers -> {
            serversLoaded.set(true);
            checkCompletion.run();
        });

        balanceInjector.addListener(balanceMap -> {
            balanceLoaded.set(true);
            checkCompletion.run();
        });

        loadUserServers(login);
        updateBalance(balance);
    }

    public DataInjector<String[]> getServersInjector() {
        return serversInjector;
    }

    public DataInjector<ConcurrentHashMap<String, AtomicInteger>> getBalanceInjector() {
        return balanceInjector;
    }

    public ConcurrentHashMap<String, AtomicInteger> getBalanceMap() {
        return balanceMap;
    }

    public String[] getUserServersArray() {
        return userServersArray;
    }

    public List<ServerAttributes> getUserServersAttributes() {
        return userServersAttributes;
    }
}
