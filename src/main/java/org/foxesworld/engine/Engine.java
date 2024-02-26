package org.foxesworld.engine;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.foxesworld.engine.config.Config;
import org.foxesworld.engine.discord.Discord;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.GuiBuilderListener;
import org.foxesworld.engine.gui.GuiProperties;
import org.foxesworld.engine.gui.components.frame.FrameConstructor;
import org.foxesworld.engine.gui.components.frame.OptionGroups;
import org.foxesworld.engine.gui.components.panel.PanelVisibility;
import org.foxesworld.engine.gui.styles.StyleProvider;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.news.News;
import org.foxesworld.engine.sound.Sound;
import org.foxesworld.engine.utils.Crypt.CryptUtils;
import org.foxesworld.engine.utils.FontUtils;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.LoadingManager;
import org.foxesworld.engine.utils.ServerInfo;
import org.foxesworld.launcher.Launcher;
import org.foxesworld.launcher.action.ActionHandler;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public abstract class Engine extends JFrame implements ActionListener, GuiBuilderListener {
    private final GuiProperties guiProperties;
    private final LoadingManager loadingManager;
    private final String configFiles;
    private Launcher launcher;
    private final String appTitle;
    private final Sound SOUND;
    public static Logger LOGGER;
    private final Discord discord;
    private News news;
    private final LanguageProvider LANG;
    private final ServerInfo serverInfo;
    private final FontUtils FONTUTILS;
    private final Config CONFIG;
    private CryptUtils CRYPTO;
    private final FrameConstructor frameConstructor;
    private final PanelVisibility panelVisibility;
    private GuiBuilder guiBuilder;
    private StyleProvider styleProvider;
    private EngineData engineData;
    private final HTTPrequest GETrequest, POSTrequest;
    public ActionHandler actionHandler;
    private boolean init = false;
    public Engine(String configFiles) {
        this.engineData = new EngineData();
        this.configFiles = configFiles;
        setEngineData(engineData.initEngineValues("engine.json"));
        guiProperties = new GuiProperties(this);
        this.CONFIG = new Config(this);
        System.setProperty("log.dir", CONFIG.getFullPath());
        LOGGER = LogManager.getLogger(Engine.class);
        appTitle = engineData.getLauncherBrand() + '-' + engineData.getLauncherVersion();
        this.panelVisibility = new PanelVisibility(this);
        LOGGER.info(appTitle + " started...");
        this.LANG = new LanguageProvider(this, this.getGuiProperties().getLocaleFile());
        this.FONTUTILS = new FontUtils(this);
        this.serverInfo = new ServerInfo(this);
        this.SOUND = new Sound(this);
        this.discord = new Discord(this);
        Configurator.setLevel(getLOGGER().getName(), Level.valueOf(CONFIG.getLogLevel()));

        this.GETrequest = new HTTPrequest(this, "GET");
        this.POSTrequest = new HTTPrequest(this, "POST");
        this.frameConstructor = new FrameConstructor(this);
        this.loadingManager = new LoadingManager(this);
        this.CRYPTO = new CryptUtils(this);
    }
    public abstract void initialize(Launcher launcher);
    @Override
    public abstract void onPanelsBuilt();
    @Override
    public abstract void onPanelBuild(Map<String, OptionGroups> groups, String componentGroup, JPanel parentPanel);
    @Override
    public abstract void actionPerformed(ActionEvent e);
    protected void loadMainPanel(String path) {
        this.guiBuilder.buildGui(path, this.getFrame().getRootPanel());
    }
    public String appPath() {
        try {
            return URLDecoder.decode(HTTPrequest.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(),StandardCharsets.UTF_8);
        } catch (java.net.URISyntaxException e) {
            return null;
        }
    }
    public String[] getConfigFiles() {
        return configFiles.split(",");
    }
    public boolean isInit() {
        return init;
    }
    public FrameConstructor getFrame() {
        return this.frameConstructor;
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
    public StyleProvider getStyleProvider() {
        return styleProvider;
    }
    public Sound getSOUND() {
        return SOUND;
    }
    public EngineData getEngineData() {
        return engineData;
    }
    public void setStyleProvider(StyleProvider styleProvider) {
        this.styleProvider = styleProvider;
    }
    public void setEngineData(EngineData engineData) {
        this.engineData = engineData;
    }
    public ServerInfo getServerInfo() {
        return serverInfo;
    }
    public Discord getDiscord() {
        return discord;
    }
    public String getAppTitle() {
        return appTitle;
    }
    public PanelVisibility getPanelVisibility() {
        return panelVisibility;
    }
    public void setActionHandler(ActionHandler actionHandler) {
        this.actionHandler = actionHandler;
    }
    public Launcher getLauncher() {
        return launcher;
    }
    public void setLauncher(Launcher launcher) {
        this.launcher = launcher;
    }
    public void setGuiBuilder(GuiBuilder guiBuilder) {
        this.guiBuilder = guiBuilder;
    }
    public void setNews(News news) {
        this.news = news;
    }
    public void setInit(boolean init) {
        this.init = init;
    }
    public GuiProperties getGuiProperties() {
        return guiProperties;
    }
    public LoadingManager getLoadingManager() {
        return loadingManager;
    }
}