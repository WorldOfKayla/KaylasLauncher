package org.takesome.launcher.auth;

import java.util.concurrent.CompletableFuture;

public interface AuthProvider {
    AuthProviderType type();

    CompletableFuture<AuthResponse> authorize(AuthProviderContext context);
}
