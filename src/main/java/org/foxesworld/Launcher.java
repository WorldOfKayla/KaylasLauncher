package org.foxesworld;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.discord.Discord;
import org.foxesworld.engine.gui.FileProperties;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.components.frame.FrameConstructor;
import org.foxesworld.engine.gui.components.frame.OptionGroups;
import org.foxesworld.engine.gui.styles.StyleProvider;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.sound.Sound;
import org.foxesworld.engine.utils.Crypt.CryptUtils;
import org.foxesworld.engine.utils.IconUtils;
import org.foxesworld.engine.utils.ServerInfo;
import org.foxesworld.launcher.LauncherValidator;
import org.foxesworld.launcher.auth.Auth;
import org.foxesworld.launcher.auth.AuthListener;
import org.foxesworld.launcher.config.Config;
import org.foxesworld.launcher.gui.ActionHandler;
import org.foxesworld.launcher.gui.Settings;
import org.foxesworld.launcher.gui.SplashScreenWindow;
import org.foxesworld.launcher.gui.components.ComponentManager;
import org.foxesworld.launcher.gui.loadingManager.LoadStatus;
import org.foxesworld.launcher.news.News;
import org.foxesworld.launcher.user.User;
import org.foxesworld.notification.NotificationPopup;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Launcher extends Engine implements AuthListener {
    private final Auth auth;
    private User user;
    private Settings settings;
    private final FileProperties fileProperties;
    private IconUtils iconUtils;
    private final File launcherFile;
    private final NotificationPopup notification;
    private static final List<String> CONFIG_FILES = List.of("config");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Launcher::showSplashAndStartLauncher);
    }

    private static void showSplashAndStartLauncher() {
        SplashScreenWindow splashScreen = new SplashScreenWindow();
        splashScreen.showSplashScreen();

        Timer launchTimer = new Timer(1600, e -> new Launcher());
        launchTimer.setRepeats(false);
        launchTimer.start();
    }

    public Launcher() {
        super(CONFIG_FILES);
        long startTime = System.currentTimeMillis();

        this.launcherFile = new File(appPath());
        this.fileProperties = getFileProperties();
        this.notification = new NotificationPopup();

        preInit();
        new LauncherValidator(this).validate();

        this.auth = new Auth(this);
        init();

        logStartupTime(startTime);
    }

    @Override
    protected void preInit() {
        this.config = new Config(this);
        this.LANG = new LanguageProvider(this, fileProperties.getLocaleFile(), getConfig().getLang());
        this.SOUND = new Sound(this, getClass().getClassLoader().getResourceAsStream(fileProperties.getSoundsFile()));
        this.frameConstructor = new FrameConstructor(this);
        this.serverInfo = new ServerInfo(this);
        this.CRYPTO = new CryptUtils(this);
    }

    @Override
    public void init() {
        setupDiscord();
        buildGui(getEngineData().getStyles());
        loadMainPanel(fileProperties.getMainFrame());
        setNews();
        getGuiBuilder().buildAdditionalPanels();
        this.loadingManager = new LoadStatus(this, getConfig().getLoaderIndex());
        this.settings = new Settings(this);
        this.settings.addListeners();
        setActionHandler(new ActionHandler(this));
        setInit(true);
    }

    @Override
    protected void postInit() {
    }

    private void setupDiscord() {
        this.discord = new Discord(this, "aiden");
        this.discord.setLargeImageText(getLANG().getStringWithKey("general.website", new String[]{"key"}, new String[]{getEngineData().getBindUrl()}));
    }

    private void setNews() {
        if (this.getConfig().isLoadNews()) {
            setNews(new News(this));
        } else {
            this.getGuiBuilder().getPanelsMap().get("newsForm").getComponent(0).setVisible(false);
        }
    }

    private void buildGui(String[] styles) {
        setStyleProvider(new StyleProvider(styles));
        setGuiBuilder(new GuiBuilder(this));
        GuiBuilder guiBuilder = getGuiBuilder();
        guiBuilder.getComponentFactory().setComponentFactoryListener(new ComponentManager(this));
        guiBuilder.setGuiBuilderListener(this);
        guiBuilder.buildGui(fileProperties.getFrameTpl(), getFrame().getRootPanel());
        this.iconUtils = new IconUtils(this);
    }

    private void logStartupTime(long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        getLOGGER().info(getAppTitle() + " started in " + duration + " ms!");
    }

    @Override
    public String appPath() {
        try {
            return URLDecoder.decode(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(), StandardCharsets.UTF_8);
        } catch (URISyntaxException e) {
            getLOGGER().error("Error decoding app path: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void onLogin(Map<String, Object> authCredentials) {
    }

    @Override
    public void onLoad(Auth auth, Map<String, Object> authCredentials) {
        auth.setAuthCredentials(authCredentials);
        try {
            if (!auth.authorizeAsync().get()) {
                config.clearConfigData(Arrays.asList("login", "password"), true);
            }
        } catch (InterruptedException | ExecutionException ignored) {
        }
    }

    @Override
    public void onPanelsBuilt() {
        if (!isInit() && getConfig().isBackgroundMusic()) {
            SOUND.playSound("music", "launcherTheme", true);
        }
    }

    @Override
    public void onAdditionalPanelBuild(JPanel jPanel) {
        this.user = new User(this);
    }

    @Override
    public void onPanelBuild(Map<String, OptionGroups> groups, String componentGroup, JPanel parentPanel) {
        parentPanel.updateUI();
        parentPanel.repaint();
        parentPanel.revalidate();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        actionHandler.handleAction(e);
    }

    public Engine getEngine() {
        return this;
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

    public Settings getSettings() {
        return settings;
    }

    public Config getConfig() {
        return (Config) this.config;
    }

    public IconUtils getIconUtils() {
        return iconUtils;
    }

    public NotificationPopup getNotification() {
        return notification;
    }

    public File getLauncherFile() {
        return launcherFile;
    }

    @SuppressWarnings("unused")
    public static class LauncherAttributes {
        private String hash;
        private String filename;

        public String getFileMd5() {
            return hash;
        }

        public String getFilename() {
            return filename;
        }
    }

    @Override
    public void showDialog(String messageKey, String errorTitle, int warningMessage, boolean terminate) {
        SwingUtilities.invokeLater(() -> {
            String errorMessage = this.getLANG().getString(messageKey);
            this.getSOUND().playSound("other", messageKey);
            UIManager.put("OptionPane.messageFont", this.getFONTUTILS().getFont("mcfont", 12.0F));
            JOptionPane.showMessageDialog(this.getFrame().getRootPane(), errorMessage, errorTitle, warningMessage);
            if (terminate) {
                System.exit(0);
            }
        });
    }
}
