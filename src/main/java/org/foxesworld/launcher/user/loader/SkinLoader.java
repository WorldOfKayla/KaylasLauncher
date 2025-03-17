package org.foxesworld.launcher.user.loader;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.HTTP.HttpParam;
import org.foxesworld.launcher.user.User;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * SkinLoader is responsible for downloading and caching user skins for both the "front" and "back" sides.
 * <p>
 * This class extends {@link HTTPrequest} and utilizes the modern CompletableFuture-based asynchronous API.
 * If a skin image is available in the cache, it is loaded directly; otherwise, an HTTP request is sent.
 * Once all skin parts have been loaded, the provided callback is executed.
 * </p>
 */
public class SkinLoader extends HTTPrequest {
    private static final String CACHE_DIR = "cache/skins/";
    private final User user;
    @HttpParam
    private final String login;

    @HttpParam
    private final String sysRequest = "skinPreview";

    private final Map<String, BufferedImage> userSkin = new HashMap<>();

    /**
     * Constructs a new SkinLoader for the specified user.
     *
     * @param user the User instance containing login information and a launcher reference
     */
    public SkinLoader(User user) {
        super(user.getLauncher(), "POST");
        this.user = user;
        this.login = user.getLogin();
        ensureCacheDirectoryExists();
    }

    /**
     * Loads the user's skin for both "front" and "back" sides asynchronously.
     * <p>
     * For each side, if a cached image exists it is used; otherwise, a request is sent to the server.
     * Once all asynchronous operations complete, the onComplete callback is invoked with the loaded skins.
     * </p>
     *
     * @param onComplete a callback that accepts a map with skin sides ("front" and "back") mapped to their respective BufferedImages
     */
    public void loadSkin(Consumer<Map<String, BufferedImage>> onComplete) {
        List<String> sides = Arrays.asList("front", "back");
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String currentSide : sides) {
            // Check if the skin image is available in the cache
            BufferedImage cachedSkin = loadSkinFromCache(login, currentSide);
            if (cachedSkin != null) {
                userSkin.put(currentSide, cachedSkin);
                futures.add(CompletableFuture.completedFuture(null));
            } else {
                // Prepare request parameters, including the skin side
                Map<String, Object> params = getAnnotatedParams();
                params.put("side", currentSide);

                CompletableFuture<Void> future = sendAsyncCF(params)
                        .thenAccept(response -> handleSkinLoad(response, currentSide))
                        .exceptionally(error -> {
                            handleSkinLoadError(error, currentSide);
                            return null;
                        });
                futures.add(future);
            }
        }

        // When all asynchronous operations are complete, call the onComplete callback with the loaded skins.
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> onComplete.accept(userSkin));
    }

    /**
     * Loads a skin image from the cache directory if it exists.
     *
     * @param login the user login
     * @param side  the skin side ("front" or "back")
     * @return the BufferedImage of the skin if found; otherwise, null
     */
    private BufferedImage loadSkinFromCache(String login, String side) {
        Path cachePath = Paths.get(CACHE_DIR + login + File.separator, side + ".png");
        if (Files.exists(cachePath)) {
            try {
                Engine.getLOGGER().info("Loading cached skin {} side for {}", side, login);
                return ImageIO.read(cachePath.toFile());
            } catch (IOException e) {
                Engine.getLOGGER().error("Failed to load cached skin {} side for {}: {}", side, login, e.getMessage());
            }
        }
        return null;
    }

    /**
     * Caches the downloaded skin image to a file.
     *
     * @param login the user login
     * @param side  the skin side ("front" or "back")
     * @param image the BufferedImage of the skin
     */
    private void cacheSkinToFile(String login, String side, BufferedImage image) {
        Path cachePath = Paths.get(CACHE_DIR + login + File.separator, side + ".png");
        try {
            ImageIO.write(image, "PNG", cachePath.toFile());
            Engine.getLOGGER().info("Skin {} side cached to file for {}", side, login);
        } catch (IOException e) {
            Engine.getLOGGER().error("Failed to cache skin {} side for {}: {}", side, login, e.getMessage());
        }
    }

    /**
     * Ensures that the cache directory for skins exists; creates it if necessary.
     */
    private void ensureCacheDirectoryExists() {
        Path cacheDir = Paths.get(CACHE_DIR + login);
        if (!Files.exists(cacheDir)) {
            try {
                Files.createDirectories(cacheDir);
                Engine.getLOGGER().info("Created cache directory: {}", CACHE_DIR);
            } catch (IOException e) {
                Engine.getLOGGER().error("Failed to create cache directory {}: {}", CACHE_DIR, e.getMessage());
            }
        }
    }

    /**
     * Collects all fields annotated with {@code @HttpParam} from this instance and returns them as a map.
     *
     * @return a map of parameter names and their corresponding values
     */
    private Map<String, Object> getAnnotatedParams() {
        Map<String, Object> params = new HashMap<>();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(HttpParam.class)) {
                field.setAccessible(true);
                try {
                    Object value = field.get(this);
                    if (value != null) {
                        params.put(field.getName(), value);
                    }
                } catch (IllegalAccessException e) {
                    Engine.getLOGGER().error("Error accessing field {}: {}", field.getName(), e.getMessage());
                }
            }
        }
        return params;
    }

    /**
     * Handles a successful server response by converting the response (expected to be a Base64-encoded image)
     * to a BufferedImage, caching it, and storing it in the userSkin map.
     *
     * @param response the server response
     * @param side     the skin side ("front" or "back")
     */
    private void handleSkinLoad(Object response, String side) {
        Launcher.LOGGER.info("Adding skin {} side for {}", side, user.getLogin());
        BufferedImage image = user.getLauncher().getImageUtils()
                .base64ToBufferedImage(String.valueOf(response));
        userSkin.put(side, image);
        cacheSkinToFile(user.getLogin(), side, image);
    }

    /**
     * Handles errors encountered during the skin download process.
     *
     * @param error the error encountered
     * @param side  the skin side ("front" or "back")
     */
    private void handleSkinLoadError(Throwable error, String side) {
        Engine.getLOGGER().error("Skin {} request failed for {}: {}", side, user.getLogin(), error.getMessage());
    }
}
