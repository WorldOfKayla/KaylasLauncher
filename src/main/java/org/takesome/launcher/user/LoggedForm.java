package org.takesome.launcher.user;

import org.takesome.kaylasEngine.gui.GuiBuilder;
import org.takesome.kaylasEngine.gui.componentAccessor.Component;
import org.takesome.kaylasEngine.gui.componentAccessor.ComponentsAccessor;
import org.takesome.kaylasEngine.gui.components.combobox.Combobox;
import org.takesome.kaylasEngine.gui.components.label.Label;

import java.util.List;

public class LoggedForm extends ComponentsAccessor {
    @Component
    private Combobox serverBox;
    @Component
    private Label greetUser, userSkin;
    public LoggedForm(GuiBuilder guiBuilder, String panelId, List<Class<?>> componentTypes) {
        super(guiBuilder, panelId, componentTypes);
    }

    public Combobox getServerBox() {
        return serverBox;
    }

    public Label getGreetUser() {
        return greetUser;
    }

    public Label getUserSkin() {
        return userSkin;
    }
}
