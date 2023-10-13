package org.foxesworld.newengine.gui;

import org.foxesworld.newengine.APP;
import org.foxesworld.newengine.gui.components.StyleProvider;
import org.foxesworld.newengine.gui.components.frame.FrameBuilder;
import org.foxesworld.newengine.locale.LanguageProvier;
import org.foxesworld.newengine.utils.DownloadUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppFrame extends JFrame {
    protected final APP app;
    private FrameBuilder frameBuilder;
    private Map<String, Map<String, StyleProvider.StyleAttributes>> elementStyles = new HashMap<>();
    private final JFrame frame;

    private DownloadUtils download;
    protected final LanguageProvier LANG;

    public AppFrame(APP app) {
        this.LANG = app.getLANG();
        this.app = app;
        this.frame = new JFrame();
        initialize();
    }

    private void initialize() {
        StyleProvider styleProvider = new StyleProvider();
        this.elementStyles = styleProvider.getElementStyles();
        frameBuilder = new FrameBuilder(this);
        frameBuilder.buildGui( "interface.json");
        displayId("test", true);
        displayId("download", false);
        this.download = new DownloadUtils(this.frameBuilder);
        this.download.download("https://cdimage.debian.org/cdimage/archive/11.7.0/amd64/iso-cd/debian-11.7.0-amd64-netinst.iso", "");
    }

    public void displayId(String id, boolean visible){
        for(Map.Entry<String, List<Component>> entryMap: frameBuilder.getComponentsMap().entrySet()){
            for (Component component : entryMap.getValue()) {
                frame.add(component);
                if(entryMap.getKey().equals(id)) {
                    component.setVisible(visible);
                }

                if(entryMap.getKey().equals("download")) {
                    if (component instanceof JProgressBar) {
                        this.frameBuilder.setProgressBar((JProgressBar) component);
                        System.out.println("Setting ProgressBar component - " + component);
                    }

                    if(component instanceof JLabel){
                        this.frameBuilder.setProgressLabel((JLabel) component);
                    }
                }
            }
        }
    }

    public Map<String, Map<String, StyleProvider.StyleAttributes>> getElementStyles() {
        return elementStyles;
    }

    public JFrame getFrame() {
        return this.frame;
    }

    public APP getApp() {
        return this.app;
    }
}
