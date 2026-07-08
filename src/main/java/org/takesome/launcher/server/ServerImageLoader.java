package org.takesome.launcher.server;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.utils.HTTP.HTTPrequest;
import org.takesome.kaylasEngine.utils.HTTP.HttpParam;
import org.takesome.launcher.user.User;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.function.Consumer;

/**
 * ServerImageLoader is responsible for downloading and caching a server image.
 * <p>
 * This class extends {@link HTTPrequest} and uses the modern CompletableFuture‑based asynchronous API.
 * It first checks if the server image exists in the local cache (in "cache/serverImages/"). If so,
 * it immediately returns the cached image; otherwise, it downloads the image asynchronously,
 * processes it (scales and rounds the corners), caches it locally, and then returns it.
 * </p>
 */
@SuppressWarnings("unused")
public class ServerImageLoader extends HTTPrequest {

    private static final String CACHE_DIR = "cache/serverImages/";
    private final User user;
    private final String serverUrl;

    @HttpParam
    private final String sysRequest = "serverImage";
    @HttpParam
    private String srvImgName;

    /**
     * Constructs a new ServerImageLoader instance.
     *
     * @param user      the User instance containing a reference to the Launcher and Engine
     * @param serverUrl the relative or absolute URL of the server image
     */
    public ServerImageLoader(User user, String serverUrl) {
        // Use GET for image download (or POST if required by your server)
        super(user.getLauncher(), "POST");
        this.user = user;
        this.serverUrl = serverUrl;
        ensureCacheDirectoryExists();
    }

    /**
     * Asynchronously loads the server image.
     * <p>
     * This method first checks if the image is cached locally. If it is, the cached image is returned
     * immediately. Otherwise, it downloads the image asynchronously using sendAsyncCF, processes it,
     * caches it, and then passes the result to the provided callback.
     * </p>
     *
     * @param onComplete a callback that accepts the processed BufferedImage
     */
    public void loadServerImage(Consumer<BufferedImage> onComplete) {
        if (serverUrl == null || serverUrl.isBlank()) {
            onComplete.accept(loadPlaceholderImage());
            return;
        }

        String fileName = extractFileName(serverUrl);
        this.srvImgName = fileName;
        BufferedImage cached = loadImageFromCache(fileName);
        if (cached != null) {
            onComplete.accept(cached);
            return;
        }

        sendAsyncCF(Collections.emptyMap())
                .thenApply(response -> {
                    BufferedImage downloaded = user.getLauncher().getImageUtils().fromBase64(response).get();//base64ToBufferedImage(response);
                    if (downloaded == null) {
                        downloaded = user.getLauncher().getImageUtils().getLocalImage("assets/ui/img/noimg.jpg");
                    }
                    BufferedImage scaled = (BufferedImage) user.getLauncher().getImageUtils().getScaledImage(downloaded, 470, 260);
                    BufferedImage rounded = user.getLauncher().getImageUtils().getRoundedImage(scaled, 25);
                    // Cache the processed image for future use
                    cacheImageToFile(fileName, rounded);
                    return rounded;
                })
                .exceptionally(e -> {
                    Launcher.LOGGER.error("Failed to load server image asynchronously: {}", e.getMessage());
                    return loadPlaceholderImage();
                })
                .thenAccept(onComplete);
    }

    private BufferedImage loadPlaceholderImage() {
        BufferedImage placeholder = user.getLauncher().getImageUtils().getLocalImage("assets/ui/img/noimg.jpg");
        BufferedImage scaled = (BufferedImage) user.getLauncher().getImageUtils().getScaledImage(placeholder, 470, 260);
        return user.getLauncher().getImageUtils().getRoundedImage(scaled, 25);
    }

    /**
     * Extracts the file name from the given URL.
     *
     * @param url the image URL (relative or absolute)
     * @return the file name extracted from the URL
     */
    private String extractFileName(String url) {
        String fullUrl;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            fullUrl = url;
        } else {
            fullUrl = user.getLauncher().getEngineData().getBindUrl() + url;
        }
        return fullUrl.substring(fullUrl.lastIndexOf('/') + 1);
    }

    /**
     * Loads the image from the local cache if it exists.
     *
     * @param fileName the name of the cached image file
     * @return the cached BufferedImage or null if not found or an error occurs
     */
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

    /**
     * Caches the processed image to a local file.
     *
     * @param fileName the name of the file to use for caching
     * @param image    the BufferedImage to cache
     */
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

    /**
     * Ensures that the cache directory for server images exists; creates it if necessary.
     */
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