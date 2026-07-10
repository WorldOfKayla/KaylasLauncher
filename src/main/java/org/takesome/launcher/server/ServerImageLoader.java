package org.takesome.launcher.server;

import org.takesome.Launcher;
import org.takesome.launcher.backend.LauncherBackendClient;
import org.takesome.launcher.user.User;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Loads server images from local cache, absolute URLs, or KaylasLauncherBackend resource mounts.
 *
 * <p>{@code serverImage} requests are intentionally disabled.</p>
 */
@SuppressWarnings("unused")
public class ServerImageLoader {

    private static final String CACHE_DIR = "cache/serverImages/";
    private final User user;
    private final String serverUrl;

    public ServerImageLoader(User user, String serverUrl) {
        this.user = user;
        this.serverUrl = serverUrl;
        ensureCacheDirectoryExists();
    }

    public void loadServerImage(Consumer<BufferedImage> onComplete) {
        if (serverUrl == null || serverUrl.isBlank()) {
            onComplete.accept(loadPlaceholderImage());
            return;
        }

        String fileName = extractFileName(serverUrl);
        BufferedImage cached = loadImageFromCache(fileName);
        if (cached != null) {
            onComplete.accept(cached);
            return;
        }

        CompletableFuture.supplyAsync(() -> loadRemoteOrBackendImage(serverUrl))
                .thenApply(downloaded -> {
                    if (downloaded == null) {
                        downloaded = user.getLauncher().getImageUtils().getLocalImage("assets/ui/img/noimg.jpg");
                    }
                    BufferedImage scaled = (BufferedImage) user.getLauncher().getImageUtils().getScaledImage(downloaded, 470, 260);
                    BufferedImage rounded = user.getLauncher().getImageUtils().getRoundedImage(scaled, 25);
                    cacheImageToFile(fileName, rounded);
                    return rounded;
                })
                .exceptionally(e -> {
                    Launcher.LOGGER.error("Failed to load server image asynchronously: {}", e.getMessage());
                    return loadPlaceholderImage();
                })
                .thenAccept(onComplete);
    }

    private BufferedImage loadRemoteOrBackendImage(String imagePath) {
        try {
            URL url = resolveImageUrl(imagePath);
            return ImageIO.read(url);
        } catch (Exception error) {
            Launcher.LOGGER.warn("Unable to load server image '{}': {}", imagePath, error.getMessage());
            return null;
        }
    }

    private URL resolveImageUrl(String imagePath) throws Exception {
        String trimmed = imagePath == null ? "" : imagePath.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return URI.create(trimmed).toURL();
        }

        LauncherBackendClient backendClient = user.getLauncher().getBackendClient();
        if (backendClient == null) {
            throw new IllegalStateException("Relative server image path requires KaylasLauncherBackend: " + trimmed);
        }
        String backendPath = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        return backendClient.resourceUri(backendPath).toURL();
    }

    private BufferedImage loadPlaceholderImage() {
        BufferedImage placeholder = user.getLauncher().getImageUtils().getLocalImage("assets/ui/img/noimg.jpg");
        BufferedImage scaled = (BufferedImage) user.getLauncher().getImageUtils().getScaledImage(placeholder, 470, 260);
        return user.getLauncher().getImageUtils().getRoundedImage(scaled, 25);
    }

    private String extractFileName(String url) {
        String safe = url == null || url.isBlank() ? "server-image.png" : url.trim();
        int slash = safe.lastIndexOf('/');
        String fileName = slash >= 0 ? safe.substring(slash + 1) : safe;
        if (fileName.isBlank()) {
            return "server-image.png";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private BufferedImage loadImageFromCache(String fileName) {
        Path cachePath = Paths.get(CACHE_DIR, fileName);
        if (Files.exists(cachePath)) {
            try {
                Launcher.LOGGER.info("Loading cached server image: {}", fileName);
                return ImageIO.read(cachePath.toFile());
            } catch (IOException e) {
                Launcher.LOGGER.error("Failed to load cached server image {}: {}", fileName, e.getMessage());
            }
        }
        return null;
    }

    private void cacheImageToFile(String fileName, BufferedImage image) {
        Path cacheDir = Paths.get(CACHE_DIR);
        if (!Files.exists(cacheDir)) {
            try {
                Files.createDirectories(cacheDir);
            } catch (IOException e) {
                Launcher.LOGGER.error("Failed to create server image cache directory: {}", e.getMessage());
            }
        }
        Path cachePath = cacheDir.resolve(fileName);
        try {
            ImageIO.write(image, "PNG", cachePath.toFile());
            Launcher.LOGGER.info("Server image cached: {}", fileName);
        } catch (IOException e) {
            Launcher.LOGGER.error("Failed to cache server image {}: {}", fileName, e.getMessage());
        }
    }

    private void ensureCacheDirectoryExists() {
        Path cacheDir = Paths.get(CACHE_DIR);
        if (!Files.exists(cacheDir)) {
            try {
                Files.createDirectories(cacheDir);
                Launcher.LOGGER.info("Created server image cache directory: {}", CACHE_DIR);
            } catch (IOException e) {
                Launcher.LOGGER.error("Failed to create server image cache directory {}: {}", CACHE_DIR, e.getMessage());
            }
        }
    }
}
