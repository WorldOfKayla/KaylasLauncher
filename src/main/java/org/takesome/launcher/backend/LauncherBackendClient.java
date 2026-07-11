package org.takesome.launcher.backend;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.crash.CrashReportSendResult;
import org.takesome.kaylasEngine.crash.CrashReportSubmission;
import org.takesome.kaylasEngine.fileLoader.FileAttributes;
import org.takesome.kaylasEngine.server.ServerAttributes;
import org.takesome.launcher.gui.LauncherNotifications;
import org.takesome.launcher.security.SecureProcessAttestation;
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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class LauncherBackendClient implements AutoCloseable {
    private static final Gson GSON = new Gson();
    private static final int REQUEST_TIMEOUT_SECONDS = 15;
    private static final String ATTESTATION_HEADER = "X-Kaylas-Launcher-Attestation";
    private static final String BACKEND_PROTOCOL_VERSION = "0.1.0";
    private static final Type SERVER_LIST_TYPE = new TypeToken<List<ServerAttributes>>() {}.getType();
    private static final Type VERSION_LIST_TYPE = new TypeToken<List<LauncherGameVersion>>() {}.getType();
    private static final Type BADGE_LIST_TYPE = new TypeToken<List<BadgeObject>>() {}.getType();
    private static final Type BALANCE_LIST_TYPE = new TypeToken<List<Map<String, Integer>>>() {}.getType();
    private static final String ATTESTATION_FAILED_LOCALE_KEY = "error.attestationFailed";
    private static final String BACKEND_UNAVAILABLE_LOCALE_KEY = "error.backendUnavailable";
    private static final String FAILURE_NOTIFICATION_LOCATION = "BOTTOM_RIGHT";
    private static final long FAILURE_NOTIFICATION_DURATION_MS = 8_000L;

    private enum FailureNotification {
        NONE,
        BACKEND_UNAVAILABLE,
        ATTESTATION_FAILED
    }

    private final Launcher launcher;
    private final URI endpoint;
    private final int heartbeatSeconds;
    private final int maxReconnectAttempts;
    private final HttpClient httpClient;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean bound = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean statusRequested = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final AtomicInteger connectionSequence = new AtomicInteger(0);
    private final AtomicReference<CompletableFuture<Void>> boundSignal = new AtomicReference<>(new CompletableFuture<>());
    private final AtomicReference<String> attestationToken = new AtomicReference<>("");
    private final AtomicReference<FailureNotification> activeFailureNotification =
            new AtomicReference<>(FailureNotification.NONE);
    private final ConcurrentHashMap<String, CompletableFuture<LauncherBackendMessage>> pendingRequests = new ConcurrentHashMap<>();
    private final Object sendLock = new Object();
    private CompletableFuture<WebSocket> sendQueue = CompletableFuture.completedFuture(null);

    private volatile WebSocket webSocket;
    private volatile ScheduledFuture<?> heartbeatTask;
    private volatile Map<String, Object> backendStatus = Map.of();

    public LauncherBackendClient(
            Launcher launcher,
            String endpoint,
            int heartbeatSeconds,
            int maxReconnectAttempts,
            boolean requireSecureTransport,
            boolean allowInsecureLoopback,
            String tlsPublicKeySha256
    ) {
        this.launcher = Objects.requireNonNull(launcher, "launcher");
        this.endpoint = URI.create(Objects.requireNonNull(endpoint, "endpoint"));
        this.heartbeatSeconds = Math.max(5, heartbeatSeconds);
        this.maxReconnectAttempts = Math.max(0, maxReconnectAttempts);
        this.httpClient = BackendTransportSecurity.createHttpClient(
                launcher,
                this.endpoint,
                requireSecureTransport,
                allowInsecureLoopback,
                tlsPublicKeySha256
        );
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        Engine.LOGGER.info("Binding launcher to backend WS: {}", endpoint);
        connect();
        heartbeatTask = launcher.getScheduledTaskService().scheduleAtFixedRate(
                "launcher-backend-heartbeat",
                this::heartbeatOrReconnect,
                Duration.ofSeconds(heartbeatSeconds),
                Duration.ofSeconds(heartbeatSeconds)
        );
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

    public CompletableFuture<LauncherServerStatus> fetchServerStatus(ServerAttributes server) {
        if (server == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("server is required"));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        if (server.getId() > 0) {
            payload.put("serverId", server.getId());
        }
        payload.put("serverName", server.getServerName());
        payload.put("host", server.getHost());
        payload.put("port", server.getPort());
        return waitUntilBound()
                .thenCompose(v -> request("SERVER_STATUS_REQUEST", payload))
                .thenApply(this::parseServerStatusMessage);
    }

    private LauncherServerStatus parseServerStatusMessage(LauncherBackendMessage message) {
        if (message == null) {
            throw new IllegalStateException("Backend returned empty server status response.");
        }
        if ("ERROR".equals(message.getType())) {
            Object errorPayload = message.getPayload() == null
                    ? "unknown backend error"
                    : message.getPayload().getOrDefault("message", message.getPayload());
            throw new IllegalStateException("Backend server status request failed: " + errorPayload);
        }
        if (!"SERVER_STATUS".equals(message.getType())) {
            throw new IllegalStateException("Unexpected backend server status response type: " + message.getType());
        }

        Object statusPayload = message.getPayload() == null ? null : message.getPayload().get("status");
        if (statusPayload == null) {
            throw new IllegalStateException("Backend server status response has no status payload.");
        }
        return GSON.fromJson(GSON.toJson(statusPayload), LauncherServerStatus.class);
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
        return fetchVersionFiles(client, null, version, platform);
    }

    public CompletableFuture<FileAttributes[]> fetchVersionFiles(String coreType, String client, String version, int platform) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (coreType != null && !coreType.isBlank()) {
            payload.put("coreType", coreType);
        }
        if (client != null && !client.isBlank()) {
            payload.put("client", client);
        }
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

    public CompletableFuture<CrashReportSendResult> submitCrashReport(CrashReportSubmission submission) {
        Objects.requireNonNull(submission, "submission");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("application", submission.application());
        payload.put("engineVersion", submission.engineVersion());
        payload.put("generatedAt", submission.generatedAt().toString());
        payload.put("context", submission.context());
        payload.put("report", submission.reportText());
        if (submission.localReport() != null && submission.localReport().getFileName() != null) {
            payload.put("localFileName", submission.localReport().getFileName().toString());
        }
        if (launcher.getUser() != null) {
            String login = launcher.getUser().getLogin();
            String uuid = launcher.getUser().getUuid();
            if (login != null && !login.isBlank()) {
                payload.put("userLogin", login);
            }
            if (uuid != null && !uuid.isBlank()) {
                payload.put("userUuid", uuid);
            }
        }

        return waitUntilBound().thenCompose(ignored -> {
            URI uri = httpUri("/user/crash-reports", null, null);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload), StandardCharsets.UTF_8));
            applyAttestationHeader(requestBuilder);
            return httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenApply(response -> {
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            String body = response.body() == null ? "" : response.body().trim();
                            if (body.length() > 512) {
                                body = body.substring(0, 512);
                            }
                            throw new IllegalStateException(
                                    "Backend rejected crash report with HTTP "
                                            + response.statusCode()
                                            + (body.isBlank() ? "" : ": " + body)
                            );
                        }
                        CrashReportSendResult result = GSON.fromJson(response.body(), CrashReportSendResult.class);
                        if (result == null || result.reportId().isBlank()) {
                            throw new IllegalStateException("Backend returned an invalid crash-report acknowledgement.");
                        }
                        return result;
                    });
        });
    }

    private <T> CompletableFuture<T> getList(String path, String login, String uuid, Type type) {
        return waitUntilBound().thenCompose(v -> {
            URI uri = httpUri(path, login, uuid);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .GET();
            applyAttestationHeader(requestBuilder);
            HttpRequest request = requestBuilder.build();
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
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .GET();
            applyAttestationHeader(requestBuilder);
            HttpRequest request = requestBuilder.build();
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
        String currentAttestationToken = attestationToken.get();
        if (currentAttestationToken != null && !currentAttestationToken.isBlank()) {
            queryParameters.put("attestation", currentAttestationToken);
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

    private void applyAttestationHeader(HttpRequest.Builder builder) {
        String token = attestationToken.get();
        if (token != null && !token.isBlank()) {
            builder.header(ATTESTATION_HEADER, token);
        }
    }

    private CompletableFuture<Void> waitUntilBound() {
        if (closed.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Launcher backend client is closed."));
        }
        if (bound.get() && webSocket != null) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> signal = boundSignal.get();
        return signal.thenApply(ignored -> (Void) null)
                .orTimeout(10, TimeUnit.SECONDS)
                .exceptionallyCompose(error -> CompletableFuture.failedFuture(
                        new IllegalStateException(
                                "Launcher backend did not connect within 10 seconds: " + endpoint,
                                error
                        )
                ));
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
        future.orTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((response, error) -> pendingRequests.remove(requestId, future));

        sendQueued(type, message).whenComplete((ignored, error) -> {
            if (error != null) {
                pendingRequests.remove(requestId);
                future.completeExceptionally(error);
            }
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
                        connected.set(false);
                        bound.set(false);
                        resetBoundSignal();
                        int attempt = reconnectAttempts.incrementAndGet();
                        Engine.LOGGER.warn(
                                "Backend WS bind failed: {} attempt={}/{} cause={}",
                                endpoint,
                                attempt,
                                maxReconnectAttempts,
                                failureMessage(error)
                        );
                        notifyBackendUnavailable();
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
        if (socket == null || !connected.get()) {
            if (maxReconnectAttempts == 0 || reconnectAttempts.get() < maxReconnectAttempts) {
                statusRequested.set(false);
                connect();
            }
            return;
        }
        if (bound.get()) {
            send("PING", Map.of("launcherTime", String.valueOf(System.currentTimeMillis())));
        }
    }

    private void hello() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("launcher", launcher.getAppTitle());
        payload.put("version", launcher.getEngineData() != null ? launcher.getEngineData().getLauncherVersion() : "unknown");
        payload.put("runtime", "KaylasLauncher");
        payload.put("protocolVersion", BACKEND_PROTOCOL_VERSION);
        send("HELLO", payload);
    }

    private void requestStatus() {
        if (statusRequested.compareAndSet(false, true)) {
            send("STATUS_REQUEST", Map.of("reason", "startup"));
        }
    }

    public void send(String type, Map<String, Object> payload) {
        if (closed.get()) {
            return;
        }
        LauncherBackendMessage message = LauncherBackendMessage.of(type, UUID.randomUUID().toString(), payload);
        sendQueued(type, message).exceptionally(error -> {
            Engine.LOGGER.warn("Unable to send backend WS message type={}: {}", type, error.getMessage());
            return null;
        });
    }

    private CompletableFuture<Void> sendQueued(String type, LauncherBackendMessage message) {
        String json = GSON.toJson(message);
        int expectedConnection = connectionSequence.get();
        CompletableFuture<WebSocket> sendFuture;
        synchronized (sendLock) {
            sendFuture = sendQueue.handle((ignoredSocket, ignoredError) -> null)
                    .thenCompose(ignored -> {
                        WebSocket socket = webSocket;
                        boolean handshakeMessage = "HELLO".equals(type)
                                || "ATTESTATION_RESPONSE".equals(type);
                        if (socket == null
                                || closed.get()
                                || !connected.get()
                                || (!bound.get() && !handshakeMessage)) {
                            return failedWebSocketSend("Launcher backend is not attested: " + endpoint);
                        }
                        if (expectedConnection != connectionSequence.get()) {
                            return failedWebSocketSend("Launcher backend connection changed before sending " + type);
                        }
                        return socket.sendText(json, true);
                    });
            sendQueue = sendFuture;
        }
        return sendFuture.thenApply(ignored -> null);
    }

    private CompletableFuture<WebSocket> failedWebSocketSend(String message) {
        CompletableFuture<WebSocket> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException(message));
        return failed;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        connected.set(false);
        bound.set(false);
        attestationToken.set("");
        pendingRequests.forEach((requestId, future) -> future.completeExceptionally(new IllegalStateException("Backend client closed.")));
        pendingRequests.clear();
        boundSignal.get().completeExceptionally(new IllegalStateException("Backend client closed."));
        ScheduledFuture<?> currentHeartbeat = heartbeatTask;
        if (currentHeartbeat != null) {
            currentHeartbeat.cancel(false);
        }
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
            connectionSequence.incrementAndGet();
            connected.set(true);
            bound.set(false);
            attestationToken.set("");
            resetBoundSignal();
            reconnectAttempts.set(0);
            statusRequested.set(false);
            activeFailureNotification.compareAndSet(
                    FailureNotification.BACKEND_UNAVAILABLE,
                    FailureNotification.NONE
            );
            Engine.LOGGER.info("Backend WS transport connected: {}", endpoint);
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
            connectionSequence.incrementAndGet();
            connected.set(false);
            bound.set(false);
            attestationToken.set("");
            resetBoundSignal();
            statusRequested.set(false);
            failPendingRequests("Backend WS closed: " + statusCode + " " + reason);
            Engine.LOGGER.warn("Backend WS closed: status={} reason={}", statusCode, reason);
            if (!closed.get()
                    && activeFailureNotification.get() != FailureNotification.ATTESTATION_FAILED) {
                notifyBackendUnavailable();
            }
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            connectionSequence.incrementAndGet();
            connected.set(false);
            bound.set(false);
            attestationToken.set("");
            resetBoundSignal();
            statusRequested.set(false);
            failPendingRequests("Backend WS error: " + failureMessage(error));
            Engine.LOGGER.warn("Backend WS error: {}", failureMessage(error));
            if (!closed.get()
                    && activeFailureNotification.get() != FailureNotification.ATTESTATION_FAILED) {
                notifyBackendUnavailable();
            }
        }
    }

    private void resetBoundSignal() {
        if (closed.get()) {
            return;
        }
        boundSignal.updateAndGet(current -> current.isDone() ? new CompletableFuture<>() : current);
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
                case "ATTESTATION_CHALLENGE" -> handleAttestationChallenge(message);
                case "ATTESTATION_OK" -> handleAttestationAccepted(message);
                case "ATTESTATION_DENIED" -> handleAttestationDenied(message);
                case "HELLO_ACK" -> {
                    Engine.LOGGER.info("Backend HELLO acknowledged: {}", message.getPayload());
                    boolean required = message.getPayload() != null
                            && Boolean.parseBoolean(String.valueOf(
                                    message.getPayload().getOrDefault("attestationRequired", false)
                            ));
                    if (!required && bound.compareAndSet(false, true)) {
                        activeFailureNotification.set(FailureNotification.NONE);
                        boundSignal.get().complete(null);
                        requestStatus();
                    }
                }
                case "PONG" -> Engine.LOGGER.debug("Backend PONG: {}", message.getPayload());
                case "STATUS" -> handleStatus(message.getPayload());
                case "AUTH_OK" -> Engine.LOGGER.info("Backend auth accepted: {}", message.getPayload());
                case "AUTH_DENIED" -> Engine.LOGGER.warn("Backend auth denied: {}", message.getPayload());
                case "SERVERS" -> Engine.LOGGER.debug("Backend servers response: {}", message.getPayload());
                case "SERVER_STATUS" -> Engine.LOGGER.debug("Backend server status response: {}", message.getPayload());
                case "VERSIONS" -> Engine.LOGGER.debug("Backend versions response: {}", message.getPayload());
                case "FILES" -> logFilesResponse(message.getPayload());
                case "ERROR" -> Engine.LOGGER.warn("Backend error: {}", message.getPayload());
                default -> Engine.LOGGER.debug("Backend message {}: {}", message.getType(), message.getPayload());
            }
        } catch (RuntimeException error) {
            Engine.LOGGER.warn("Invalid backend WS payload: {}", payload, error);
        }
    }

    private void handleAttestationChallenge(LauncherBackendMessage message) {
        Map<String, Object> payload = message.getPayload();
        if (payload == null) {
            rejectAttestationLocally("Backend challenge payload is missing");
            return;
        }
        String challenge = String.valueOf(payload.getOrDefault("challenge", "")).trim();
        String session = String.valueOf(payload.getOrDefault("session", "")).trim();
        String protocolVersion = String.valueOf(
                payload.getOrDefault("protocolVersion", BACKEND_PROTOCOL_VERSION)
        ).trim();
        String attestationProtocol = String.valueOf(
                payload.getOrDefault(
                        "attestationProtocol",
                        SecureProcessAttestation.SOFTWARE_PROTOCOL
                )
        ).trim();
        if (challenge.isBlank() || session.isBlank()) {
            rejectAttestationLocally("Backend challenge is incomplete");
            return;
        }

        try {
            String launcherVersion = launcher.getEngineData() == null
                    ? "unknown"
                    : launcher.getEngineData().getLauncherVersion();
            Map<String, Object> evidence = SecureProcessAttestation.create(
                    challenge,
                    session,
                    launcherVersion,
                    protocolVersion,
                    attestationProtocol
            );
            Map<String, Object> responsePayload = new LinkedHashMap<>();
            responsePayload.put("evidence", evidence);
            LauncherBackendMessage response = LauncherBackendMessage.of(
                    "ATTESTATION_RESPONSE",
                    message.getRequestId(),
                    responsePayload
            );
            sendQueued("ATTESTATION_RESPONSE", response).exceptionally(error -> {
                rejectAttestationLocally("Unable to send attestation response: " + error.getMessage());
                return null;
            });
            Engine.LOGGER.info(
                    "SecureProcess attestation response created: profile={} keyId={} build={} nativeVersion={}",
                    evidence.get("version"),
                    evidence.get("keyId"),
                    evidence.get("launcherBuildSha256"),
                    evidence.get("nativeVersion")
            );
        } catch (RuntimeException error) {
            rejectAttestationLocally(error.getMessage());
        }
    }

    private void handleAttestationAccepted(LauncherBackendMessage message) {
        Map<String, Object> payload = message.getPayload();
        String token = payload == null
                ? ""
                : String.valueOf(payload.getOrDefault("accessToken", "")).trim();
        if (token.isBlank()) {
            rejectAttestationLocally("Backend accepted attestation without an access token");
            return;
        }
        attestationToken.set(token);
        activeFailureNotification.set(FailureNotification.NONE);
        bound.set(true);
        boundSignal.get().complete(null);
        reconnectAttempts.set(0);
        Engine.LOGGER.info(
                "SecureProcess attestation accepted: keyId={} expiresAt={}",
                payload.get("keyId"),
                payload.get("expiresAt")
        );
        requestStatus();
    }

    private void handleAttestationDenied(LauncherBackendMessage message) {
        Object reason = message.getPayload() == null
                ? "attestation denied"
                : message.getPayload().getOrDefault("reason", message.getPayload());
        rejectAttestationLocally(String.valueOf(reason));
    }

    private void rejectAttestationLocally(String reason) {
        connected.set(false);
        bound.set(false);
        attestationToken.set("");
        IllegalStateException failure = new IllegalStateException(
                "SecureProcess backend attestation failed: " + reason
        );
        boundSignal.get().completeExceptionally(failure);
        failPendingRequests(failure.getMessage());
        notifyAttestationFailure(reason);
        Engine.LOGGER.error(failure.getMessage());
        WebSocket socket = webSocket;
        if (socket != null) {
            socket.sendClose(1008, "SecureProcess attestation failed");
        }
    }

    private void notifyAttestationFailure(String reason) {
        String normalizedReason = reason == null || reason.isBlank()
                ? "attestation denied"
                : reason.trim();
        showFailureNotification(
                FailureNotification.ATTESTATION_FAILED,
                ATTESTATION_FAILED_LOCALE_KEY,
                Map.of("reason", normalizedReason)
        );
    }

    private void notifyBackendUnavailable() {
        showFailureNotification(
                FailureNotification.BACKEND_UNAVAILABLE,
                BACKEND_UNAVAILABLE_LOCALE_KEY,
                Map.of()
        );
    }

    private void showFailureNotification(
            FailureNotification notification,
            String localeKey,
            Map<String, ?> replacements
    ) {
        if (closed.get()) {
            return;
        }
        FailureNotification previous = activeFailureNotification.getAndSet(notification);
        if (previous == notification) {
            return;
        }
        try {
            LauncherNotifications.showLocalized(
                    launcher,
                    "ERROR",
                    FAILURE_NOTIFICATION_LOCATION,
                    FAILURE_NOTIFICATION_DURATION_MS,
                    localeKey,
                    replacements,
                    null
            );
        } catch (RuntimeException error) {
            activeFailureNotification.compareAndSet(notification, previous);
            Engine.LOGGER.warn(
                    "Unable to display launcher backend notification key={}: {}",
                    localeKey,
                    failureMessage(error)
            );
        }
    }

    private static String failureMessage(Throwable error) {
        if (error == null) {
            return "unknown error";
        }
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message.trim();
    }

    private void logFilesResponse(Map<String, Object> payload) {
        if (payload == null) {
            Engine.LOGGER.debug("Backend files response: empty payload");
            return;
        }
        Object files = payload.get("files");
        int count = files instanceof List<?> list ? list.size() : -1;
        Engine.LOGGER.debug(
                "Backend files response: coreType={} client={} version={} platform={} count={} scope={}",
                payload.get("coreType"),
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
