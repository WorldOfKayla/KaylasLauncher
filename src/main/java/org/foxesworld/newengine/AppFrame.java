package org.foxesworld.newengine;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.foxesworld.newengine.action.ActionHandler;
import org.foxesworld.newengine.gui.GuiBuilder;
import org.foxesworld.newengine.gui.components.frame.Frame;
import org.foxesworld.newengine.gui.styles.StyleProvider;
import org.foxesworld.newengine.locale.LanguageProvier;
import org.foxesworld.newengine.utils.DownloadUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class AppFrame extends JFrame implements ActionListener {
    protected final APP app;
    private GuiBuilder guiBuilder;
    private ActionHandler actionHandler;
    private Map<String, Map<String, StyleProvider.StyleAttributes>> elementStyles = new HashMap<>();
    private final Frame frame;
    private DownloadUtils download;
    protected final LanguageProvier LANG;

    public AppFrame(APP app) {
        this.app = app;
        this.LANG = app.getLANG();
        this.frame = new Frame(this);
        initialize();
    }

    private void initialize() {
        StyleProvider styleProvider = new StyleProvider();
        this.elementStyles = styleProvider.getElementStyles();
        this.guiBuilder = new GuiBuilder(this);
        this.download = new DownloadUtils();
        this.actionHandler = new ActionHandler(this);
        this.loadFrames();
    }

    //Will update
    public void displayPanel(String id, boolean visible) {
        for (Map.Entry<String, JPanel> entryMap : guiBuilder.getPanelsMap().entrySet()) {
            JPanel groupPanel = entryMap.getValue();

            if (entryMap.getKey().equals(id)) {
                groupPanel.setVisible(visible);
            }

            if (entryMap.getKey().equals("download")) {
                if (groupPanel.getName() != null) {
                    for(Component component: guiBuilder.getComponentsMap(groupPanel.getName())){
                        if(this.download.getDownloadComponents().get(component.getName()) ==null) {
                            APP.LOGGER.debug("Adding " + component.getName() + " as default " + component);
                            this.download.addDownloadComponent(component.getName(), component);
                            this.download.setDownloadPanel(guiBuilder.getPanelsMap().get("download"));
                        }
                    }
                }
            }
        }
        APP.LOGGER.debug("Setting " + id + " visibility to " + visible);
        frame.getRootPanel().revalidate();
        frame.getRootPanel().repaint();
    }

    //Will update
    private void loadFrames() {
        Gson gson = new Gson();
        List loadedFrames = new ArrayList();
        InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(Frame.class.getClassLoader().getResourceAsStream("loadFrames.json")), StandardCharsets.UTF_8);
        FrameListAttributes[] array = gson.fromJson(reader, FrameListAttributes[].class);
        for (FrameListAttributes obj : array) {
            this.guiBuilder.buildGui(obj.framePath, obj.inputStream);
            APP.LOGGER.debug("Processing " + obj.framePath);
            loadedFrames.add(obj.frameName);
            if (obj.groupVisibility != null) {
                for (Map entryMap : obj.groupVisibility) {
                    String group = String.valueOf(entryMap.get("groupName"));
                    boolean visible = (boolean) entryMap.get("visible");
                    this.displayPanel(group, visible);
                }
            }
        }
        APP.LOGGER.info("Loaded Frames " + loadedFrames);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.actionHandler.handleAction(e);
    }

    private class FrameListAttributes {
        @SerializedName("frameName")
        String frameName;
        @SerializedName("framePath")
        String framePath;
        @SerializedName("inputStream")
        boolean inputStream;
        @SerializedName("groupVisibility")
        List<Map> groupVisibility;
    }

    public Map<String, Map<String, StyleProvider.StyleAttributes>> getElementStyles() {
        return elementStyles;
    }

    public DownloadUtils getDownload() {
        return download;
    }

    public GuiBuilder getGuiBuilder() {
        return guiBuilder;
    }

    public Frame getFrame() {
        return this.frame;
    }

    public APP getApp() {
        return this.app;
    }
}