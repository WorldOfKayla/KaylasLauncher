package org.foxesworld.launcher.user;

import org.foxesworld.engine.gui.ComponentsAccessor;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.serverBox.ServerBox;

import java.util.List;

public class UserServers extends ComponentsAccessor {
    private final ServerBox serverBox;
    private final DropBox serverListBox;
    public UserServers(GuiBuilder guiBuilder, String panelId, List<Class<?>> componentTypes) {
        super(guiBuilder, panelId, componentTypes);
        serverBox = (ServerBox) this.getComponent("serverStatusBox");
        serverListBox = (DropBox) this.getComponent("serverBox");
    }

    public ServerBox getServerBox() {
        return serverBox;
    }

    public DropBox getServerListBox() {
        return serverListBox;
    }
}
