package org.foxesworld.launcher.user;

import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.componentAccessor.Component;
import org.foxesworld.engine.gui.componentAccessor.ComponentsAccessor;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.label.Label;

import java.util.List;

public class LoggedForm extends ComponentsAccessor {
    @Component
    private DropBox serverBox;
    @Component
    private Label greetUser, userSkin;
    public LoggedForm(GuiBuilder guiBuilder, String panelId, List<Class<?>> componentTypes) {
        super(guiBuilder, panelId, componentTypes);
    }

    public DropBox getServerBox() {
        return serverBox;
    }

    public Label getGreetUser() {
        return greetUser;
    }

    public Label getUserSkin() {
        return userSkin;
    }
}
