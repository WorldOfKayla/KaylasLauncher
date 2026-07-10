package org.takesome.launcher.user.loader;

import org.apache.logging.log4j.Logger;
import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.launcher.auth.AuthResponse;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class BadgeLoader {

    private static final Logger LOGGER = Engine.getLOGGER();

    private final Engine engine;
    private final String login;

    public BadgeLoader(Engine engine, String login) {
        this.engine = engine;
        this.login = login;
    }

    public void loadBadgesAsync(Consumer<List<BadgeObject>> onSuccess, Consumer<Throwable> onFailure) {
        if (!(engine instanceof Launcher launcher)) {
            completeWithEmpty(onSuccess);
            return;
        }

        AuthResponse authResponse = launcher.getAuth() == null ? null : launcher.getAuth().getAuthResponse();
        if (authResponse == null || !authResponse.isBackendManaged()) {
            LOGGER.warn("Badge loading is disabled; returning empty badges for {}.", login);
            completeWithEmpty(onSuccess);
            return;
        }

        if (launcher.getBackendClient() == null) {
            LOGGER.warn("Backend client is unavailable; returning empty badges for {}.", login);
            completeWithEmpty(onSuccess);
            return;
        }

        launcher.getBackendClient().fetchBadges(login, authResponse.getUuid())
                .thenAccept(badges -> {
                    LOGGER.info("Loaded {} backend-managed badges for user {} over WS BADGES_REQUEST.", badges.size(), login);
                    if (onSuccess != null) {
                        onSuccess.accept(badges);
                    }
                })
                .exceptionally(error -> {
                    LOGGER.error("Failed to load backend badges for user {}: {}", login, error.getMessage());
                    if (onFailure != null) {
                        onFailure.accept(error);
                    }
                    return null;
                });
    }

    private void completeWithEmpty(Consumer<List<BadgeObject>> onSuccess) {
        if (onSuccess != null) {
            onSuccess.accept(Collections.emptyList());
        }
    }
}
