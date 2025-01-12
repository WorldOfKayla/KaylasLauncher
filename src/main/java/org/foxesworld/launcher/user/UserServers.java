package org.foxesworld.launcher.user;

import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.componentAccessor.ComponentsAccessor;
import org.foxesworld.engine.gui.components.dropBox.DropBox;

import java.util.List;

public class UserServers extends ComponentsAccessor {
    private final DropBox serverListBox;
    public UserServers(GuiBuilder guiBuilder, String panelId, List<Class<?>> componentTypes) {
        super(guiBuilder, panelId, componentTypes);
        serverListBox = (DropBox) this.getComponent("serverBox");
    }

    public DropBox getServerListBox() {
        return serverListBox;
    }
}
