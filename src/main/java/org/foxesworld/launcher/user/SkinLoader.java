package org.foxesworld.launcher.user;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SkinLoader {
    private final User user;
    private final HTTPrequest postRequest;
    private final Map<String, Object> skinData = new HashMap<>();
    private final Map<String, BufferedImage> userSkin = new HashMap<>();

    public SkinLoader(User user) {
        this.user = user;
        this.postRequest = user.getLauncher().getPOSTrequest();
        skinData.put("sysRequest", "skinPreview");
        skinData.put("login", user.getLogin());
    }

    public void loadSkin(Consumer<Map<String, BufferedImage>> onComplete) {
        List<String> sides = Arrays.asList("front", "back");
        CompletableFuture<?>[] futures = new CompletableFuture[sides.size()];
        for (int i = 0; i < sides.size(); i++) {
            String side = sides.get(i);
            Map<String, Object> requestData = new HashMap<>(skinData);
            requestData.put("side", side);

            CompletableFuture<Boolean> future = new CompletableFuture<>();
            futures[i] = future;

            postRequest.sendAsync(requestData,
                    response -> handleSkinLoad(response, side, future),
                    error -> handleSkinLoadError(error, side, future)
            );
        }

        CompletableFuture.allOf(futures)
                .thenRun(() -> onComplete.accept(this.userSkin))
                .exceptionally(ex -> {
                    Engine.getLOGGER().error("Skin loading failed: {}", ex.getMessage());
                    return null;
                });
    }

    private void handleSkinLoad(Object response, String side, CompletableFuture<Boolean> future) {
        Launcher.LOGGER.info("Adding skin {} side for {}", side, user.getLogin());
        this.userSkin.put(side, this.user.getLauncher().getImageUtils().base64ToBufferedImage(String.valueOf(response)));
        future.complete(true);
    }

    private void handleSkinLoadError(Throwable error, String side, CompletableFuture<Boolean> future) {
        Engine.getLOGGER().error("Skin {} request failed: {} for {}", side, error, user.getLogin());
        future.completeExceptionally(error);
    }
}