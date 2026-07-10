package org.takesome.launcher.auth;

import java.util.concurrent.CompletableFuture;

public final class WebApiAuthProvider implements AuthProvider {

    @Override
    public AuthProviderType type() {
        return AuthProviderType.WEB_API;
    }

    @Override
    public CompletableFuture<AuthResponse> authorize(AuthProviderContext context) {
        context.auth().setAuthRequest(null);
        return CompletableFuture.failedFuture(new IllegalStateException(
                "Web API authentication is disabled. Use KaylasLauncherBackend WS auth."
        ));
    }
}
