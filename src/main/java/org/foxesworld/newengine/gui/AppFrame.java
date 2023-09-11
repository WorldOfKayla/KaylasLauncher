package org.foxesworld.newengine.gui;

import org.foxesworld.newengine.APP;
import org.foxesworld.newengine.gui.components.StyleLoader;
import org.foxesworld.newengine.locale.LanguageProvier;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class AppFrame extends JFrame {
    protected final APP app;
    private FrameBuilder frameBuilder;
    private Map<String, Map<String, StyleLoader.StyleAttributes>> elementStyles = new HashMap<>();
    private final JFrame frame;
    protected final LanguageProvier LANG;

    public AppFrame(APP app) {
        this.LANG = app.getLANG();
        this.app = app;
        this.frame = new JFrame();
        initialize();
    }

    private void initialize() {
        StyleLoader styleLoader = new StyleLoader();
        this.elementStyles = styleLoader.getElementStyles();
        frameBuilder = new FrameBuilder(this);
        frameBuilder.buildGui( "interface.json");
        frameComponents("test", true);
    }

    public void frameComponents(String id, boolean visible) {
        for (Component component : frameBuilder.getComponentsMap().get(id)) {
            if (visible) {
                frame.add(component);
            } else {
                frame.remove(component);
            }
        }
    }

    public Map<String, Map<String, StyleLoader.StyleAttributes>> getElementStyles() {
        return elementStyles;
    }

    public JFrame getFrame() {
        return this.frame;
    }

    public APP getApp() {
        return this.app;
    }
}
