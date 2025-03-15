package org.foxesworld.launcher.user;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.HTTP.HttpParam;

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

public class SkinLoader extends HTTPrequest {
    private static final String CACHE_DIR = "cache/skins/";

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
        ensureCacheDirectoryExists();
    }

    /**
     * Загружает скин для обеих сторон (front и back).
     * Запросы выполняются параллельно с использованием CompletableFuture.
     * После завершения всех запросов вызывается onComplete.
     *
     * @param onComplete callback, который вызывается после загрузки скинов
     */
    public void loadSkin(Consumer<Map<String, BufferedImage>> onComplete) {
        List<String> sides = Arrays.asList("front", "back");
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String currentSide : sides) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            // Если скин уже есть в кеше, завершаем future сразу
            BufferedImage cachedSkin = loadSkinFromCache(login, currentSide);
            if (cachedSkin != null) {
                userSkin.put(currentSide, cachedSkin);
                future.complete(null);
            } else {
                // Формируем параметры запроса, включая сторону скина
                Map<String, Object> params = getAnnotatedParams();
                params.put("side", currentSide);
                this.sendAsync(params,
                        response -> {
                            handleSkinLoad(response, currentSide);
                            future.complete(null);
                        },
                        error -> {
                            handleSkinLoadError(error, currentSide);
                            future.complete(null);
                        }
                );
            }
            futures.add(future);
        }

        // После завершения всех асинхронных операций вызываем callback
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> onComplete.accept(userSkin));
    }

    /**
     * Загружает скин из кеша (файла), если он существует.
     *
     * @param login Логин пользователя.
     * @param side  Сторона скина (front или back).
     * @return Загруженное изображение или null, если кеш отсутствует.
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
     * Сохраняет загруженный скин в кеш (файл).
     *
     * @param login Логин пользователя.
     * @param side  Сторона скина (front или back).
     * @param image зображение скина.
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
     * Проверяет и создаёт директорию кеша, если её нет.
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
     * Собирает все поля, помеченные аннотацией @HttpParam,
     * и возвращает их в виде карты "имя-параметр" - "значение".
     *
     * @return карта параметров запроса
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

    /**
     * Обработка успешного ответа от сервера.
     *
     * @param response Ответ сервера
     * @param side     Сторона скина (front или back)
     */
    private void handleSkinLoad(Object response, String side) {
        Launcher.LOGGER.info("Adding skin {} side for {}", side, user.getLogin());
        BufferedImage image = this.user.getLauncher().getImageUtils()
                .base64ToBufferedImage(String.valueOf(response));
        this.userSkin.put(side, image);
        cacheSkinToFile(user.getLogin(), side, image);
    }

    /**
     * Обработка ошибки при выполнении запроса.
     *
     * @param error Ошибка выполнения запроса
     * @param side  Сторона скина (front или back)
     */
    private void handleSkinLoadError(Throwable error, String side) {
        Engine.getLOGGER().error("Skin {} request failed: {} for {}", side, error, user.getLogin());
    }
}
