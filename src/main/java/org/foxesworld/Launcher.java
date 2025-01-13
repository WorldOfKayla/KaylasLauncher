package org.foxesworld;

import org.apache.logging.log4j.Level;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.discord.Discord;
import org.foxesworld.engine.gui.FileProperties;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.components.frame.FrameConstructor;
import org.foxesworld.engine.gui.components.frame.OptionGroups;
import org.foxesworld.engine.gui.components.panel.Panel;
import org.foxesworld.engine.gui.styles.StyleProvider;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.sound.Sound;
import org.foxesworld.engine.utils.Crypt.CryptUtils;
import org.foxesworld.engine.utils.DragListener;
import org.foxesworld.engine.utils.IconUtils;
import org.foxesworld.engine.utils.ServerInfo;
import org.foxesworld.launcher.LauncherValidator;
import org.foxesworld.launcher.auth.Auth;
import org.foxesworld.launcher.auth.AuthListener;
import org.foxesworld.launcher.config.Config;
import org.foxesworld.launcher.gui.ActionHandler;
import org.foxesworld.launcher.gui.InitialValue;
import org.foxesworld.launcher.gui.Settings;
import org.foxesworld.launcher.gui.loadingManager.LoadStatus;
import org.foxesworld.launcher.user.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Launcher extends Engine implements AuthListener {
    private final Auth auth;
    private User user;
    private Settings settings;
    private final FileProperties fileProperties;
    private IconUtils iconUtils;
    private final File launcherFile;
    private static final Map<String, Class<?>> CONFIG_FILES = new HashMap<>();
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Launcher::new);
    }

    static {
        CONFIG_FILES.put("config", Config.class);
    }

    /*
    private static void showSplashAndStartLauncher() {
        SplashScreenWindow splashScreen = new SplashScreenWindow();
        splashScreen.showSplashScreen();

        Timer launchTimer = new Timer(890, e -> new Launcher(splashScreen));
        launchTimer.setRepeats(false);
        launchTimer.start();
    } */

    public Launcher() {
        super(Runtime.getRuntime().availableProcessors(), "forge", CONFIG_FILES);
        long startTime = System.currentTimeMillis();
        this.launcherFile = new File(appPath());
        this.fileProperties = getFileProperties();

        preInit();
        this.auth = new Auth(this);
        new LauncherValidator(this).validate();
        init();
        logStartupTime(startTime);
    }

    @Override
    protected void preInit() {
        this.config = new Config(this);
        System.setProperty("AppDir", System.getenv("APPDATA"));
        System.setProperty("RamAmount", String.valueOf(Runtime.getRuntime().maxMemory() / 45));
        this.config.processConfig();
        this.LANG = new LanguageProvider(this, fileProperties.getLocaleFile(), getConfig().getLang());
        this.SOUND = new Sound(this, getClass().getClassLoader().getResourceAsStream(fileProperties.getSoundsFile()));
        this.frameConstructor = new FrameConstructor(this);
        this.serverInfo = new ServerInfo(this);
        this.CRYPTO = new CryptUtils(this);
        this.setLogLevel(Level.valueOf(((org.foxesworld.launcher.config.Config)config).getLogLevel()));
    }

    @Override
    public void init() {
        SwingUtilities.invokeLater(() -> setActionHandler(new ActionHandler(this)));
            setupDiscord();
            buildGui(getEngineData().getStyles());
            loadMainPanel(fileProperties.getMainFrame());
            this.getExecutorServiceProvider().submitTask(() -> {
                    this.user = new User(this);
                    setNews();
                    this.loadingManager = new LoadStatus(this, getConfig().getLoaderIndex());
                    this.settings = new Settings(this);
                    this.settings.addListeners();
                }, "init");
        setInit(true);
    }

    @Override
    protected void postInit() {
        this.getExecutorServiceProvider().submitTask(() -> {
            this.getFrame().setFocusStatusListener(this);
            getGuiBuilder().buildAdditionalPanels();
            if(this.getConfig().isBackgroundMusic()) {
                SOUND.playSound("music", "launcherTheme", true);
            }
        }, "postInit");
    }

    private void setupDiscord() {
        this.getExecutorServiceProvider().submitTask(() -> {
            this.discord = new Discord(this, "aiden");
            this.discord.setLargeImageText(getLANG().getStringWithKey("general.website", new String[]{"key"}, new String[]{getEngineData().getBindUrl()}));
        }, "discordSetUp");
    }

    private void setNews() {
        //if (this.getConfig().isLoadNews()) {
            //setNews(new News(this));
        //} else {
            this.user.getServerInfoDisplayer().displayServerInfo(this.getConfig().getSelectedServer());
        //}
    }

    private void buildGui(String[] styles) {
        setStyleProvider(new StyleProvider(styles));
        setGuiBuilder(new GuiBuilder(this));
        GuiBuilder guiBuilder = getGuiBuilder();
        guiBuilder.getComponentFactory().setComponentFactoryListener(new InitialValue(this));
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
        getSOUND().playSound("other", "loggedIn");
    }

    @Override
    public void onLoad(Auth auth, Map<String, Object> authCredentials) {
        //this.getExecutorServiceProvider().submitTask(() -> {
            auth.setAuthCredentials(authCredentials);
            try {
                if (!auth.authorizeAsync().get()) {
                    config.clearConfigData(Arrays.asList("login", "password"), true);
                }
            } catch (InterruptedException | ExecutionException ignored) {}
        //}, "auth");
    }

    @Override
    public void onPanelsBuilt() {}

    @Override
    public void onAdditionalPanelBuild(JPanel jPanel) {}

    @Override
    public void onGuiBuilt() {}

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

    public File getLauncherFile() {
        return launcherFile;
    }

    @Override
    @Deprecated
    public void updateFocus(boolean hasFocus) {
        JPanel oldTitleBar = this.getGuiBuilder().getPanelsMap().get("titleBar");

        if (oldTitleBar != null) {
            String texturePath = "assets/ui/img/bg/header.png";
            BufferedImage texture = this.getImageUtils().getLocalImage(texturePath);

            int sectionHeight = oldTitleBar.getHeight();
            int yOffset = hasFocus ? sectionHeight : 0;

            DragListener dragListener = new DragListener();
            JPanel newTitleBar = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);

                    if (texture != null) {
                        g.drawImage(texture, 0, 0, getWidth(), getHeight(),
                                0, yOffset, texture.getWidth(), yOffset + sectionHeight, null);
                    }

                    Graphics2D g2d = (Graphics2D) g;
                    Color shadowColor = new Color(0, 0, 0, 77);
                    g2d.setColor(shadowColor);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            };

            newTitleBar.setBounds(oldTitleBar.getBounds());
            newTitleBar.setLayout(oldTitleBar.getLayout());
            newTitleBar.setOpaque(false);
            newTitleBar.setName(oldTitleBar.getName());

            for (Component component : oldTitleBar.getComponents()) {
                oldTitleBar.remove(component);
                newTitleBar.add(component);
            }

            dragListener.addDragListener(newTitleBar, this.getFrame());
            this.getGuiBuilder().getPanelsMap().put("titleBar", newTitleBar);

            Container parent = oldTitleBar.getParent();
            parent.remove(oldTitleBar);
            parent.add(newTitleBar);
            parent.revalidate();
            parent.repaint();
        }
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
        this.getExecutorServiceProvider().submitTask(() -> {
        SwingUtilities.invokeLater(() -> {
            String errorMessage = this.getLANG().getString(messageKey);
            this.getSOUND().playSound("other", messageKey);
            UIManager.put("OptionPane.messageFont", this.getFONTUTILS().getFont("mcfont", 12.0F));
            JOptionPane.showMessageDialog(this.getFrame().getRootPane(), errorMessage, errorTitle, warningMessage);
            if (terminate) {
                System.exit(0);
            }
        });
        }, "modalDialog");
    }
}
