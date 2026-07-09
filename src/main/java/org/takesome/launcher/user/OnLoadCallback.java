package org.takesome.launcher.user;

/**
 * Универсальный интерфейс обратного вызова для асинхронных операций загрузки.
 *
 * @param <T> Тип загружаемых данных.
 */
@FunctionalInterface
public interface OnLoadCallback<T> {
    /**
     * Вызывается при успешной загрузке данных.
     *
     * @param result Загруженные данные.
     */
    void onSuccess(T result);

    /**
     * Вызывается, если во время загрузки произошла ошибка.
     * Реализация по умолчанию просто логирует ошибку.
     *
     * @param error Исключение, описывающее ошибку.
     */
    default void onFailure(Throwable error) {
        System.err.println("Data loading failed: " + error.getMessage());
        error.printStackTrace();
    }
}