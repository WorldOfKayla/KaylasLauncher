package org.foxesworld.launcher.user;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.HTTP.HttpParam;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SkinLoader extends HTTPrequest {
    private final User user;
    @HttpParam
    private final String login;
    @HttpParam
    private final String sysRequest = "skinPreview";
    private final Map<String, BufferedImage> userSkin = new HashMap<>();

    public SkinLoader(User user) {
        super(user.getLauncher(), "POST");
        this.user = user;
        this.login = user.getLogin();
    }

    public void loadSkin(Consumer<Map<String, BufferedImage>> onComplete) {
        List<String> sides = Arrays.asList("front", "back");
        CompletableFuture<?>[] futures = new CompletableFuture[sides.size()];
        for (int i = 0; i < sides.size(); i++) {
            final String currentSide = sides.get(i);
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            futures[i] = future;

            Map<String, Object> params = getAnnotatedParams();
            params.put("side", currentSide);

            this.sendAsync(params,
                    response -> handleSkinLoad(response, currentSide, future),
                    error -> handleSkinLoadError(error, currentSide, future)
            );
        }

        CompletableFuture.allOf(futures).thenRun(() -> onComplete.accept(this.userSkin)).exceptionally(ex -> {
                    Engine.getLOGGER().error("Skin loading failed: {}", ex.getMessage());
                    return null;
                });
    }

    /**
     * Метод собирает все поля, помеченные аннотацией @HttpParam,
     * и возвращает их в виде карты "имя-параметр" - "значение".
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
                    Engine.getLOGGER().error("Ошибка доступа к полю {}: {}", field.getName(), e.getMessage());
                }
            }
        }
        return params;
    }

    private void handleSkinLoad(Object response, String side, CompletableFuture<Boolean> future) {
        Launcher.LOGGER.info("Adding skin {} side for {}", side, user.getLogin());
        BufferedImage image = this.user.getLauncher().getImageUtils()
                .base64ToBufferedImage(String.valueOf(response));
        this.userSkin.put(side, image);
        future.complete(true);
    }

    private void handleSkinLoadError(Throwable error, String side, CompletableFuture<Boolean> future) {
        Engine.getLOGGER().error("Skin {} request failed: {} for {}", side, error, user.getLogin());
        future.completeExceptionally(error);
    }
}
