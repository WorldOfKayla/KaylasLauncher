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
import org.foxesworld.engine.utils.loadManager.LoadingManager;
import org.foxesworld.launcher.auth.Auth;
import org.foxesworld.launcher.auth.AuthListener;
import org.foxesworld.launcher.config.Config;
import org.foxesworld.launcher.gui.ActionHandler;
import org.foxesworld.launcher.gui.Settings;
import org.foxesworld.launcher.gui.components.ComponentManager;
import org.foxesworld.launcher.news.News;
import org.foxesworld.launcher.user.User;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

public class Launcher extends Engine implements AuthListener {

    private Auth auth;
    private User user;
    private Settings settings;
    private final FileProperties fileProperties;
    private IconUtils iconUtils;
    private final File launcher;
    private final NotificationPopup notification;
    private static final List<String> configFiles = List.of("config");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Launcher::new);
    }

    public Launcher() {
        super(configFiles);
        long startTime = System.currentTimeMillis();
        this.launcher = new File(this.appPath());
        this.fileProperties = getFileProperties();
        this.notification = new NotificationPopup();
        this.preInit();

        if (!isLauncherValid()) {
            showDialog("error.invalidLauncher", this.getAppTitle() + "Guard", JOptionPane.WARNING_MESSAGE, true);
        } else {
            int launchingWith = Integer.parseInt(JVMHelper.getJavaVersion(System.getProperty("java.home") + "/bin").replaceAll("\\D", ""));
            int expectedJRE = Integer.parseInt(getEngineData().getProgramRuntime().replaceAll("\\D", ""));
            if (launchingWith != expectedJRE) {
                if (launcher.isFile()) {
                    Engine.LOGGER.warn("Using incorrect JRE {}", launchingWith);
                    showDialog("error.invalidJVM", this.getAppTitle() + "Guard", JOptionPane.WARNING_MESSAGE, true);
                    return;
                } else if (launcher.isDirectory()) {
                    Engine.LOGGER.warn("Using a JRE different from {}", getEngineData().getProgramRuntime());
                    if (isRunningInIDE()) {
                        Engine.LOGGER.warn("If you'll launch it not in IDE using {} will get an exception!", JVMHelper.getJavaVersion(System.getProperty("java.home") + "/bin"));
                    }
                } else {
                    Engine.LOGGER.warn("Launcher path is neither a file nor a directory. 0_0");
                }
            }
            this.auth = new Auth(this);
            init();
            //notification.display("FoxesWorld", "Добро пожаловать и чувствуйте себя как дома!", imageUtils.getLocalImage("assets/ui/icons/logo.png"));
            setActionHandler(new ActionHandler(this));
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            getLOGGER().info(this.getAppTitle() + " started in " + String.format("%d ms", duration) + "!");
        }
    }

    private boolean isRunningInIDE() {
        String classPath = System.getProperty("java.class.path");
        return classPath.contains("build");
    }

    @Override
    protected void preInit() {
        this.config = new Config(this);
        this.LANG = new LanguageProvider(this, this.fileProperties.getLocaleFile(), String.valueOf(this.getConfig().getCONFIG().get("lang")));
        this.SOUND = new Sound(this, this.getClass().getClassLoader().getResourceAsStream(this.fileProperties.getSoundsFile()));
        this.frameConstructor = new FrameConstructor(this);
        this.loadingManager = new LoadingManager(this, this.getConfig().getLoaderIndex());
        this.serverInfo = new ServerInfo(this);
        this.CRYPTO = new CryptUtils(this);
    }

    private void buildGui(String[] styles) {
        setStyleProvider(new StyleProvider(styles));
        setGuiBuilder(new GuiBuilder(this));
        this.getGuiBuilder().getComponentFactory().setComponentFactoryListener(new ComponentManager(this));
        getGuiBuilder().setGuiBuilderListener(this);
        this.getGuiBuilder().buildGui(this.getFileProperties().getFrameTpl(), this.getFrame().getRootPanel());
        this.iconUtils = new IconUtils(this);
    }

    @Override
    public void init() {
        this.discord = new Discord(this, "aiden");
        this.buildGui(this.getEngineData().getStyles());
        setNews(new News(this));
        loadMainPanel(this.fileProperties.getMainFrame());

        // ALL PANELS ARE BUILT
        this.getGuiBuilder().buildAdditionalPanels();
        this.settings = new Settings(this);
        this.settings.addListeners();
        setInit(true);
    }

    private boolean isLauncherValid() {
        getLOGGER().info("Starting launcher validation");

        Map<String, String> launcherRequest = new HashMap<>();
        launcherRequest.put("sysRequest", "downloadLatest");

        String selfMd5 = HashUtils.md5(this.appPath());
        getLOGGER().info("Calculated self MD5: " + selfMd5);

        if ("IDE".equals(selfMd5)) {
            return true;
        }

        try {
            String response = this.getPOSTrequest().send(launcherRequest);
            LauncherAttributes launcherAttributes = new Gson().fromJson(response, LauncherAttributes.class);
            getLOGGER().info("Server response MD5: " + launcherAttributes.getFileMd5());

            boolean isValid = Objects.equals(selfMd5, launcherAttributes.getFileMd5());
            if (!isValid) {
                getLOGGER().warn("Launcher validation failed: MD5 mismatch");
            }

            return isValid;
        } catch (JsonSyntaxException e) {
            getLOGGER().error("JSON parsing error during launcher validation: " + e.getMessage(), e);
        } catch (Exception e) {
            getLOGGER().error("Unexpected error during launcher validation: " + e.getMessage(), e);
        }
        return false;
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

    @Override
    public void onLoad(Auth auth, Map<String, String> authCredentials) {
        auth.setAuthCredentials(authCredentials);
        if (!auth.authorize()) {
            this.config.clearConfigData(Arrays.asList("login", "password"), true);
        }
    }

    @Override
    public void onPanelsBuilt() {
        this.setUser(new User(this));
        if (!isInit() && this.getConfig().isBackgroundMusic()) {
            this.getSOUND().playSound("music", "launcherTheme", true);
        }
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
        this.actionHandler.handleAction(e);
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

    @SuppressWarnings("unused")
    static class LauncherAttributes {
        private String fileMd5;
        private String filename;

        public String getFileMd5() {
            return fileMd5;
        }

        public String getFilename() {
            return filename;
        }
    }

    static class SplashScreenWindow {
        private final JWindow window;
        private final JLabel label;

        public SplashScreenWindow() {
            window = new JWindow();
            label = new JLabel(new ImageIcon(getClass().getClassLoader().getResource("assets/ui/img/serum.png")));
            window.getContentPane().add(label, BorderLayout.CENTER);
            window.setSize(500, 300); // Размер окна сплэш-экрана
            window.setLocationRelativeTo(null);
        }

        public void showSplashScreen() {
            window.setVisible(true);
            Timer timer = new Timer(3000, e -> {
                window.setVisible(false);
                window.dispose();
            });
            timer.setRepeats(false);
            timer.start();
        }

        public void dispose() {
            window.dispose();
        }
    }
}