package org.foxesworld.launcher.news;

import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.componentAccessor.ComponentsAccessor;

import java.util.List;

public class NewsComponents extends ComponentsAccessor {

    public NewsComponents(GuiBuilder guiBuilder, String panelId, List<Class<?>> componentTypes) {
        super(guiBuilder, panelId, componentTypes);
    }

    public void turnOffLoader() {
        this.getComponent("newsLoading").setVisible(false);
    }
}
