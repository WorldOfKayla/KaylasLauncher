package org.takesome.launcher.user.loader;

import org.takesome.kaylasEngine.Engine;
import org.takesome.launcher.backend.LauncherBackendClient;
import org.takesome.launcher.user.User;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Loads player skin preview images from KaylasLauncherBackend /user/skin.
 * The backend reads local ./skins and ./cloaks repositories and returns PNG images.
 */
public class SkinLoader {
    private final User user;
    private final String login;
    private final Map<String, BufferedImage> userSkin = new HashMap<>();

    public SkinLoader(User user) {
        this.user = user;
        this.login = user.getLogin();
    }

    public void loadSkin(Consumer<Map<String, BufferedImage>> onComplete) {
        LauncherBackendClient backendClient = user.getLauncher().getBackendClient();
        if (backendClient == null) {
            Engine.getLOGGER().warn("Backend client is unavailable. Using fallback skin preview for {}", login);
            userSkin.put("front", fallbackPreview("front"));
            userSkin.put("back", fallbackPreview("back"));
            onComplete.accept(userSkin);
            return;
        }

        List<CompletableFuture<Void>> futures = List.of(
                loadSide(backendClient, "front"),
                loadSide(backendClient, "back")
        );

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    userSkin.putIfAbsent("front", fallbackPreview("front"));
                    userSkin.putIfAbsent("back", fallbackPreview("back"));
                    onComplete.accept(userSkin);
                });
    }

    private CompletableFuture<Void> loadSide(LauncherBackendClient backendClient, String side) {
        return backendClient.fetchSkin(login, user.getUuid(), side)
                .thenAccept(image -> {
                    if (image != null) {
                        userSkin.put(side, image);
                        Engine.getLOGGER().info("Loaded {} skin preview for {} from backend /user/skin", side, login);
                    } else {
                        userSkin.put(side, fallbackPreview(side));
                    }
                })
                .exceptionally(error -> {
                    Engine.getLOGGER().error("Skin {} request failed for {} via backend /user/skin: {}", side, login, rootMessage(error));
                    userSkin.put(side, fallbackPreview(side));
                    return null;
                });
    }

    private BufferedImage fallbackPreview(String side) {
        BufferedImage image = new BufferedImage(96, 192, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.setColor(new Color(0, 0, 0, 0));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(new Color(0x666666));
            graphics.fillRect(32, 8, 32, 32);
            graphics.setColor(new Color(0x444444));
            graphics.fillRect(32, 40, 32, 48);
            graphics.setColor(new Color(0x777777));
            graphics.fillRect(16, 40, 16, 48);
            graphics.fillRect(64, 40, 16, 48);
            graphics.setColor(new Color(0x333333));
            graphics.fillRect(32, 88, 16, 48);
            graphics.fillRect(48, 88, 16, 48);
            if ("back".equals(side)) {
                graphics.setColor(new Color(0x222222));
                graphics.drawRect(24, 42, 48, 100);
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
