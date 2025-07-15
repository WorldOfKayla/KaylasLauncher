// Файл: org/foxesworld/launcher/user/BetterDataLoader.java
package org.foxesworld.launcher.user;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Улучшенный универсальный загрузчик данных, который сам выполняет HTTP-запрос.
 * Он наследует HTTPrequest и инкапсулирует всю асинхронную логику.
 *
 * @param <T> Тип конечных, обработанных данных.
 */
public class BetterDataLoader<T> extends HTTPrequest {

    private final Function<Object, T> dataProcessor;

    /**
     * @param dataProcessor Функция, которая преобразует "сырые" данные (Object) в конечный результат (T).
     * @param engine        Экземпляр Engine.
     * @param requestMethod HTTP-метод, например "POST".
     */
    public BetterDataLoader(Function<Object, T> dataProcessor, Engine engine, String requestMethod) {
        super(engine, requestMethod);
        this.dataProcessor = dataProcessor;
    }

    /**
     * Запускает процесс загрузки данных с указанными параметрами.
     *
     * @param params   Параметры для HTTP-запроса.
     * @param callback Объект, который будет уведомлен о результате операции.
     */
    public void load(Map<String, Object> params, OnLoadCallback<T> callback) {
        this.sendAsyncCF(params)
                .thenApply(dataProcessor)
                .thenAccept(callback::onSuccess)
                .exceptionally(error -> {
                    // Извлекаем реальную причину ошибки, если она обернута
                    Throwable cause = error.getCause() != null ? error.getCause() : error;
                    callback.onFailure(cause);
                    return null;
                });
    }

    /**
     * Асинхронно запускает процесс загрузки и возвращает CompletableFuture с результатом.
     * Этот метод идеален для композиции нескольких асинхронных операций.
     *
     * @param params Параметры для HTTP-запроса.
     * @return CompletableFuture, который завершится с обработанным результатом типа T.
     */
    public CompletableFuture<T> loadAsync(Map<String, Object> params) {
        return this.sendAsyncCF(params)
                .thenApply(dataProcessor); // thenApply преобразует результат и пробрасывает его дальше
    }
}