package org.takesome.launcher.auth;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.server.ServerAttributes;
import org.takesome.kaylasEngine.utils.DataInjector;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class UserDataLoader {
    private final Engine engine;
    private final DataInjector<String[]> serversInjector;
    private final DataInjector<ConcurrentHashMap<String, AtomicInteger>> balanceInjector;
    private String[] userServersArray;
    private List<ServerAttributes> userServersAttributes;
    private ConcurrentHashMap<String, AtomicInteger> balanceMap = new ConcurrentHashMap<>();

    public UserDataLoader(Engine engine) {
        this.engine = engine;
        this.serversInjector = new DataInjector<>();
        this.balanceInjector = new DataInjector<>();
    }

    public void loadUserServers(final String login) {
        if (login == null || login.isEmpty()) {
            Engine.getLOGGER().warn("Empty login provided, aborting loadUserServers.");
            return;
        }
        if (!(engine instanceof Launcher launcher) || launcher.getBackendClient() == null) {
            Engine.getLOGGER().warn("Backend WS client is unavailable; server list will not be loaded through legacy HTTP.");
            userServersAttributes = Collections.emptyList();
            userServersArray = new String[0];
            serversInjector.setContent(userServersArray);
            return;
        }

        try {
            userServersAttributes = launcher.getBackendClient().fetchServers(login).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while loading server list over backend WS.", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to load server list over backend WS.", e);
        }
        userServersArray = userServersAttributes.stream()
                .map(sa -> sa.getServerName() + " " + sa.getServerVersion())
                .toArray(String[]::new);
        Engine.getLOGGER().info("Loaded {} servers over backend WS", userServersArray.length);
        serversInjector.setContent(userServersArray);
    }

    public void loadBackendUserData(final String login, final Runnable onDataLoaded) {
        Launcher.LOGGER.info("Loading backend-managed userdata for {}", login);
        if (!(engine instanceof Launcher launcher) || launcher.getBackendClient() == null) {
            Engine.LOGGER.warn("Backend client is unavailable; using empty backend-managed userdata for {}", login);
            applyBackendUserData(login, Collections.emptyList(), Collections.emptyList(), onDataLoaded);
            return;
        }

        CompletableFuture<List<ServerAttributes>> serversFuture = launcher.getBackendClient().fetchServers(login)
                .exceptionally(error -> {
                    Engine.LOGGER.error("Failed to load backend servers for {}: {}", login, error.getMessage());
                    return Collections.emptyList();
                });
        String userUuid = launcher.getAuth() != null && launcher.getAuth().getAuthResponse() != null ? launcher.getAuth().getAuthResponse().getUuid() : null;
        CompletableFuture<List<Map<String, Integer>>> balanceFuture = launcher.getBackendClient().fetchBalance(login, userUuid)
                .exceptionally(error -> {
                    Engine.LOGGER.error("Failed to load backend balance for {}: {}", login, error.getMessage());
                    return Collections.emptyList();
                });

        serversFuture.thenCombine(balanceFuture, (servers, balance) -> {
            applyBackendUserData(login, servers, balance, onDataLoaded);
            return null;
        });
    }

    private void applyBackendUserData(final String login,
                                      final List<ServerAttributes> servers,
                                      final List<Map<String, Integer>> balance,
                                      final Runnable onDataLoaded) {
        userServersAttributes = servers == null ? Collections.emptyList() : servers;
        userServersArray = userServersAttributes.stream()
                .map(sa -> sa.getServerName() + " " + sa.getServerVersion())
                .toArray(String[]::new);
        Launcher.LOGGER.info("Loaded {} backend-managed servers for {}", userServersArray.length, login);
        serversInjector.setContent(userServersArray);
        updateBalance(balance == null ? Collections.emptyList() : balance);
        if (onDataLoaded != null) {
            onDataLoaded.run();
        }
    }

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
            balanceInjector.setContent(balanceMap);
        } catch (Exception ex) {
            Engine.LOGGER.error("Error updating balance", ex);
        }
    }

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

    public DataInjector<String[]> getServersInjector() { return serversInjector; }
    public DataInjector<ConcurrentHashMap<String, AtomicInteger>> getBalanceInjector() { return balanceInjector; }
    public ConcurrentHashMap<String, AtomicInteger> getBalanceMap() { return balanceMap; }
    public String[] getUserServersArray() { return userServersArray; }
    public List<ServerAttributes> getUserServersAttributes() { return userServersAttributes; }
}
