package org.takesome.launcher.user.loader;

import org.takesome.kaylasEngine.Engine;
import org.takesome.launcher.backend.LauncherBackendClient;
import org.takesome.launcher.user.User;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Loads player head image from KaylasLauncherBackend /user/head.
 * The launcher does not read local skin repositories directly; the backend serves PNG data
 * from its local skins/cloaks repositories.
 */
public class HeadLoader {
    private final User user;

    public HeadLoader(User user, String requestMethod) {
        this.user = user;
    }

    public void getUserHeadAsync(String login,
                                 Consumer<BufferedImage> onSuccess,
                                 Consumer<Exception> onFailure) {
        if (login == null || login.isEmpty()) {
            Engine.getLOGGER().warn("Login is null or empty in getUserHead");
            if (onFailure != null) {
                onFailure.accept(new IllegalArgumentException("Login cannot be null or empty"));
            }
            return;
        }

        LauncherBackendClient backendClient = user.getLauncher().getBackendClient();
        if (backendClient == null) {
            if (onFailure != null) {
                onFailure.accept(new IllegalStateException("Launcher backend client is unavailable."));
            }
            return;
        }

        backendClient.fetchHead(login, user.getUuid())
                .thenAccept(image -> {
                    if (image != null) {
                        onSuccess.accept(image);
                    } else if (onFailure != null) {
                        onFailure.accept(new IllegalStateException("Backend returned empty head image."));
                    }
                })
                .exceptionally(error -> {
                    Engine.getLOGGER().error("Failed to retrieve user head from backend for login: {}", login, error);
                    if (onFailure != null) {
                        onFailure.accept(error instanceof Exception exception ? exception : new Exception(error));
                    }
                    return null;
                });
    }
}
