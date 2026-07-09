package org.takesome.launcher.user.loader;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.Logger;
import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.launcher.auth.AuthResponse;
import org.takesome.launcher.user.BetterDataLoader;
import org.takesome.launcher.user.OnLoadCallback;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class BadgeLoader extends BetterDataLoader<List<BadgeObject>> {

    private static final String SYS_REQUEST_NAME = "GetBadges";
    private static final Logger LOGGER = Engine.getLOGGER();

    private final Engine engine;
    private final String login;

    public BadgeLoader(Engine engine, String login) {
        super(createBadgeProcessor(), engine, "POST");
        this.engine = engine;
        this.login = login;
    }

    public void loadBadgesAsync(Consumer<List<BadgeObject>> onSuccess, Consumer<Throwable> onFailure) {
        if (engine instanceof Launcher launcher) {
            AuthResponse authResponse = launcher.getAuth() == null ? null : launcher.getAuth().getAuthResponse();
            if (authResponse != null && authResponse.isBackendManaged()) {
                if (launcher.getBackendClient() == null) {
                    LOGGER.warn("Backend client is unavailable; returning empty badges for {}.", login);
                    if (onSuccess != null) {
                        onSuccess.accept(Collections.emptyList());
                    }
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
                return;
            }
        }

        Map<String, Object> params = Map.of(
                "user_doaction", SYS_REQUEST_NAME,
                "userDisplay", this.login
        );

        this.load(params, new OnLoadCallback<>() {
            @Override
            public void onSuccess(List<BadgeObject> badges) {
                LOGGER.info("Successfully loaded {} badges for user {}.", badges.size(), login);
                if (onSuccess != null) {
                    onSuccess.accept(badges);
                }
            }

            @Override
            public void onFailure(Throwable error) {
                LOGGER.error("Failed to load badges for user {}: {}", login, error.getMessage(), error);
                if (onFailure != null) {
                    onFailure.accept(error);
                }
            }
        });
    }

    private static Function<Object, List<BadgeObject>> createBadgeProcessor() {
        return response -> {
            String json = (response != null) ? String.valueOf(response) : null;
            if (json == null || json.trim().isEmpty()) {
                LOGGER.warn("Received empty response from server for badges request.");
                return Collections.emptyList();
            }

            try {
                List<BadgeObject> parsedBadges = BadgeJsonParser.parse(json);
                return (parsedBadges != null) ? parsedBadges : Collections.emptyList();
            } catch (JsonSyntaxException e) {
                LOGGER.error("JSON syntax error while parsing badges: {}", e.getMessage());
                throw e;
            }
        };
    }

    private static class BadgeJsonParser {
        private static final Gson GSON = new Gson();
        private static final Type BADGE_LIST_TYPE = new TypeToken<List<BadgeObject>>() {}.getType();

        public static List<BadgeObject> parse(String json) throws JsonSyntaxException {
            return GSON.fromJson(json, BADGE_LIST_TYPE);
        }
    }
}
