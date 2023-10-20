package org.foxesworld.engine;

import com.google.gson.Gson;
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
import org.foxesworld.engine.gui.components.SystemComponents;
import org.foxesworld.engine.gui.components.frame.Frame;
import org.foxesworld.engine.gui.styles.StyleProvider;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.sound.Sound;
import org.foxesworld.engine.utils.Crypt.CryptUtils;
import org.foxesworld.engine.utils.DownloadUtils;
import org.foxesworld.engine.utils.FontUtils;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class Engine extends JFrame implements ActionListener {
    protected final APP app;

    private EngineData engineData;
    private final Logger LOGGER = LogManager.getLogger(APP.class);
    private GuiBuilder guiBuilder;
    private StyleProvider styleProvider;
    private LoadState loadState;
    private CryptUtils cryptUtils;
    private Sound sound;
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

    public Engine(APP app) {
        this.app = app;
        this.engineData = new EngineData();
        this.initEngineValues(getApp().getEngineVars());

        this.config = new Config(this);
        this.CONFIG = config.getCONFIG();
        this.LOCALE = String.valueOf(CONFIG.get("Lang"));
        this.LANG = new LanguageProvider(this, "/assets/lang/locale.json");
        this.fontUtils = new FontUtils(this);
        this.sound = new Sound(this);
        Configurator.setLevel(LOGGER.getName(), Level.valueOf((String) CONFIG.get("LogLevel")));
        this.GETrequest = new HTTPrequest(this,"GET");
        this.POSTrequest = new HTTPrequest(this,"POST");
        this.frame = new Frame(this);
        this.cryptUtils = new CryptUtils(this);
        initialize();
    }

    @Deprecated
    private void initEngineValues(String propertyPath){
        InputStream inputStream = Engine.class.getClassLoader().getResourceAsStream(propertyPath);

        if (inputStream != null) {
            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            engineData = new Gson().fromJson(reader, EngineData.class);
        }
    }

    /* TODO
    *   Remove too many calls of GuiBuilder
    *   In process
    * */
    private void initialize() {
        styleProvider = new StyleProvider(this);
        this.elementStyles = styleProvider.getElementStyles();
        this.guiBuilder = new GuiBuilder(this);
        getGuiBuilder().buildGui("assets/frames/frame.json", true, this.getFrame().getRootPanel());
        this.loadMainPanel(this.app.getMainFrame());
        this.loadState = new LoadState(this);
        this.auth = new Auth(this);
        this.download = new DownloadUtils(this);
        this.actionHandler = new ActionHandler(this);
        sound.playSound("intro.ogg");
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
            if(displayValue == true) {
                if(guiBuilder.getChildsNparents().containsKey(panelName)){
                    for(Component component: guiBuilder.getAllChildComponents(panelName)){
                        setComponentValues(component);
                    }
                } else {
                    for(Component component: guiBuilder.getComponentsMap().get(panelName)){
                        setComponentValues(component);
                    }
                }

            }
        }
    }

    private void loadMainPanel(String path) {
        this.guiBuilder.buildGui(path, true, this.getFrame().getRootPanel());
        this.defineSystemComponents();
    }

    /* NOTE
    *  May system components will be removed soon as we're planing to define them
    *  While loading a new panel */
    @Deprecated
    private void defineSystemComponents(){
        List systemIds = Arrays.asList("progressBar", "progressLabel");
        this.systemComponents = new SystemComponents();
        for(Map.Entry<String, List<Component>> panels: guiBuilder.getComponentsMap().entrySet()){
            String panelName = panels.getKey();
            for(Component component: panels.getValue()){
                if(systemIds.contains(component.getName())){
                    this.systemComponents.addComponent(component.getName(), component);
                    getLOGGER().debug("Adding system component '" + component.getName()+"'");
                }
            }
        }
    }

    /*
    * TODO
    *  We should specify which form to perform to avoid setting values of unneeded components
    *  VERY IMPORTANT */
    private void setComponentValues(Component component){
        if(component instanceof  JLabel){
            String text = ((JLabel) component).getText();
            //To replace Text on labels
        } else {
            if(component instanceof  JCheckBox) {
                if(component.isEnabled()){
                    ((JCheckBox) component).setSelected((Boolean) CONFIG.get(component.getName()));
                }
            } else {
                if(component instanceof  JTextField) {
                    if(CONFIG.get(component.getName()) != null)
                    ((JTextField) component).setText(String.valueOf(CONFIG.get(component.getName())));
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

    public StyleProvider getStyleProvider() {
        return styleProvider;
    }

    public Sound getSound() {
        return sound;
    }

    public LoadState getLoadingState() {
        return loadState;
    }

    public EngineData getEngineData() {
        return engineData;
    }
}