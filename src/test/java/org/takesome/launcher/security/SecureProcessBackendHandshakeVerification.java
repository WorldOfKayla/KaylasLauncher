package org.takesome.launcher.security;

import com.google.gson.Gson;
import org.takesome.launcher.backend.LauncherBackendMessage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** End-to-end WebSocket verification for SecureProcess backend attestation. */
public final class SecureProcessBackendHandshakeVerification {
    private static final Gson GSON = new Gson();
    private static final String BACKEND_PROTOCOL = "0.1.0";

    private SecureProcessBackendHandshakeVerification() {
    }

    public static void main(String[] args) throws Exception {
        URI endpoint = URI.create(args.length == 0
                ? "ws://127.0.0.1:18080/ws/launcher"
                : args[0]);
        SecureProcessResult result = SecureProcess.initializeEarly();
        require(result.fullyApplied(), "SecureProcess baseline is not fully applied");

        MessageListener listener = new MessageListener();
        WebSocket socket = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build()
                .newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .buildAsync(endpoint, listener)
                .get(10, TimeUnit.SECONDS);

        String handshakeId = UUID.randomUUID().toString();
        send(socket, LauncherBackendMessage.of(
                "HELLO",
                handshakeId,
                Map.of(
                        "launcher", "KaylasLauncher",
                        "version", "2.12.0",
                        "runtime", "KaylasLauncher",
                        "protocolVersion", BACKEND_PROTOCOL
                )
        ));

        LauncherBackendMessage challengeMessage = listener.awaitType(
                "ATTESTATION_CHALLENGE",
                10
        );
        Map<String, Object> challenge = challengeMessage.getPayload();
        require(challenge != null, "Backend challenge payload is missing");

        Map<String, Object> evidence = SecureProcessAttestation.create(
                String.valueOf(challenge.get("challenge")),
                String.valueOf(challenge.get("session")),
                "2.12.0",
                String.valueOf(challenge.getOrDefault("protocolVersion", BACKEND_PROTOCOL))
        );
        send(socket, LauncherBackendMessage.of(
                "ATTESTATION_RESPONSE",
                challengeMessage.getRequestId(),
                Map.of("evidence", evidence)
        ));

        LauncherBackendMessage accepted = listener.awaitType("ATTESTATION_OK", 10);
        String token = String.valueOf(accepted.getPayload().getOrDefault("accessToken", ""));
        require(!token.isBlank(), "Backend attestation response has no access token");
        listener.awaitType("HELLO_ACK", 10);
        verifyHttpGate(endpoint, token);

        String statusId = UUID.randomUUID().toString();
        send(socket, LauncherBackendMessage.of(
                "STATUS_REQUEST",
                statusId,
                Map.of("reason", "attestation-verification")
        ));
        LauncherBackendMessage status = listener.awaitType("STATUS", 10);
        require(status.getPayload() != null && status.getPayload().containsKey("status"),
                "Attested backend status response is missing");

        socket.sendClose(WebSocket.NORMAL_CLOSURE, "verification complete")
                .get(5, TimeUnit.SECONDS);
        System.out.println("SecureProcess backend handshake verification passed.");
        System.out.println("Backend endpoint=" + endpoint);
        System.out.println("Attestation keyId=" + accepted.getPayload().get("keyId"));
        System.out.println("Access token issued=true");
    }

    private static void verifyHttpGate(URI websocketEndpoint, String token) throws Exception {
        String scheme = "wss".equalsIgnoreCase(websocketEndpoint.getScheme()) ? "https" : "http";
        URI resource = new URI(
                scheme,
                null,
                websocketEndpoint.getHost(),
                websocketEndpoint.getPort(),
                "/badges/secure-process-attestation-verification.svg",
                null,
                null
        );
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpResponse<Void> denied = client.send(
                HttpRequest.newBuilder(resource).GET().build(),
                HttpResponse.BodyHandlers.discarding()
        );
        require(denied.statusCode() == 401,
                "Protected HTTP resource did not reject missing attestation token: "
                        + denied.statusCode());

        HttpResponse<Void> accepted = client.send(
                HttpRequest.newBuilder(resource)
                        .header("X-Kaylas-Launcher-Attestation", token)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.discarding()
        );
        require(accepted.statusCode() != 401,
                "Protected HTTP resource rejected an attested access token");
    }

    private static void send(WebSocket socket, LauncherBackendMessage message) throws Exception {
        socket.sendText(GSON.toJson(message), true).get(5, TimeUnit.SECONDS);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static final class MessageListener implements WebSocket.Listener {
        private final StringBuilder partial = new StringBuilder();
        private final LinkedBlockingQueue<LauncherBackendMessage> messages = new LinkedBlockingQueue<>();
        private final CompletableFuture<Void> closed = new CompletableFuture<>();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                LauncherBackendMessage message = GSON.fromJson(
                        partial.toString(),
                        LauncherBackendMessage.class
                );
                partial.setLength(0);
                if (message != null) {
                    messages.offer(message);
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            closed.complete(null);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            closed.completeExceptionally(error);
        }

        LauncherBackendMessage awaitType(String type, int timeoutSeconds) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
            while (System.nanoTime() < deadline) {
                long remaining = deadline - System.nanoTime();
                LauncherBackendMessage message = messages.poll(
                        Math.max(1L, remaining),
                        TimeUnit.NANOSECONDS
                );
                if (message == null) {
                    break;
                }
                if ("ATTESTATION_DENIED".equals(message.getType())
                        || "ERROR".equals(message.getType())) {
                    throw new IllegalStateException(
                            "Backend rejected verification: " + message.getPayload()
                    );
                }
                if (type.equals(message.getType())) {
                    return message;
                }
            }
            throw new IllegalStateException("Timed out waiting for backend message: " + type);
        }
    }
}
