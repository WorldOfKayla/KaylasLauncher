package org.foxesworld.engine.gui.components;

import java.awt.*;
import java.util.HashMap;

public class SystemComponents {

    private HashMap<String, Component> componentsMap = new HashMap<>();

    public void addComponent(String componentName, Component component) {
        this.componentsMap.put(componentName,component);
    }

    public HashMap<String, Component> getComponentsMap() {
        return this.componentsMap;
    }
}
