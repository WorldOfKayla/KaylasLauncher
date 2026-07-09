package org.takesome.launcher.backend;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.fileLoader.FileAttributes;
import org.takesome.kaylasEngine.server.ServerAttributes;
import org.takesome.launcher.user.loader.BadgeObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class LauncherBackendClient implements AutoCloseable {
    private static final Gson GSON = new Gson();
    private static final int REQUEST_TIMEOUT_SECONDS = 15;
    private static final Type SERVER_LIST_TYPE = new TypeToken<List<ServerAttributes>>() {}.getType();
    private static final Type VERSION_LIST_TYPE = new TypeToken<List<LauncherGameVersion>>() {}.getType();
    private static final Type BADGE_LIST_TYPE = new TypeToken<List<BadgeObject>>() {}.getType();
    private static final Type BALANCE_LIST_TYPE = new TypeToken<List<Map<String, Integer>>>() {}.getType();

    private final Launcher launcher;
    private final URI endpoint;
    private final int heartbeatSeconds;
    private final int maxReconnectAttempts;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean bound = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean statusRequested = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final ConcurrentHashMap<String, CompletableFuture<LauncherBackendMessage>> pendingRequests = new ConcurrentHashMap<>();

    private volatile WebSocket webSocket;
    private volatile Map<String, Object> backendStatus = Map.of();

    public LauncherBackendClient(Launcher launcher, String endpoint, int heartbeatSeconds, int maxReconnectAttempts) {
        this.launcher = Objects.requireNonNull(launcher, "launcher");
        this.endpoint = URI.create(Objects.requireNonNull(endpoint, "endpoint"));
        this.heartbeatSeconds = Math.max(5, heartbeatSeconds);
        this.maxReconnectAttempts = Math.max(0, maxReconnectAttempts);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "kaylas-launcher-backend-client");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        Engine.LOGGER.info("Binding launcher to backend WS: {}", endpoint);
        connect();
        scheduler.scheduleAtFixedRate(this::heartbeatOrReconnect,
                heartbeatSeconds,
                heartbeatSeconds,
                TimeUnit.SECONDS);
    }

    public boolean isBound() {
        return bound.get();
    }

    public URI endpoint() {
        return endpoint;
    }

    public URI resourceUri(String path) {
        return httpUri(path, null, null, Map.of());
    }

    public Map<String, Object> getBackendStatus() {
        return backendStatus;
    }

    public CompletableFuture<LauncherBackendMessage> authenticate(String username, String password) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username);
        payload.put("password", password);
        return waitUntilBound().thenCompose(v -> request("AUTH_REQUEST", payload));
    }

    public CompletableFuture<List<ServerAttributes>> fetchServers(String login) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (login != null && !login.isBlank()) {
            payload.put("login", login);
        }

        return waitUntilBound()
                .thenCompose(v -> request("SERVERS_REQUEST", payload))
                .thenApply(this::parseServersMessage);
    }

    private List<ServerAttributes> parseServersMessage(LauncherBackendMessage message) {
        if (message == null) {
            throw new IllegalStateException("Backend returned empty server response.");
        }
        if ("ERROR".equals(message.getType())) {
            Object errorPayload = message.getPayload() == null
                    ? "unknown backend error"
                    : message.getPayload().getOrDefault("message", message.getPayload());
            throw new IllegalStateException("Backend server request failed: " + errorPayload);
        }
        if (!"SERVERS".equals(message.getType())) {
            throw new IllegalStateException("Unexpected backend server response type: " + message.getType());
        }

        Object serversPayload = message.getPayload() == null ? null : message.getPayload().get("servers");
        if (serversPayload == null) {
            return List.of();
        }

        List<ServerAttributes> servers = GSON.fromJson(GSON.toJson(serversPayload), SERVER_LIST_TYPE);
        return servers == null ? List.of() : servers;
    }

    public CompletableFuture<List<LauncherGameVersion>> fetchVersions() {
        return fetchVersions(null);
    }

    public CompletableFuture<List<LauncherGameVersion>> fetchVersions(String client) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (client != null && !client.isBlank()) {
            payload.put("client", client);
        }

        return waitUntilBound()
                .thenCompose(v -> request("VERSIONS_REQUEST", payload))
                .thenApply(this::parseVersionsMessage);
    }

    private List<LauncherGameVersion> parseVersionsMessage(LauncherBackendMessage message) {
        if (message == null) {
            throw new IllegalStateException("Backend returned empty versions response.");
        }
        if ("ERROR".equals(message.getType())) {
            Object errorPayload = message.getPayload() == null
                    ? "unknown backend error"
                    : message.getPayload().getOrDefault("message", message.getPayload());
            throw new IllegalStateException("Backend versions request failed: " + errorPayload);
        }
        if (!"VERSIONS".equals(message.getType())) {
            throw new IllegalStateException("Unexpected backend versions response type: " + message.getType());
        }

        Object versionsPayload = message.getPayload() == null ? null : message.getPayload().get("versions");
        if (versionsPayload == null) {
            return List.of();
        }

        List<LauncherGameVersion> versions = GSON.fromJson(GSON.toJson(versionsPayload), VERSION_LIST_TYPE);
        return versions == null ? List.of() : versions;
    }

    public CompletableFuture<FileAttributes[]> fetchVersionFiles(String client, String version, int platform) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("client", client);
        payload.put("version", version);
        payload.put("platform", platform);

        return waitUntilBound()
                .thenCompose(v -> request("FILES_REQUEST", payload))
                .thenApply(this::parseFilesMessage);
    }

    private FileAttributes[] parseFilesMessage(LauncherBackendMessage message) {
        if (message == null) {
            throw new IllegalStateException("Backend returned empty files response.");
        }
        if ("ERROR".equals(message.getType())) {
            Object errorPayload = message.getPayload() == null
                    ? "unknown backend error"
                    : message.getPayload().getOrDefault("message", message.getPayload());
            throw new IllegalStateException("Backend files request failed: " + errorPayload);
        }
        if (!"FILES".equals(message.getType())) {
            throw new IllegalStateException("Unexpected backend files response type: " + message.getType());
        }

        Object filesPayload = message.getPayload() == null ? null : message.getPayload().get("files");
        if (filesPayload == null) {
            return new FileAttributes[0];
        }

        FileAttributes[] files = GSON.fromJson(GSON.toJson(filesPayload), FileAttributes[].class);
        return files == null ? new FileAttributes[0] : files;
    }

    public CompletableFuture<List<BadgeObject>> fetchBadges(String login, String uuid) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (uuid != null && !uuid.isBlank()) {
            payload.put("uuid", uuid);
        }
        if (login != null && !login.isBlank()) {
            payload.put("login", login);
        }

        return waitUntilBound()
                .thenCompose(v -> request("BADGES_REQUEST", payload))
                .thenApply(this::parseBadgesMessage);
    }

    private List<BadgeObject> parseBadgesMessage(LauncherBackendMessage message) {
        if (message == null) {
            throw new IllegalStateException("Backend returned empty badges response.");
        }
        if ("ERROR".equals(message.getType())) {
            Object errorPayload = message.getPayload() == null
                    ? "unknown backend error"
                    : message.getPayload().getOrDefault("message", message.getPayload());
            throw new IllegalStateException("Backend badges request failed: " + errorPayload);
        }
        if (!"BADGES".equals(message.getType())) {
            throw new IllegalStateException("Unexpected backend badges response type: " + message.getType());
        }

        Object badgesPayload = message.getPayload() == null ? null : message.getPayload().get("badges");
        if (badgesPayload == null) {
            return List.of();
        }

        List<BadgeObject> badges = GSON.fromJson(GSON.toJson(badgesPayload), BADGE_LIST_TYPE);
        return badges == null ? List.of() : badges;
    }

    public CompletableFuture<List<Map<String, Integer>>> fetchBalance(String login, String uuid) {
        return getList("/user/balance", login, uuid, BALANCE_LIST_TYPE);
    }

    public CompletableFuture<BufferedImage> fetchHead(String login, String uuid) {
        return getImage("/user/head", login, uuid, Map.of());
    }

    public CompletableFuture<BufferedImage> fetchSkin(String login, String uuid, String side) {
        return getImage("/user/skin", login, uuid, Map.of("side", side == null || side.isBlank() ? "front" : side));
    }

    private <T> CompletableFuture<T> getList(String path, String login, String uuid, Type type) {
        return waitUntilBound().thenCompose(v -> {
            URI uri = httpUri(path, login, uuid);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .GET()
                    .build();
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            throw new IllegalStateException("Backend HTTP " + response.statusCode() + " for " + uri);
                        }
                        return GSON.fromJson(response.body(), type);
                    });
        });
    }

    private CompletableFuture<BufferedImage> getImage(String path, String login, String uuid, Map<String, String> extraQuery) {
        return waitUntilBound().thenCompose(v -> {
            URI uri = httpUri(path, login, uuid, extraQuery);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .GET()
                    .build();
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                    .thenApply(response -> {
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            throw new IllegalStateException("Backend HTTP " + response.statusCode() + " for " + uri);
                        }
                        try {
                            BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.body()));
                            if (image == null) {
                                throw new IllegalStateException("Backend returned non-image data for " + uri);
                            }
                            return image;
                        } catch (Exception error) {
                            throw new IllegalStateException("Unable to decode backend image response for " + uri, error);
                        }
                    });
        });
    }

    private URI httpUri(String path, String login, String uuid) {
        return httpUri(path, login, uuid, Map.of());
    }

    private URI httpUri(String path, String login, String uuid, Map<String, String> extraQuery) {
        String scheme = "wss".equalsIgnoreCase(endpoint.getScheme()) ? "https" : "http";
        Map<String, String> queryParameters = new LinkedHashMap<>();
        if (uuid != null && !uuid.isBlank()) {
            queryParameters.put("uuid", uuid);
        } else if (login != null && !login.isBlank()) {
            queryParameters.put("login", login);
        }
        if (extraQuery != null) {
            extraQuery.forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                    queryParameters.put(key, value);
                }
            });
        }

        String query = queryParameters.isEmpty()
                ? null
                : queryParameters.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .reduce((left, right) -> left + "&" + right)
                .orElse(null);
        try {
            return new URI(scheme, null, endpoint.getHost(), endpoint.getPort(), path, query, null);
        } catch (Exception error) {
            throw new IllegalStateException("Unable to build backend HTTP URI for " + path, error);
        }
    }

    private CompletableFuture<Void> waitUntilBound() {
        return CompletableFuture.runAsync(() -> {
            long deadline = System.currentTimeMillis() + 10000L;
            while (!closed.get() && System.currentTimeMillis() < deadline) {
                if (bound.get() && webSocket != null) {
                    return;
                }
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for launcher backend connection.", e);
                }
            }
            throw new IllegalStateException("Launcher backend did not connect within 10 seconds: " + endpoint);
        });
    }

    public CompletableFuture<LauncherBackendMessage> request(String type, Map<String, Object> payload) {
        WebSocket socket = webSocket;
        if (socket == null || closed.get() || !bound.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Launcher backend is not connected: " + endpoint));
        }

        String requestId = UUID.randomUUID().toString();
        LauncherBackendMessage message = LauncherBackendMessage.of(type, requestId, payload);
        CompletableFuture<LauncherBackendMessage> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        scheduler.schedule(() -> {
            CompletableFuture<LauncherBackendMessage> pending = pendingRequests.remove(requestId);
            if (pending != null) {
                pending.completeExceptionally(new TimeoutException("Backend request timed out: " + type));
            }
        }, REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        socket.sendText(GSON.toJson(message), true)
                .exceptionally(error -> {
                    pendingRequests.remove(requestId);
                    future.completeExceptionally(error);
                    return null;
                });
        return future;
    }

    private void connect() {
        if (closed.get()) {
            return;
        }
        httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .buildAsync(endpoint, new BackendWebSocketListener())
                .whenComplete((socket, error) -> {
                    if (error != null) {
                        bound.set(false);
                        int attempt = reconnectAttempts.incrementAndGet();
                        Engine.LOGGER.warn("Backend WS bind failed: {} attempt={}/{}", endpoint, attempt, maxReconnectAttempts);
                        return;
                    }
                    this.webSocket = socket;
                });
    }

    private void heartbeatOrReconnect() {
        if (closed.get()) {
            return;
        }
        WebSocket socket = webSocket;
        if (socket == null || !bound.get()) {
            if (maxReconnectAttempts == 0 || reconnectAttempts.get() < maxReconnectAttempts) {
                statusRequested.set(false);
                connect();
            }
            return;
        }
        send("PING", Map.of("launcherTime", String.valueOf(System.currentTimeMillis())));
    }

    private void hello() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("launcher", launcher.getAppTitle());
        payload.put("version", launcher.getEngineData() != null ? launcher.getEngineData().getLauncherVersion() : "unknown");
        payload.put("runtime", "KaylasLauncher");
        payload.put("protocolVersion", "0.1.0");
        send("HELLO", payload);
    }

    private void requestStatus() {
        if (statusRequested.compareAndSet(false, true)) {
            send("STATUS_REQUEST", Map.of("reason", "startup"));
        }
    }

    public void send(String type, Map<String, Object> payload) {
        WebSocket socket = webSocket;
        if (socket == null || closed.get()) {
            return;
        }
        LauncherBackendMessage message = LauncherBackendMessage.of(type, UUID.randomUUID().toString(), payload);
        socket.sendText(GSON.toJson(message), true)
                .exceptionally(error -> {
                    bound.set(false);
                    Engine.LOGGER.warn("Unable to send backend WS message type={}: {}", type, error.getMessage());
                    return null;
                });
    }

    @Override
    public void close() {
        closed.set(true);
        pendingRequests.forEach((requestId, future) -> future.completeExceptionally(new IllegalStateException("Backend client closed.")));
        pendingRequests.clear();
        scheduler.shutdownNow();
        WebSocket socket = webSocket;
        if (socket != null) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "launcher shutdown");
        }
    }

    private final class BackendWebSocketListener implements WebSocket.Listener {
        private final StringBuilder partial = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            LauncherBackendClient.this.webSocket = webSocket;
            bound.set(true);
            reconnectAttempts.set(0);
            statusRequested.set(false);
            Engine.LOGGER.info("Backend WS bound: {}", endpoint);
            webSocket.request(1);
            hello();
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                String payload = partial.toString();
                partial.setLength(0);
                handleBackendMessage(payload);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            bound.set(false);
            statusRequested.set(false);
            failPendingRequests("Backend WS closed: " + statusCode + " " + reason);
            Engine.LOGGER.warn("Backend WS closed: status={} reason={}", statusCode, reason);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            bound.set(false);
            statusRequested.set(false);
            failPendingRequests("Backend WS error: " + error.getMessage());
            Engine.LOGGER.warn("Backend WS error: {}", error.getMessage());
        }
    }

    private void failPendingRequests(String message) {
        pendingRequests.forEach((requestId, future) -> future.completeExceptionally(new IllegalStateException(message)));
        pendingRequests.clear();
    }

    private void handleBackendMessage(String payload) {
        try {
            LauncherBackendMessage message = GSON.fromJson(payload, LauncherBackendMessage.class);
            if (message == null || message.getType() == null) {
                Engine.LOGGER.warn("Backend WS returned empty message: {}", payload);
                return;
            }

            if (message.getRequestId() != null) {
                CompletableFuture<LauncherBackendMessage> pending = pendingRequests.remove(message.getRequestId());
                if (pending != null) {
                    pending.complete(message);
                }
            }

            switch (message.getType()) {
                case "HELLO_ACK" -> {
                    bound.set(true);
                    Engine.LOGGER.info("Backend HELLO acknowledged: {}", message.getPayload());
                    requestStatus();
                }
                case "PONG" -> Engine.LOGGER.debug("Backend PONG: {}", message.getPayload());
                case "STATUS" -> handleStatus(message.getPayload());
                case "AUTH_OK" -> Engine.LOGGER.info("Backend auth accepted: {}", message.getPayload());
                case "AUTH_DENIED" -> Engine.LOGGER.warn("Backend auth denied: {}", message.getPayload());
                case "SERVERS" -> Engine.LOGGER.debug("Backend servers response: {}", message.getPayload());
                case "VERSIONS" -> Engine.LOGGER.debug("Backend versions response: {}", message.getPayload());
                case "FILES" -> logFilesResponse(message.getPayload());
                case "ERROR" -> Engine.LOGGER.warn("Backend error: {}", message.getPayload());
                default -> Engine.LOGGER.debug("Backend message {}: {}", message.getType(), message.getPayload());
            }
        } catch (RuntimeException error) {
            Engine.LOGGER.warn("Invalid backend WS payload: {}", payload, error);
        }
    }

    private void logFilesResponse(Map<String, Object> payload) {
        if (payload == null) {
            Engine.LOGGER.debug("Backend files response: empty payload");
            return;
        }
        Object files = payload.get("files");
        int count = files instanceof List<?> list ? list.size() : -1;
        Engine.LOGGER.debug(
                "Backend files response: client={} version={} platform={} count={} scope={}",
                payload.get("client"),
                payload.get("version"),
                payload.get("platform"),
                count,
                payload.get("scope")
        );
    }

    private void handleStatus(Map<String, Object> payload) {
        Object statusObject = payload == null ? null : payload.get("status");
        if (statusObject instanceof Map<?, ?> statusMap) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            statusMap.forEach((key, value) -> normalized.put(String.valueOf(key), value));
            backendStatus = normalized;
            Engine.LOGGER.info("Backend status loaded: modules={} capabilities={}",
                    normalized.get("modules"),
                    normalized.get("capabilities"));
            return;
        }
        Engine.LOGGER.info("Backend status: {}", payload);
    }
}
