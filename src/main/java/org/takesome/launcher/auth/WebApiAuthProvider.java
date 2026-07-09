package org.takesome.launcher.auth;

import com.google.gson.Gson;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public final class WebApiAuthProvider implements AuthProvider {
    private static final Gson GSON = new Gson();

    @Override
    public AuthProviderType type() {
        return AuthProviderType.WEB_API;
    }

    @Override
    public CompletableFuture<AuthResponse> authorize(AuthProviderContext context) {
        AuthRequest request = new AuthRequest(context.engine(), context.login(), context.password());
        context.auth().setAuthRequest(request);
        return request.sendAsyncCF(Collections.emptyMap())
                .thenApply(response -> GSON.fromJson(String.valueOf(response), AuthResponse.class));
    }
}
