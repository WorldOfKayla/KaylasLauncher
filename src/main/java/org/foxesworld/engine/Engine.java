package org.foxesworld.engine;

import com.google.gson.Gson;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.foxesworld.APP;
import org.foxesworld.engine.action.ActionHandler;
import org.foxesworld.engine.config.Config;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.components.SystemComponents;
import org.foxesworld.engine.gui.components.frame.FrameConstructor;
import org.foxesworld.engine.gui.styles.StyleProvider;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.sound.Sound;
import org.foxesworld.engine.utils.Crypt.CryptUtils;
import org.foxesworld.engine.utils.DownloadUtils;
import org.foxesworld.engine.utils.FontUtils;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.launcher.Auth.Auth;
import org.foxesworld.launcher.user.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Engine extends JFrame implements ActionListener {
    protected final APP APP;
    private final Sound SOUND;
    private final Logger LOGGER = LogManager.getLogger(APP.class);
    private final  LanguageProvider LANG;
    private final FontUtils FONTUTILS;
    private final Config CONFIG;
    private final CryptUtils CRYPTO;
    private final FrameConstructor frameConstructor;
    private GuiBuilder guiBuilder;
    private StyleProvider styleProvider;
    private Auth auth;
    private User user;
    private EngineData engineData;
    private final HTTPrequest GETrequest;
    private final HTTPrequest POSTrequest;
    private SystemComponents systemComponents;
    private ActionHandler actionHandler;
    private DownloadUtils download;

    /*
    * TODO
    *  LOMBOK
    * */
    public Engine(APP APP) {
        this.APP = APP;
        this.engineData = new EngineData();
        this.initEngineValues(getAPP().getEngineVars());
        this.CONFIG = new Config(this);
        this.getAPP().setLOCALE(String.valueOf(CONFIG.getCONFIG().get("Lang")));
        this.LANG = new LanguageProvider(this.getAPP(), "/assets/lang/locale.json");
        this.FONTUTILS = new FontUtils(this);
        this.SOUND = new Sound(this);
        Configurator.setLevel(getLOGGER().getName(), Level.valueOf((String) CONFIG.getCONFIG().get("LogLevel")));
        this.GETrequest = new HTTPrequest(this,"GET");
        this.POSTrequest = new HTTPrequest(this,"POST");
        this.frameConstructor = new FrameConstructor(this);
        this.CRYPTO = new CryptUtils(this);
        initialize();
    }

    @Deprecated
    private void initEngineValues(String propertyPath){
        InputStream inputStream = Engine.class.getClassLoader().getResourceAsStream(propertyPath);

        if (inputStream != null) {
            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            setEngineData(new Gson().fromJson(reader, EngineData.class));
        }
    }

    /* TODO
    *   Remove too many calls of GuiBuilder
    *   In process
    * */
    private void initialize() {
        setAuth(new Auth(this));
        getLOGGER().info("Loading engine auth(" + getAuth().isAuthorised()+")");
        setStyleProvider(new StyleProvider(this));
        this.guiBuilder = new GuiBuilder(this);
        getGuiBuilder().buildGui("assets/frames/frame.json", true, this.getFrame().getRootPanel());
        this.loadMainPanel(this.APP.getMainFrame());
        user = new User(this.auth);
        this.download = new DownloadUtils(this);
        this.actionHandler = new ActionHandler(this);
        SOUND.playSound("intro.ogg");
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
            /*
            if(displayValue == true) {
                if(guiBuilder.getChildsNparents().containsKey(panelName)){
                    for(Component component: guiBuilder.getAllChildComponents(panelName)){
                        //setComponentValues(component);
                    }
                } else {
                    for(Component component: guiBuilder.getComponentsMap().get(panelName)){
                        //setComponentValues(component);
                    }
                }
            } */
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
        List<String> systemIds = Arrays.asList("progressBar", "progressLabel");
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
    *  VERY IMPORTANT
    *  May initialValue help us */
    private void setComponentValues(Component component){
        if(component instanceof  JLabel){
            String text = ((JLabel) component).getText();
            //To replace Text on labels
        } else {
            if(component instanceof  JCheckBox) {
                if(component.isEnabled()){
                    ((JCheckBox) component).setSelected((Boolean) CONFIG.getCONFIG().get(component.getName()));
                }
            } else {
                if(component instanceof  JTextField) {
                    if(CONFIG.getCONFIG().get(component.getName()) != null)
                    ((JTextField) component).setText(String.valueOf(CONFIG.getCONFIG().get(component.getName())));
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.actionHandler.handleAction(e);
    }
    public FrameConstructor getFrame() {
        return this.frameConstructor;
    }
    public APP getAPP() {
        return this.APP;
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
    public FontUtils getFONTUTILS() {
        return FONTUTILS;
    }
    public Config getCONFIG() {
        return CONFIG;
    }
    public Auth getAuth() {
        return auth;
    }
    public StyleProvider getStyleProvider() {
        return styleProvider;
    }
    public Sound getSOUND() {
        return SOUND;
    }
    public EngineData getEngineData() {
        return engineData;
    }
    public void setAuth(Auth auth) {
        this.auth = auth;
    }
    public void setStyleProvider(StyleProvider styleProvider) {
        this.styleProvider = styleProvider;
    }
    public void setEngineData(EngineData engineData) {
        this.engineData = engineData;
    }
    public User getUser() {
        return user;
    }
}