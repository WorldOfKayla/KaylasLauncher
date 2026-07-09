package org.takesome.launcher.auth;

import org.takesome.launcher.backend.LauncherBackendClient;
import org.takesome.launcher.backend.LauncherBackendMessage;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class WsAuthProvider implements AuthProvider {
    @Override
    public AuthProviderType type() {
        return AuthProviderType.WS;
    }

    @Override
    public CompletableFuture<AuthResponse> authorize(AuthProviderContext context) {
        LauncherBackendClient backendClient = context.launcher().getBackendClient();
        if (backendClient == null) {
            return CompletableFuture.completedFuture(AuthResponse.transientFailure("Launcher backend client is not initialized."));
        }

        return backendClient.authenticate(context.login(), context.password())
                .thenApply(this::toAuthResponse)
                .exceptionally(error -> AuthResponse.transientFailure("Backend authorization failed: " + rootMessage(error)));
    }

    private AuthResponse toAuthResponse(LauncherBackendMessage message) {
        if (message == null || message.getType() == null) {
            return AuthResponse.transientFailure("Backend returned empty authorization response.");
        }

        Map<String, Object> payload = message.getPayload();
        if ("AUTH_OK".equals(message.getType())) {
            Object userObject = payload == null ? null : payload.get("user");
            if (userObject instanceof Map<?, ?> user) {
                String uuid = stringValue(user.get("uuid"));
                String username = stringValue(user.get("username"));
                String displayName = stringValue(user.get("displayName"));
                String role = stringValue(user.get("role"));
                return AuthResponse.successBackend(uuid, username, displayName, role);
            }
            return AuthResponse.transientFailure("Backend AUTH_OK response has no user payload.");
        }

        String reason = payload == null ? "Backend authorization denied." : stringValue(payload.get("reason"));
        if (reason == null || reason.isBlank()) {
            reason = "Backend authorization denied.";
        }
        return AuthResponse.failure(reason, shouldClearCredentials(reason));
    }

    private boolean shouldClearCredentials(String reason) {
        return "INVALID_CREDENTIALS".equalsIgnoreCase(reason)
                || "USER_DISABLED".equalsIgnoreCase(reason)
                || "USER_LOCKED".equalsIgnoreCase(reason);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
