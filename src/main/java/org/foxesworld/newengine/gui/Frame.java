package org.foxesworld.newengine.gui;

import org.foxesworld.newengine.APP;
import org.foxesworld.newengine.locale.LanguageProvier;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Frame extends JFrame {
    protected APP app;
    private GuiBuilder guiBuilder;

    private Map<String, List<String>> elementStyles = new HashMap<>();
    private JFrame frame;
    protected LanguageProvier LANG;

    public Frame(APP app) {
        this.LANG = app.getLANG();
        this.app = app;
        this.frame = new JFrame();
        initialize();
    }

    private void initialize() {
        StyleLoader styleLoader = new StyleLoader(app);
        this.elementStyles = styleLoader.getElementStyles();
        guiBuilder = new GuiBuilder(this);
        guiBuilder.buildGui( "interface.json");
        frameComponents("test", true);
    }

    public void frameComponents(String id, boolean visible) {
        for (Component component : guiBuilder.getComponentsMap().get(id)) {
            if (visible) {
                frame.add(component);
            } else {
                frame.remove(component);
            }
        }
    }

    public Map<String, List<String>> getElementStyles() {
        return elementStyles;
    }

    public JFrame getFrame() {
        return this.frame;
    }

    public APP getApp() {
        return this.app;
    }
}
