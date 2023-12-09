package org.foxesworld.engine;

import com.google.gson.Gson;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.foxesworld.APP;
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
import org.foxesworld.engine.utils.CurrentMonth;
import org.foxesworld.engine.utils.FontUtils;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.ServerInfo;
import org.foxesworld.engine.utils.md5Func;
import org.foxesworld.launcher.Auth.Auth;
import org.foxesworld.launcher.user.User;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Engine extends JFrame implements ActionListener, GuiBuilderListener {
    protected final APP APP;
    private final Sound SOUND;
    public static Logger LOGGER;
    private final Discord discord;
    private News news;
    private final  LanguageProvider LANG;
    private  final ServerInfo serverInfo;
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

    /*
    * TODO
    *  LOMBOK
    * */
    public Engine(APP APP) {
        this.APP = APP;
        this.engineData = new EngineData();
        initEngineValues(getAPP().getEngineVars());
        this.CONFIG = new Config(this);
        System.setProperty("log.dir", CONFIG.getFullPath());
        LOGGER = LogManager.getLogger(APP.class);
        LOGGER.info(engineData.getLauncherBrand() + '-' + engineData.getLauncherVersion() + " started...");
        this.getAPP().setLOCALE(String.valueOf(CONFIG.getLang()));
        this.LANG = new LanguageProvider(this.getAPP(), this.getAPP().getLocaleFile());
        this.FONTUTILS = new FontUtils(this);
        this.serverInfo = new ServerInfo(this);
        this.SOUND = new Sound(this);
        this.discord = new Discord(this);
        Configurator.setLevel(getLOGGER().getName(), Level.valueOf(CONFIG.getLogLevel()));

        this.GETrequest = new HTTPrequest(this,"GET");
        this.POSTrequest = new HTTPrequest(this,"POST");
        if(isLauncherValid()) {
            this.frameConstructor = new FrameConstructor(this);
            this.CRYPTO = new CryptUtils(this);
            setAuth(new Auth(this));
            initialize(this.auth.getAuthCredentials("login"));
        } else {
            JOptionPane.showMessageDialog(new JFrame(), "Invalid MD5!", engineData.getLauncherBrand() + '-'+ engineData.getLauncherVersion(), JOptionPane.WARNING_MESSAGE);
        }
    }

    void initEngineValues(String propertyPath){
        InputStream inputStream = Engine.class.getClassLoader().getResourceAsStream(propertyPath);
        if (inputStream != null) {
            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            setEngineData(new Gson().fromJson(reader, EngineData.class));
        }
    }

    public void initialize(String login) {
        this.discord.discordRpcStart(this.getLANG().getString("game.login") + login,"FoxesEngine"  + '-' + this.getEngineData().getLauncherVersion(),"aiden");
        getLOGGER().info("Loading engine auth(" + getAuth().isAuthorised()+")");
        setStyleProvider(new StyleProvider(this));
        this.guiBuilder = new GuiBuilder(this);
        this.guiBuilder.setGuiBuilderListener(this);
        this.news = new News(this);
        this.getGuiBuilder().buildGui(getAPP().getFrameTpl(), this.getFrame().getRootPanel());
        this.loadMainPanel(this.APP.getMainFrame());

        //ALL PANELS ARE BUILT
        this.getGuiBuilder().buildAdditionalPanels();
        user = new User(this.auth);
        this.actionHandler = new ActionHandler(this);
    }
    @Override
    public void onPanelsBuilt() {
        if(CONFIG.isEnableSound()) {
            getSOUND().playSound("uiMus.ogg", true);
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
    private void panelVisibility(String panelElement){
        String[] parts = panelElement.split("->");
        if (parts.length == 2) {
            String panelName = parts[0];
            boolean displayValue = Boolean.parseBoolean(parts[1]);
            JPanel groupPanel = guiBuilder.getPanelsMap().get(panelName);
            if(groupPanel != null) {
                groupPanel.setVisible(displayValue);
            }
        }
    }
    private void loadMainPanel(String path) {
        this.guiBuilder.buildGui(path, this.getFrame().getRootPanel());
    }

    private boolean isLauncherValid() {
        Map<String, String> launcherRequest = new HashMap<>();
        launcherRequest.put("sysRequest", "downloadLatest");
        String selfMd5 = md5Func.md5(appPath());
        LauncherAttributes launcherAttributes = new Gson().fromJson(this.POSTrequest.send(engineData.getBindUrl(), launcherRequest), LauncherAttributes.class);
        if(!selfMd5.equals("IDE")) {
            if(!Objects.equals(selfMd5, launcherAttributes.getFileMd5())) {
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public String appPath() {
        try {
            return URLDecoder.decode(HTTPrequest.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(), "UTF-8");
        } catch (UnsupportedEncodingException | java.net.URISyntaxException e) {
            return null;
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
}