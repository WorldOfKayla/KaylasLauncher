package org.foxesworld.newengine;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.foxesworld.newengine.action.ActionHandler;
import org.foxesworld.newengine.action.Auth;
import org.foxesworld.newengine.config.Config;
import org.foxesworld.newengine.gui.GuiBuilder;
import org.foxesworld.newengine.gui.components.SystemComponents;
import org.foxesworld.newengine.gui.components.frame.Frame;
import org.foxesworld.newengine.gui.styles.StyleProvider;
import org.foxesworld.newengine.locale.LanguageProvider;
import org.foxesworld.newengine.utils.DownloadUtils;
import org.foxesworld.newengine.utils.FontUtils;
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
    private final Logger LOGGER = LogManager.getLogger(APP.class);
    private GuiBuilder guiBuilder;
    private boolean authorised = false;
    private String LOCALE;
    private LanguageProvider LANG;
    private Map<String, Object> CONFIG;
    private Auth auth;
    private Config config;
    private HTTPrequest GETrequest,POSTrequest;
    private FontUtils fontUtils;
    private SystemComponents systemComponents;
    private ActionHandler actionHandler;
    private Map<String, Map<String, StyleProvider.StyleAttributes>> elementStyles = new HashMap<>();
    private final Frame frame;
    private DownloadUtils download;
    private String[] configFiles = new String[]{"config"};

    public AppFrame(APP app) {
        this.app = app;
        config = new Config(this);
        CONFIG = config.getCONFIG();
        LOCALE = String.valueOf(CONFIG.get("Lang"));
        this.LANG = new LanguageProvider(this, "/assets/lang/locale.json");
        this.fontUtils = new FontUtils(this);
        Configurator.setLevel(LOGGER.getName(), Level.valueOf((String) CONFIG.get("LogLevel")));
        this.GETrequest = new HTTPrequest(this,"GET");
        this.POSTrequest = new HTTPrequest(this,"POST");
        this.frame = new Frame(this);
        initialize();
    }

    private void initialize() {
        StyleProvider styleProvider = new StyleProvider();
        this.elementStyles = styleProvider.getElementStyles();
        this.guiBuilder = new GuiBuilder(this);
        this.loadFrames();
        this.auth = new Auth(this);
        this.download = new DownloadUtils(this);
        this.actionHandler = new ActionHandler(this);
    }

    public void displayPanel(String displayString) {
        String[] panelElements = displayString.split("\\|");
        if (panelElements.length <= 1) {
            this.processSinglePanel(displayString);
        } else {
            for (String panelElement : panelElements) {
                this.processSinglePanel(panelElement);
            }
        }
    }

    private void processSinglePanel(String panelElement){
        String[] parts = panelElement.split("->");
        if (parts.length == 2) {
            String panelName = parts[0];
            boolean displayValue = Boolean.parseBoolean(parts[1]);

            JPanel groupPanel = guiBuilder.getPanelsMap().get(panelName);
            groupPanel.setVisible(displayValue);
            getLOGGER().debug("Setting " + panelName + " visible to " + displayValue);
        }
    }


    private void loadFrames() {
        List loadedFrames = new ArrayList();
        InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(AppFrame.class.getClassLoader().getResourceAsStream("loadFrames.json")), StandardCharsets.UTF_8);
        FrameListAttributes[] array = new Gson().fromJson(reader, FrameListAttributes[].class);
        for (FrameListAttributes obj : array) {
            this.guiBuilder.buildGui(obj.framePath, obj.inputStream);
            loadedFrames.add(obj.frameName);
        }
        getLOGGER().info("Loaded Frames " + loadedFrames);
        this.processComponents();
    }

    private void processComponents(){
        List systemIds = Arrays.asList("progressBar", "progressLabel"); //Components we define as system
        this.systemComponents = new SystemComponents();
        for(Map.Entry<String, List<Component>> panels: guiBuilder.getComponentsMap().entrySet()){
            String panelName = panels.getKey();
            for(Component component: panels.getValue()){
                if(systemIds.contains(component.getName())){
                    this.systemComponents.addComponent(component.getName(), component);
                    getLOGGER().debug("Adding system component '" + component.getName()+"'");
                }
                this.setComponentValues(component);
            }
        }
    }

    private void setComponentValues(Component component){
        if(component instanceof  JLabel){
            String text = ((JLabel) component).getText();
        } else {
            if(component instanceof  JCheckBox) {
                ((JCheckBox) component).setSelected((Boolean) CONFIG.get(component.getName()));
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

    public Logger getLOGGER() {
        return LOGGER;
    }

    public LanguageProvider getLANG() {
        return LANG;
    }

    public Map<String, Object> getCONFIG() {
        return CONFIG;
    }

    public String getLOCALE() {
        return LOCALE;
    }

    public FontUtils getFontUtils() {
        return fontUtils;
    }

    public String[] getConfigFiles() {
        return configFiles;
    }

    public Config getConfig() {
        return config;
    }

    public Auth getAuth() {
        return auth;
    }

    public boolean isAuthorised() {
        return authorised;
    }

    public void setAuthorised(boolean authorised) {
        this.authorised = authorised;
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