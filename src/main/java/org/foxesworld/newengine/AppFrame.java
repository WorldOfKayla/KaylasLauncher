package org.foxesworld.newengine;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.foxesworld.newengine.action.ActionHandler;
import org.foxesworld.newengine.gui.GuiBuilder;
import org.foxesworld.newengine.gui.components.frame.Frame;
import org.foxesworld.newengine.gui.styles.StyleProvider;
import org.foxesworld.newengine.locale.LanguageProvier;
import org.foxesworld.newengine.utils.DownloadUtils;
import org.foxesworld.newengine.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

public class AppFrame extends JFrame implements ActionListener {
    protected final APP app;
    private GuiBuilder guiBuilder;
    private ActionHandler actionHandler;
    private Map<String, Map<String, StyleProvider.StyleAttributes>> elementStyles = new HashMap<>();
    private final Frame frame;
    private DownloadUtils download;
    protected final LanguageProvier LANG;

    public AppFrame(APP app) {
        this.LANG = app.getLANG();
        this.app = app;
        this.frame = new Frame(this);
        initialize();
    }

    private void initialize() {
        StyleProvider styleProvider = new StyleProvider();
        this.elementStyles = styleProvider.getElementStyles();
        this.guiBuilder = new GuiBuilder(this);
        this.loadFrames();
        this.actionHandler = new ActionHandler(this);

        this.download = new DownloadUtils(this.guiBuilder);
    }

    public void displayGroup(String id, boolean visible) {
        for (Map.Entry<String, List<Component>> entryMap : guiBuilder.getComponentsMap().entrySet()) {
            for (Component component : entryMap.getValue()) {
                this.frame.getContentPanel().add(component);
                if (entryMap.getKey().equals(id)) {
                    component.setVisible(visible);
                }

                if (entryMap.getKey().equals("download")) {
                    if (component instanceof JProgressBar) {
                        this.guiBuilder.setProgressBar((JProgressBar) component);
                    }

                    if (component instanceof JLabel) {
                        this.guiBuilder.setProgressLabel((JLabel) component);
                    }
                }
            }
        }
    }

    private void loadFrames() {
        Gson gson = new Gson();
        List loadedFrames = new ArrayList();
        InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(Frame.class.getClassLoader().getResourceAsStream("loadFrames.json")), StandardCharsets.UTF_8);
        FrameListAttributes[] array = gson.fromJson(reader, FrameListAttributes[].class);
        for (FrameListAttributes obj : array) {
            this.guiBuilder.buildGui(obj.framePath, obj.inputStream);
            loadedFrames.add(obj.frameName);
            if(obj.groupVisibility!=null) {
                for (Map entryMap : obj.groupVisibility) {
                    String group = String.valueOf(entryMap.get("groupName"));
                    boolean visible = (boolean) entryMap.get("visible");
                    displayGroup(group, visible);
                    APP.LOGGER.info("Setting "+group +" visibility to "+visible);
                }
            }

        }
        APP.LOGGER.info("Loaded Frames "+loadedFrames);
    }

    //WIP
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

    public Frame getFrame() {
        return this.frame;
    }

    public APP getApp() {
        return this.app;
    }
}
