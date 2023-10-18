package org.foxesworld.engine;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.foxesworld.APP;
import org.foxesworld.engine.action.ActionHandler;
import org.foxesworld.engine.action.Auth;
import org.foxesworld.engine.config.Config;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.LoadState;
import org.foxesworld.engine.gui.attributes.FrameAttributes;
import org.foxesworld.engine.gui.components.SystemComponents;
import org.foxesworld.engine.gui.components.frame.Frame;
import org.foxesworld.engine.gui.styles.StyleProvider;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.utils.Crypt.CryptUtils;
import org.foxesworld.engine.utils.DownloadUtils;
import org.foxesworld.engine.utils.FontUtils;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;

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
    private LoadState loadState;
    private CryptUtils cryptUtils;
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
        this.cryptUtils = new CryptUtils(this);
        initialize();
    }

    private void initialize() {
        StyleProvider styleProvider = new StyleProvider();
        this.elementStyles = styleProvider.getElementStyles();
        this.buildFrame("assets/frames/frame.json");
        this.guiBuilder = new GuiBuilder(this);
        getGuiBuilder().buildGui("assets/frames/frame.json", true, this.getFrame().getRootPanel());
        //this.loadFrames();
       this.loadMainPanel("assets/frames/mainFrame.json");
        this.loadState = new LoadState(this);
        this.auth = new Auth(this);
        this.download = new DownloadUtils(this);
        this.actionHandler = new ActionHandler(this);
    }

    private void buildFrame(String path){
        Gson gson = new Gson();
        FrameAttributes frameAttributes;
        InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(AppFrame.class.getClassLoader().getResourceAsStream(path)), StandardCharsets.UTF_8);
        frameAttributes = gson.fromJson(reader, FrameAttributes.class);
        frame.buildFrame(frameAttributes);
    }

    public void displayPanel(String displayString) {
        String[] panelElements = displayString.split("\\|");
        if (panelElements.length <= 1) {
            this.processSinglePanelDisplay(displayString);
        } else {
            for (String panelElement : panelElements) {
                this.processSinglePanelDisplay(panelElement);
            }
        }
    }

    private void processSinglePanelDisplay(String panelElement){
        String[] parts = panelElement.split("->");
        if (parts.length == 2) {
            String panelName = parts[0];
            boolean displayValue = Boolean.parseBoolean(parts[1]);

            JPanel groupPanel = guiBuilder.getPanelsMap().get(panelName);
            groupPanel.setVisible(displayValue);
            getLOGGER().debug("Setting " + panelName + " visible to " + displayValue);
        }
    }


    private void loadMainPanel(String path) {
        this.guiBuilder.buildGui(path, true, this.getFrame().getRootPanel());
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
                if(component.isEnabled()){
                    ((JCheckBox) component).setSelected((Boolean) CONFIG.get(component.getName()));
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

    public LoadState getLoadingState() {
        return loadState;
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