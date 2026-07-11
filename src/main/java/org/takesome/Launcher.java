package org.takesome;

import org.apache.logging.log4j.Level;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.crash.CrashReportDialog;
import org.takesome.kaylasEngine.crash.CrashReportSubscription;
import org.takesome.kaylasEngine.discord.Discord;
import org.takesome.kaylasEngine.gui.GuiBuilder;
import org.takesome.kaylasEngine.gui.components.frame.OptionGroups;
import org.takesome.kaylasEngine.gui.styles.StyleProvider;
import org.takesome.kaylasEngine.locale.LanguageProvider;
import org.takesome.kaylasEngine.utils.IconUtils;
import org.takesome.kaylasEngine.utils.ServerInfo;
import org.takesome.kaylasEngine.utils.hook.HookException;
import org.takesome.launcher.LauncherValidator;
import org.takesome.launcher.auth.Auth;
import org.takesome.kaylasEngine.EngineData;
import org.takesome.launcher.backend.LauncherBackendClient;
import org.takesome.launcher.config.Config;
import org.takesome.launcher.discord.LauncherDiscordPresence;
import org.takesome.launcher.gui.ActionHandler;
import org.takesome.launcher.gui.InitialValue;
import org.takesome.launcher.gui.Settings;
import org.takesome.launcher.gui.SplashScreenWindow;
import org.takesome.launcher.gui.loadingManager.LoadStatus;
import org.takesome.launcher.gui.components.LauncherComponentLibrary;
import org.takesome.launcher.security.SecureProcessIncidentHandler;
import org.takesome.launcher.security.SecureProcessLog;
import org.takesome.launcher.security.SecureProcessModuleMonitor;
import org.takesome.launcher.security.SecureProcess;
import org.takesome.launcher.security.SecureProcessResult;
import org.takesome.launcher.user.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Launcher extends Engine {
    private Auth auth;
    long startTime;
    private User user;
    private Settings settings;
    private final File launcherFile;
    private static final Map<String, Class<?>> CONFIG_FILES = new HashMap<>();
    private ActionHandler actionHandler;
    private LauncherBackendClient backendClient;
    private CrashReportSubscription crashReportSubscription;
    private final LauncherDiscordPresence discordPresence;
    private SecureProcessModuleMonitor secureProcessModuleMonitor;

    public static void main(String[] args) {
        System.setProperty("log.dir", System.getProperty("user.dir", "."));
        SecureProcessLog.logger().info("Launcher security bootstrap started: java={}, vendor={}, os={}, arch={}",
                System.getProperty("java.version", "unknown"),
                System.getProperty("java.vendor", "unknown"),
                System.getProperty("os.name", "unknown"),
                System.getProperty("os.arch", "unknown"));

        SecureProcessResult secureProcess = SecureProcess.initializeEarly();
        if (secureProcess.fullyApplied()) {
            SecureProcessLog.logger().info(
                    "SecureProcess active: version={}, applied=0x{}, failed=0x{}, sha256={}, path={}",
                    secureProcess.nativeVersion(),
                    Integer.toHexString(secureProcess.appliedFlags()),
                    Integer.toHexString(secureProcess.failedFlags()),
                    secureProcess.sha256(),
                    secureProcess.libraryPath());
        } else {
            SecureProcessLog.logger().error("SecureProcess degraded: {}", secureProcess.message());
        }

        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("file.encoding", "UTF-8");
        SwingUtilities.invokeLater(Launcher::showSplashAndStartLauncher);
    }

    static {
        CONFIG_FILES.put("config", Config.class);
    }

    private static void showSplashAndStartLauncher() {
        SplashScreenWindow splashScreen = new SplashScreenWindow();
        //splashScreen.showSplashScreen();
        //splashScreen.getLottieSwingEngine().getAnimationPanel().setOnAnimationCompleted(() -> {
            new Launcher(null);
            //splashScreen.dispose();
        //});
    }

    public Launcher(Rectangle bounds) {
        super(Runtime.getRuntime().availableProcessors(), "forge", CONFIG_FILES);
        if (SecureProcess.isNativeLibraryLoaded()) {
            long intervalSeconds = Long.getLong("kaylas.secureProcess.auditIntervalSeconds", 30L);
            this.secureProcessModuleMonitor = SecureProcessModuleMonitor.start(
                    Duration.ofSeconds(Math.max(1L, intervalSeconds)),
                    SecureProcessIncidentHandler::handle
            );
        }
        this.discordPresence = new LauncherDiscordPresence(this, "aiden");
        startTime = System.currentTimeMillis();
        this.launcherFile = new File(appPath());
        //preInit();
        safeSubmitTask(() -> new LauncherValidator(this).validate(), "validation");

        //init();
        if(bounds != null) {
            this.getFrame().setBounds(bounds);
        }
    }

    @Override
    protected void preInit() {
        this.config = new Config(this);
        System.setProperty("AppDir", System.getenv("APPDATA"));
        System.setProperty("RamAmount", String.valueOf(Runtime.getRuntime().maxMemory() / 45));
        try {
            if (getPreInitHooks().hook(null, null)) {
                LOGGER.info("Pre-init hooks requested launcher initialization to stop.");
                return;
            }
        } catch (HookException e) {
            LOGGER.error("Pre-init hook execution failed", e);
        }
        this.config.processConfig();
        this.LANG = new LanguageProvider(this, fileProperties.getLocaleFile(), getConfig().getLang());
        this.serverInfo = new ServerInfo(this);
        this.setLogLevel(Level.valueOf(((org.takesome.launcher.config.Config) config).getLogLevel()));
        this.frameConstructor.setFocusStatusListener(this);
        bindBackend();
        this.auth = new Auth(this);
    }

    private void bindBackend() {
        EngineData.BackendBinding backend = getEngineData() == null ? null : getEngineData().getBackend();
        if (backend == null || !backend.isEnabled()) {
            LOGGER.info("Launcher backend binding is disabled by engine runtime config.");
            return;
        }
        try {
            this.backendClient = new LauncherBackendClient(
                    this,
                    backend.getWsUrl(),
                    backend.getHeartbeatSeconds(),
                    backend.getMaxReconnectAttempts(),
                    backend.isRequireSecureTransport(),
                    backend.isAllowInsecureLoopback(),
                    backend.getTlsPublicKeySha256()
            );
            this.backendClient.start();
            this.crashReportSubscription = CrashReportDialog.subscribe(this, this.backendClient::submitCrashReport);
        } catch (RuntimeException error) {
            LOGGER.warn("Unable to initialize launcher backend binding: {}", error.getMessage());
        }
    }

    @Override
    public void init() {
        getSOUND().getSoundPlayer().stopAllSounds();
        setupDiscord();

        safeSubmitTask(() -> {
            buildGui(new InitialValue(this));
            LauncherComponentLibrary.register(getGuiBuilder());
            loadMainPanel(fileProperties.getMainFrame());
        }, "init");
    }

    @Override
    protected void postInit() {
        safeSubmitTask(() -> getGuiBuilder().buildAdditionalPanels(), "postInit");
    }

    private void setupDiscord() {
        safeSubmitTask(() -> {
            this.discord = new Discord(this, "aiden");
            this.discord.setLargeImageText(getEngineData().getLauncherBrand() + " " + getEngineData().getLauncherVersion());
            this.discordPresence.refresh();
        }, "discordSetUp");
    }

    public void logStartupTime(long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        getLOGGER().info(getAppTitle() + " started in " + duration + " ms.");
    }

    @Override
    public String appPath() {
        try {
            return URLDecoder.decode(
                    Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(),
                    StandardCharsets.UTF_8
            );
        } catch (URISyntaxException e) {
            getLOGGER().error("Unable to resolve application path: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void onPanelsBuilt() {
        if(!isInit()) {
            LambdaInit();
            setInit(true);
            logStartupTime(getStartTime());
        }
    }

    public void LambdaInit(){
        safeSubmitTask(() -> SwingUtilities.invokeLater(() -> {
            this.loadingManager = new LoadStatus(this, getConfig().getLoaderIndex());
            this.settings = new Settings(this);
            this.actionHandler = new ActionHandler(this);
            setActionHandler(this.actionHandler);
            setUser(new User(this));
        }), "lambdaInit");
    }
    @Override
    public void onAdditionalPanelBuild(JPanel jPanel) {
    }

    @Override
    public void onGuiBuilt() {
    }

    @Override
    public void onPanelBuild(Map<String, OptionGroups> groups, String componentGroup, Container parentPanel) {
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

    public LauncherBackendClient getBackendClient() {
        return backendClient;
    }

    public LauncherDiscordPresence getDiscordPresence() {
        return discordPresence;
    }

    @Override
    public void shutdownExecutorService() {
        CrashReportSubscription currentCrashReportSubscription = crashReportSubscription;
        if (currentCrashReportSubscription != null) {
            currentCrashReportSubscription.close();
            crashReportSubscription = null;
        }
        Discord currentDiscord = getDiscord();
        if (currentDiscord != null) {
            currentDiscord.shutdown();
        }
        LauncherBackendClient currentBackendClient = backendClient;
        if (currentBackendClient != null) {
            currentBackendClient.close();
        }
        SecureProcessModuleMonitor currentMonitor = secureProcessModuleMonitor;
        if (currentMonitor != null) {
            currentMonitor.close();
        }
        super.shutdownExecutorService();
    }

    public File getLauncherFile() {
        return launcherFile;
    }

    @Override
    public void updateFocus(boolean hasFocus) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> updateFocus(hasFocus));
            return;
        }

        if (this.getGuiBuilder() != null) {
            JPanel oldTitleBar = this.getGuiBuilder().getPanelsMap().get("titleBar");

            if (oldTitleBar != null) {
                String texturePath = "assets/ui/img/bg/header.png";
                BufferedImage texture = this.getImageUtils().getLocalImage(texturePath);

                int sectionHeight = oldTitleBar.getHeight();
                int yOffset = hasFocus ? sectionHeight : 0;

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

                getPanelListenerRegistry().install("windowDrag", newTitleBar, this.getFrame());
                this.getGuiBuilder().getPanelsMap().put("titleBar", newTitleBar);

                Container parent = oldTitleBar.getParent();
                parent.remove(oldTitleBar);
                parent.add(newTitleBar);
                parent.revalidate();
                parent.repaint();
            }
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
        safeSubmitTask(() -> SwingUtilities.invokeLater(() -> {
            String errorMessage = this.getLANG().getString(messageKey);
            this.getSOUND().playSound("other", messageKey);
            UIManager.put("OptionPane.messageFont", this.getFONTUTILS().getFont("mcfont", 12.0F));
            JOptionPane.showMessageDialog(this.getFrame().getRootPane(), errorMessage, errorTitle, warningMessage);
            if (terminate) {
                System.exit(0);
            }
        }), "modalDialog");
    }

    public long getStartTime() {
        return startTime;
    }
}
