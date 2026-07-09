package org.takesome.launcher.auth;

import java.util.concurrent.CompletableFuture;

public final class NoPasswordAuthProvider implements AuthProvider {
    @Override
    public AuthProviderType type() {
        return AuthProviderType.NO_PASSWORD;
    }

    @Override
    public CompletableFuture<AuthResponse> authorize(AuthProviderContext context) {
        String login = context.login();
        if (login == null || login.isBlank()) {
            return CompletableFuture.completedFuture(AuthResponse.failure("Login is required for no-password authorization."));
        }
        return CompletableFuture.completedFuture(AuthResponse.successNoPassword(login.trim()));
    }
}
