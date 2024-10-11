package org.foxesworld;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.foxesworld.Notification.NotificationPopup;
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
import org.foxesworld.engine.utils.HashUtils;
import org.foxesworld.engine.utils.IconUtils;
import org.foxesworld.engine.utils.ServerInfo;
import org.foxesworld.engine.utils.helper.JVMHelper;
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

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Launcher extends Engine implements AuthListener {

    private Auth auth;
    private User user;
    private Settings settings;
    private final FileProperties fileProperties;
    private IconUtils iconUtils;
    private final File launcher;
    private final NotificationPopup notification;
    private static final List<String> CONFIG_FILES = List.of("config");

    public static void main(String[] args) {
        SplashScreenWindow splashScreen = new SplashScreenWindow();
        splashScreen.showSplashScreen();
        SwingUtilities.invokeLater(Launcher::new);
    }

    public Launcher() {
        super(CONFIG_FILES);
        long startTime = System.currentTimeMillis();
        this.launcher = new File(appPath());
        this.fileProperties = getFileProperties();
        this.notification = new NotificationPopup();
        preInit();

        if (!isLauncherValid()) {
            showInvalidLauncherDialog();
            return;
        }

        validateJRE();

        this.auth = new Auth(this);
        init();
        long duration = System.currentTimeMillis() - startTime;
        getLOGGER().info(getAppTitle() + " started in " + String.format("%d ms", duration) + "!");
    }

    private void showInvalidLauncherDialog() {
        showDialog("error.invalidLauncher", getAppTitle() + " Guard", JOptionPane.WARNING_MESSAGE, true);
    }

    private void validateJRE() {
        int launchingWith = Integer.parseInt(JVMHelper.getJavaVersion(System.getProperty("java.home") + "/bin").replaceAll("\\D", ""));
        int expectedJRE = Integer.parseInt(getEngineData().getProgramRuntime().replaceAll("\\D", ""));

        if (launchingWith != expectedJRE) {
            if (launcher.isFile()) {
                Engine.LOGGER.warn("Using incorrect JRE {}", launchingWith);
                showDialog("error.invalidJVM", getAppTitle() + " Guard", JOptionPane.WARNING_MESSAGE, true);
            } else if (launcher.isDirectory()) {
                logJREWarning(expectedJRE);
            } else {
                Engine.LOGGER.warn("Launcher path is neither a file nor a directory. 0_0");
            }
        }
    }

    private void logJREWarning(int expectedJRE) {
        Engine.LOGGER.warn("Using a JRE different from {}", getEngineData().getProgramRuntime());
        if (isRunningInIDE()) {
            Engine.LOGGER.warn("Launching in IDE using {}", JVMHelper.getJavaVersion(System.getProperty("java.home") + "/bin"));
        }
    }

    private boolean isRunningInIDE() {
        return System.getProperty("java.class.path").contains("build");
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

    private void buildGui(String[] styles) {
        setStyleProvider(new StyleProvider(styles));
        setGuiBuilder(new GuiBuilder(this));
        getGuiBuilder().getComponentFactory().setComponentFactoryListener(new ComponentManager(this));
        getGuiBuilder().setGuiBuilderListener(this);
        getGuiBuilder().buildGui(fileProperties.getFrameTpl(), getFrame().getRootPanel());
        this.iconUtils = new IconUtils(this);
    }

    @Override
    public void init() {
        this.discord = new Discord(this, "aiden");
        this.discord.setLargeImageText(getLANG().getStringWithKey("general.website", new String[]{"key"}, new String[]{getEngineData().getBindUrl()}));
        buildGui(getEngineData().getStyles());
        loadMainPanel(fileProperties.getMainFrame());
        if (this.getConfig().isLoadNews()) {
            setNews(new News(this));
        } else {
            this.getGuiBuilder().getPanelsMap().get("newsForm").getComponent(0).setVisible(false);
        }

        getGuiBuilder().buildAdditionalPanels();
        this.loadingManager = new LoadStatus(this, getConfig().getLoaderIndex());
        this.settings = new Settings(this);
        this.settings.addListeners();
        setActionHandler(new ActionHandler(this));
        setInit(true);
    }

    private boolean isLauncherValid() {
        getLOGGER().info("Starting launcher validation");

        Map<String, Object> launcherRequest = Map.of("sysRequest", "downloadLatest");
        String selfMd5 = HashUtils.md5(appPath());
        getLOGGER().info("Calculated self MD5: {}", selfMd5);

        if ("IDE".equals(selfMd5)) {
            return true;
        }

        try {
            String response = getPOSTrequest().send(launcherRequest);
            LauncherAttributes launcherAttributes = new Gson().fromJson(response, LauncherAttributes.class);
            getLOGGER().info("Server response MD5: {}", launcherAttributes.getFileMd5());

            boolean isValid = Objects.equals(selfMd5, launcherAttributes.getFileMd5());
            if (!isValid) {
                getLOGGER().warn("Launcher validation failed: MD5 mismatch");
            }

            return isValid;
        } catch (JsonSyntaxException e) {
            getLOGGER().error("JSON parsing error during launcher validation: {}", e.getMessage(), e);
        } catch (Exception e) {
            getLOGGER().error("Unexpected error during launcher validation: {}", e.getMessage(), e);
        }
        return false;
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
        // Implementation
    }

    @Override
    public void onLoad(Auth auth, Map<String, Object> authCredentials) {
        auth.setAuthCredentials(authCredentials);
        if (!auth.authorize()) {
            config.clearConfigData(Arrays.asList("login", "password"), true);
        }
    }

    @Override
    public void onPanelsBuilt() {
        setUser(new User(this));
        if (!isInit() && getConfig().isBackgroundMusic()) {
            SOUND.playSound("music", "launcherTheme", true);
        }
    }


    @Override
    public void onAdditionalPanelBuild(JPanel jPanel) {
        // Implementation
    }

    @Override
    public void onPanelBuild(Map<String, OptionGroups> groups, String componentGroup, JPanel parentPanel) {
        parentPanel.updateUI();
        parentPanel.repaint();
        parentPanel.revalidate();
        getLOGGER().debug("Built panel {} with parent {}", componentGroup, parentPanel.getName());
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

    @SuppressWarnings("unused")
    static class LauncherAttributes {
        private String hash;
        private String filename;

        public String getFileMd5() {
            return hash;
        }

        public String getFilename() {
            return filename;
        }
    }
}