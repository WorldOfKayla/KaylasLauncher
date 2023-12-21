package org.foxesworld.engine;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.foxesworld.engine.action.ActionHandler;
import org.foxesworld.engine.config.Config;
import org.foxesworld.engine.discord.Discord;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.GuiBuilderListener;
import org.foxesworld.engine.gui.components.frame.FrameConstructor;
import org.foxesworld.engine.gui.components.frame.OptionGroups;
import org.foxesworld.engine.gui.styles.StyleProvider;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.news.News;
import org.foxesworld.engine.sound.Sound;
import org.foxesworld.engine.utils.Crypt.CryptUtils;
import org.foxesworld.engine.utils.FontUtils;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.ServerInfo;
import org.foxesworld.launcher.Auth.Auth;
import org.foxesworld.launcher.User.User;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Engine extends JFrame implements ActionListener, GuiBuilderListener {

    //Initial CONSTANTS required for Engine loading
    private String[] bootstrapKeys = {"frameTpl", "mainFrame", "localeFile", "engineVars", "configFiles"};
    private String frameTpl, mainFrame, localeFile, engineVars,configFiles;
    private  String appTitle;
    private final Sound SOUND;
    public static Logger LOGGER;
    private final Discord discord;
    private News news;
    private final LanguageProvider LANG;
    private final ServerInfo serverInfo;
    private final FontUtils FONTUTILS;
    private final Config CONFIG;
    private CryptUtils CRYPTO;
    private FrameConstructor frameConstructor;
    private GuiBuilder guiBuilder;
    private StyleProvider styleProvider;
    private Auth auth;
    private User user;
    private EngineData engineData;
    private final HTTPrequest GETrequest, POSTrequest;
    private ActionHandler actionHandler;
    private boolean init = false;

    public Engine(String bootstrapFile) {
        this.engineData = new EngineData();
        this.readBootstrapValues(bootstrapFile);
        initEngineValues(this.engineVars);
        this.CONFIG = new Config(this);
        System.setProperty("log.dir", CONFIG.getFullPath());
        LOGGER = LogManager.getLogger(Engine.class);
        appTitle = engineData.getLauncherBrand() + '-' + engineData.getLauncherVersion();
        LOGGER.info(appTitle + " started...");
        this.LANG = new LanguageProvider(this, this.localeFile);
        this.FONTUTILS = new FontUtils(this);
        this.serverInfo = new ServerInfo(this);
        this.SOUND = new Sound(this);
        this.discord = new Discord(this);
        Configurator.setLevel(getLOGGER().getName(), Level.valueOf(CONFIG.getLogLevel()));

        this.GETrequest = new HTTPrequest(this, "GET");
        this.POSTrequest = new HTTPrequest(this, "POST");
        this.frameConstructor = new FrameConstructor(this);
        this.CRYPTO = new CryptUtils(this);
        setAuth(new Auth(this));
        initialize(this.auth.getAuthCredentials("login"));
    }

    private void readBootstrapValues(String jsonPath) {
        try (InputStream inputStream = Engine.class.getClassLoader().getResourceAsStream(jsonPath)) {
            JsonObject configJson = new Gson().fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), JsonObject.class);
            for (String key : bootstrapKeys) {
                try {
                    Field field = Engine.class.getDeclaredField(key);
                    if(field.hashCode()!= 0) {
                        field.set(this,  configJson.get(key).getAsString());
                    }
                } catch (NoSuchFieldException | IllegalAccessException ignored) {}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void initEngineValues(String propertyPath) {
        InputStream inputStream = Engine.class.getClassLoader().getResourceAsStream(propertyPath);
        if (inputStream != null) {
            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            setEngineData(new Gson().fromJson(reader, EngineData.class));
        }
    }

    public void initialize(String login) {
        this.discord.discordRpcStart(this.getLANG().getString("game.login") + login, "FoxesEngine" + '-' + this.getEngineData().getLauncherVersion(), "aiden");
        getLOGGER().info("Loading engine auth(" + getAuth().isAuthorised() + ")");
        setStyleProvider(new StyleProvider(this));
        this.guiBuilder = new GuiBuilder(this);
        this.guiBuilder.setGuiBuilderListener(this);
        this.news = new News(this);
        this.getGuiBuilder().buildGui(this.frameTpl, this.getFrame().getRootPanel());
        this.loadMainPanel(this.mainFrame);

        //ALL PANELS ARE BUILT
        this.getGuiBuilder().buildAdditionalPanels();
        user = new User(this.auth);
        this.actionHandler = new ActionHandler(this);
        init = true;
    }
    @Override
    public void onPanelsBuilt() {
        if (CONFIG.isEnableSound()) {
            if (!isInit()) {
                getSOUND().playSound("uiMus.ogg", true);
            }
        }
    }
    @Override
    public void onPanelBuild(Map<String, OptionGroups> groups, String componentGroup, JPanel parentPanel) {
        parentPanel.updateUI();
        parentPanel.repaint();
        parentPanel.revalidate();
        parentPanel.setDoubleBuffered(true);
        LOGGER.debug("Built panel {} with parent {}", componentGroup, parentPanel.getName());
    }
    public void displayPanel(String displayString) {
        String[] panelElements = displayString.split("\\|");
        if (panelElements.length <= 1) {
            this.panelVisibility(displayString);
        } else {
            for (String panelElement : panelElements) {
                this.panelVisibility(panelElement);
            }
        }
    }
    private void panelVisibility(String panelElement) {
        String[] parts = panelElement.split("->");
        if (parts.length == 2) {
            String panelName = parts[0];
            boolean displayValue = Boolean.parseBoolean(parts[1]);
            JPanel groupPanel = guiBuilder.getPanelsMap().get(panelName);
            if (groupPanel != null) {
                groupPanel.setVisible(displayValue);
            }
        }
    }
    private void loadMainPanel(String path) {
        this.guiBuilder.buildGui(path, this.getFrame().getRootPanel());
    }

    public String appPath() {
        try {
            return URLDecoder.decode(
                    HTTPrequest.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(),
                    StandardCharsets.UTF_8);
        } catch (java.net.URISyntaxException e) {
            return null;
        }
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        this.actionHandler.handleAction(e);
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
    public ServerInfo getServerInfo() {
        return serverInfo;
    }
    public Discord getDiscord() {
        return discord;
    }

    public String getAppTitle() {
        return appTitle;
    }
}