package org.foxesworld.newengine;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import org.foxesworld.newengine.action.ActionHandler;
import org.foxesworld.newengine.gui.GuiBuilder;
import org.foxesworld.newengine.gui.components.SystemComponents;
import org.foxesworld.newengine.gui.components.frame.Frame;
import org.foxesworld.newengine.gui.styles.StyleProvider;
import org.foxesworld.newengine.locale.LanguageProvider;
import org.foxesworld.newengine.utils.DownloadUtils;
import org.foxesworld.newengine.utils.HTTP.HTTPrequest;

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
    private HTTPrequest GETrequest,POSTrequest;
    private SystemComponents systemComponents;
    private ActionHandler actionHandler;
    private Map<String, Map<String, StyleProvider.StyleAttributes>> elementStyles = new HashMap<>();
    private final Frame frame;
    private DownloadUtils download;
    protected final LanguageProvider LANG;

    public AppFrame(APP app) {
        this.app = app;
        this.LANG = app.getLANG();
        this.GETrequest = new HTTPrequest("GET");
        this.POSTrequest = new HTTPrequest("POST");
        this.frame = new Frame(this);
        initialize();
    }

    private void initialize() {
        StyleProvider styleProvider = new StyleProvider();
        this.elementStyles = styleProvider.getElementStyles();
        this.guiBuilder = new GuiBuilder(this);
        this.loadFrames();
        this.download = new DownloadUtils(this);
        this.actionHandler = new ActionHandler(this);
    }

    public void  displayPanel(String json){
        JsonArray jsonArray = new JsonParser().parse(json).getAsJsonArray();
        DisplayAttributes[] panels = new Gson().fromJson(jsonArray, DisplayAttributes[].class);
        for(DisplayAttributes panel: panels){
            JPanel groupPanel = guiBuilder.getPanelsMap().get(panel.panel);
           groupPanel.setVisible(panel.display);
            APP.LOGGER.debug("Setting " + panel.panel + " visible to " + panel.display);
        }
    }

    private void loadFrames() {
        List loadedFrames = new ArrayList();
        InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(Frame.class.getClassLoader().getResourceAsStream("loadFrames.json")), StandardCharsets.UTF_8);
        FrameListAttributes[] array = new Gson().fromJson(reader, FrameListAttributes[].class);
        for (FrameListAttributes obj : array) {
            this.guiBuilder.buildGui(obj.framePath, obj.inputStream);
            loadedFrames.add(obj.frameName);
        }
        APP.LOGGER.info("Loaded Frames " + loadedFrames);
        this.defineSystemComponents();
    }

    private void defineSystemComponents(){
        List systemIds = Arrays.asList("progressBar", "progressLabel");
        this.systemComponents = new SystemComponents();
        for(Map.Entry<String, List<Component>> panels: guiBuilder.getComponentsMap().entrySet()){
            String panelName = panels.getKey();
            for(Component component: panels.getValue()){
                if(systemIds.contains(component.getName())){
                    this.systemComponents.addComponent(component.getName(), component);
                    APP.LOGGER.debug("Adding system component '" + component.getName()+"'");
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.actionHandler.handleAction(e);
    }

    public Map<String, Map<String, StyleProvider.StyleAttributes>> getElementStyles() {
        return elementStyles;
    }

    public Frame getFrame() {
        return this.frame;
    }

    public APP getApp() {
        return this.app;
    }

    public SystemComponents getSystemComponents() {
        return systemComponents;
    }

    public DownloadUtils getDownload() {
        return download;
    }

    public GuiBuilder getGuiBuilder() {
        return guiBuilder;
    }

    public HTTPrequest getGETrequest() {
        return GETrequest;
    }

    public HTTPrequest getPOSTrequest() {
        return POSTrequest;
    }

    @Deprecated
    private class FrameListAttributes {
        @SerializedName("frameName")
        String frameName;
        @SerializedName("framePath")
        String framePath;
        @SerializedName("inputStream")
        boolean inputStream;
    }

    private class DisplayAttributes {
        @SerializedName("panel")
        private String panel;
        @SerializedName("display")
        private  boolean display;
    }
}