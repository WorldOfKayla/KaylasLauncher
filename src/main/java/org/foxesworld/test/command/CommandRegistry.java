package org.foxesworld.test.command;

import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.foxesworld.engine.Engine;

public final class CommandRegistry {

    private static final Map<String, Command> commands = new ConcurrentHashMap<>();

    private CommandRegistry() {
    }

    public static void register(String key, Command command) {
        commands.put(key, command);
    }

    public static void execute(String commandKey, ActionEvent event) {
        Command command = commands.get(commandKey);
        if (command != null) {
            command.execute(event);
        } else {
            Engine.getLOGGER().warn("Cant register command: " + commandKey);
        }
    }
}