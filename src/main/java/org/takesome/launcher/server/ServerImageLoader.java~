package org.takesome.launcher.server;

import org.takesome.Launcher;
import org.takesome.launcher.backend.LauncherBackendClient;
import org.takesome.launcher.user.User;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Thin launcher adapter over the engine-owned asynchronous image cache.
 */
@SuppressWarnings("unused")
public final class ServerImageLoader {
    private static final Path CACHE_DIR = Paths.get("cache", "serverImages");
    private static final String IMAGE_VARIANT = "server-card-470x260-r25";
    private static final String PLACEHOLDER_RESOURCE = "assets/ui/img/noimg.jpg";

    private final User user;
    private final String serverUrl;

    public ServerImageLoader(User user, String serverUrl) {
        this.user = Objects.requireNonNull(user, "user");
        this.serverUrl = serverUrl;
    }

    public CompletableFuture<BufferedImage> loadServerImage() {
        if (serverUrl == null || serverUrl.isBlank()) {
            return CompletableFuture.completedFuture(loadPlaceholderImage());
        }

        try {
            URI source = resolveImageUri(serverUrl);
            return user.getLauncher()
                    .getAsyncImageCache()
                    .load(source, CACHE_DIR, IMAGE_VARIANT, this::prepareImage)
                    .exceptionally(error -> {
                        Launcher.LOGGER.warn(
                                "Unable to load server image '{}': {}",
                                serverUrl,
                                rootMessage(error)
                        );
                        return loadPlaceholderImage();
                    });
        } catch (RuntimeException error) {
            Launcher.LOGGER.warn("Unable to resolve server image '{}': {}", serverUrl, error.getMessage());
            return CompletableFuture.completedFuture(loadPlaceholderImage());
        }
    }

    public void loadServerImage(Consumer<BufferedImage> onComplete) {
        Objects.requireNonNull(onComplete, "onComplete");
        loadServerImage().thenAccept(onComplete);
    }

    private URI resolveImageUri(String imagePath) {
        String trimmed = imagePath == null ? "" : imagePath.trim();
        URI candidate = URI.create(trimmed);
        if (candidate.isAbsolute()) {
            return candidate;
        }

        LauncherBackendClient backendClient = user.getLauncher().getBackendClient();
        if (backendClient == null) {
            throw new IllegalStateException("Relative server image path requires KaylasLauncherBackend: " + trimmed);
        }
        String backendPath = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        return backendClient.resourceUri(backendPath);
    }

    private BufferedImage prepareImage(BufferedImage image) {
        BufferedImage scaled = (BufferedImage) user.getLauncher().getImageUtils().getScaledImage(image, 470, 260);
        return user.getLauncher().getImageUtils().getRoundedImage(scaled, 25);
    }

    private BufferedImage loadPlaceholderImage() {
        return prepareImage(user.getLauncher().getImageUtils().getLocalImage(PLACEHOLDER_RESOURCE));
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        return current == null || current.getMessage() == null
                ? String.valueOf(error)
                : current.getMessage();
    }
}
