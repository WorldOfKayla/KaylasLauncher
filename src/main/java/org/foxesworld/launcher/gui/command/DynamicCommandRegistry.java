package org.foxesworld.launcher.gui.command;

import java.awt.event.ActionEvent;
import java.util.function.Consumer;

/**
 * Интерфейс для простой динамической регистрации команд.
 * Позволяет регистрировать, удалять и выполнять команды по их ключу.
 */
public interface DynamicCommandRegistry {
    /**
     * Регистрирует команду с уникальным ключом.
     *
     * @param key     уникальный идентификатор команды
     * @param command лямбда-выражение или метод, реализующий обработку команды
     */
    void registerCommand(String key, Consumer<ActionEvent> command);

    /**
     * Удаляет команду из реестра по указанному ключу.
     *
     * @param key уникальный идентификатор команды
     */
    void unregisterCommand(String key);

    /**
     * Выполняет команду, зарегистрированную по данному ключу.
     *
     * @param key   уникальный идентификатор команды
     * @param event событие, инициировавшее выполнение команды
     */
    void executeCommand(String key, ActionEvent event);
}
