package org.foxesworld.engine.gui.components.serverBox;

import org.foxesworld.engine.gui.components.ComponentFactory;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class ServerBoxStyle {
    private  ComponentFactory componentFactory;

    public ServerBoxStyle(ComponentFactory componentFactory) {
        this.componentFactory = componentFactory;
    }

    public void apply(ServerBox serverBox) {
        serverBox.setFont(componentFactory.engine.getFONTUTILS().getFont(componentFactory.style.getFont(), componentFactory.style.getFontSize()));
        serverBox.setBackground(hexToColor(componentFactory.style.getColor()));
        serverBox.setForeground(hexToColor(componentFactory.style.getColor()));
        serverBox.sb = this;
    }
}