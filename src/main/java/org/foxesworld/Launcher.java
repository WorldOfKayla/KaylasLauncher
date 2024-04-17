package org.foxesworld;

import com.google.gson.Gson;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.FileProperties;
import org.foxesworld.engine.utils.ImageUtils;
import org.foxesworld.engine.utils.helper.JVMHelper;
import org.foxesworld.launcher.config.Config;
import org.foxesworld.engine.discord.Discord;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.components.frame.FrameConstructor;
import org.foxesworld.engine.gui.components.frame.OptionGroups;
import org.foxesworld.engine.gui.styles.StyleProvider;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.sound.Sound;
import org.foxesworld.engine.utils.Crypt.CryptUtils;
import org.foxesworld.engine.utils.LoadingManager;
import org.foxesworld.engine.utils.ServerInfo;
import org.foxesworld.launcher.auth.AuthListener;
import org.foxesworld.launcher.news.News;
import org.foxesworld.engine.utils.HashUtils;
import org.foxesworld.launcher.auth.Auth;
import org.foxesworld.launcher.gui.Settings;
import org.foxesworld.launcher.user.User;
import org.foxesworld.launcher.gui.ActionHandler;
import org.foxesworld.launcher.gui.components.Components;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Launcher extends Engine implements AuthListener {
    private final Engine engine;
    private Auth auth;
    private User user;
    private Settings settings;
    private final FileProperties fileProperties;
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Launcher::new);
    }
    public Launcher() {
        super("config");
        this.fileProperties = getFileProperties();
        this.preInit();
        this.engine = this;
        String error;
        if (!isLauncherValid(this)) {
            error = "invalidLauncher";
            engine.getSOUND().playSound("other", error);
            getLOGGER().error(error);
            JOptionPane.showMessageDialog(new JFrame(), error, engine.getAppTitle(), JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        } else {
            int launchingWith = Integer.parseInt(JVMHelper.getJavaVersion(System.getProperty("java.home") + "/bin").replaceAll("\\D", ""));
            if(launchingWith == Integer.parseInt(getEngineData().getProgramRuntime().replaceAll("\\D", ""))) {
                this.auth = new Auth(this);
                init();
                setActionHandler(new ActionHandler(this));
                getLOGGER().debug("Launcher started!");
            } else {
                error = "invalidJVM";
                engine.getSOUND().playSound("other", error);
                getLOGGER().error(error);
                JOptionPane.showMessageDialog(new JFrame(), error, engine.getAppTitle(), JOptionPane.WARNING_MESSAGE);
                System.exit(0);
            }
        }
    }

    /*
    * TODO
    *  May be merged to Engine
    * */
    @Override
    protected void preInit(){
        this.config = new Config(this);
        this.LANG = new LanguageProvider(this, this.fileProperties.getLocaleFile(), String.valueOf(this.getConfig().getCONFIG().get("lang")));
        this.SOUND = new Sound(this, Engine.class.getClassLoader().getResourceAsStream(this.fileProperties.getSoundsFile()));
        this.frameConstructor = new FrameConstructor(this);
        this.loadingManager = new LoadingManager(this);
        this.serverInfo = new ServerInfo(this);
        this.CRYPTO = new CryptUtils(this);
    }

    public void buildGui(String[] styles){
        setStyleProvider(new StyleProvider(styles));
        setGuiBuilder(new GuiBuilder(this));
        this.getGuiBuilder().getComponentFactory().setComponentFactoryListener(new Components(this));
        getGuiBuilder().setGuiBuilderListener(this);
        this.getGuiBuilder().buildGui(this.getFileProperties().getFrameTpl(), this.getFrame().getRootPanel());
    }
    @Override
    public void init() {
        this.discord = new Discord(this);
        this.buildGui(this.getEngineData().getStyles());
        setNews(new News(this));
        loadMainPanel(this.fileProperties.getMainFrame());

        //ALL PANELS ARE BUILT
        this.getGuiBuilder().buildAdditionalPanels();
        this.setUser(new User(this));
        if(this.user.getLogin()!=null) {
            this.discord.discordRpcStart(this.getLANG().getString("game.login") + this.getAuth().getAuthCredentials("login"), getAppTitle(), "aiden");
        }
        this.settings = new Settings(this);
        this.settings.addListeners();
        setInit(true);
    }
    private boolean isLauncherValid(Engine engine) {
        Map<String, String> launcherRequest = new HashMap<>();
        launcherRequest.put("sysRequest", "downloadLatest");
        String selfMd5 = HashUtils.md5(this.appPath());
        LauncherAttributes launcherAttributes = new Gson().fromJson(engine.getPOSTrequest().send(launcherRequest), LauncherAttributes.class);
        if (!selfMd5.equals("IDE")) {
            return Objects.equals(selfMd5, launcherAttributes.getFileMd5());
        } else {
            return true;
        }
    }
    @Override
    public String appPath() {
        try {
            return URLDecoder.decode(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(), StandardCharsets.UTF_8);
        } catch (URISyntaxException var2) {
            return null;
        }
    }

    @Override
    public void onLogin(Map<String, String> authCredentials) {
    }

    public Settings getSettings() {
        return settings;
    }

    @Override
    public void onLoad(Auth auth, Map<String, String> authCredentials) {
        if (!auth.authorize(authCredentials)) {
            this.config.clearConfigData(Arrays.asList("login", "password"), true);
        }
    }

    @Override
    public void onPanelsBuilt() {
        if (!isInit() && this.getConfig().isBackgroundMusic()) {
            getSOUND().playSound("music", "launcherTheme", true);
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
    @Override
    public void actionPerformed(ActionEvent e) {
        actionHandler.handleAction(e);
    }

    public Engine getEngine() {
        return engine;
    }
    public Auth getAuth() {
        return auth;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
    public  Config getConfig() { return (Config) this.config; }


    static class LauncherAttributes {
        @SuppressWarnings("unused")
        private String fileMd5;
        @SuppressWarnings("unused")
        private String filename;
        public String getFileMd5() {
            return fileMd5;
        }
        @SuppressWarnings("unused")
        public String getFilename() {
            return filename;
        }
    }

}