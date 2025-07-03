package org.foxesworld.launcher.user;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.Function;

/**
 * Универсальный загрузчик данных, который выполняет асинхронную операцию
 * и сообщает о результате через OnLoadCallback.
 *
 * @param <R> Тип "сырого" ответа от источника (например, String для HTTP-ответа).
 * @param <T> Тип конечных, обработанных данных (например, GroupObject).
 */
public class DataLoader<R, T> {

    private final Supplier<CompletableFuture<R>> dataSupplier;
    private final Function<R, T> dataProcessor;

    /**
     * @param dataSupplier Функция, которая запускает асинхронную операцию и возвращает Future с "сырыми" данными.
     * @param dataProcessor Функция, которая преобразует "сырые" данные в конечный результат.
     */
    public DataLoader(Supplier<CompletableFuture<R>> dataSupplier, Function<R, T> dataProcessor) {
        this.dataSupplier = dataSupplier;
        this.dataProcessor = dataProcessor;
    }

    /**
     * Запускает процесс загрузки данных.
     *
     * @param callback Объект, который будет уведомлен о результате операции.
     */
    public void load(OnLoadCallback<T> callback) {
        dataSupplier.get()
                .thenApply(dataProcessor)
                .thenAccept(callback::onSuccess)
                .exceptionally(error -> {
                    callback.onFailure(error.getCause() != null ? error.getCause() : error);
                    return null;
                });
    }
}