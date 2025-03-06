package org.foxesworld.test.command;

import java.awt.event.ActionEvent;

@FunctionalInterface
public interface Command {
    void execute(ActionEvent event);
}
