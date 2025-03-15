package org.foxesworld;

import org.apache.logging.log4j.Level;
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
import org.foxesworld.engine.utils.DragListener;
import org.foxesworld.engine.utils.IconUtils;
import org.foxesworld.engine.utils.ServerInfo;
import org.foxesworld.engine.utils.hook.HookException;
import org.foxesworld.launcher.LauncherValidator;
import org.foxesworld.launcher.auth.Auth;
import org.foxesworld.launcher.config.Config;
import org.foxesworld.launcher.gui.ActionHandler;
import org.foxesworld.launcher.gui.InitialValue;
import org.foxesworld.launcher.gui.Settings;
import org.foxesworld.launcher.gui.SplashScreenWindow;
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
import java.util.HashMap;
import java.util.Map;

public class Launcher extends Engine {
    private Auth auth;
    long startTime;
    private User user;
    private Settings settings;
    private final FileProperties fileProperties;
    private IconUtils iconUtils;
    private final File launcherFile;
    private static final Map<String, Class<?>> CONFIG_FILES = new HashMap<>();
    private ActionHandler actionHandler;

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("file.encoding", "UTF-8");
        SwingUtilities.invokeLater(Launcher::showSplashAndStartLauncher);
    }

    static {
        CONFIG_FILES.put("config", Config.class);
    }

    private static void showSplashAndStartLauncher() {
        SplashScreenWindow splashScreen = new SplashScreenWindow();
        splashScreen.showSplashScreen();
        splashScreen.getLottieSwingEngine().getAnimationPanel().setOnAnimationCompleted(() -> {
            new Launcher();
            splashScreen.dispose();
        });
    }

    public Launcher() {
        super(Runtime.getRuntime().availableProcessors(), "forge", CONFIG_FILES);
        startTime = System.currentTimeMillis();
        this.launcherFile = new File(appPath());
        this.fileProperties = getFileProperties();
        preInit();
        safeSubmitTask(() -> new LauncherValidator(this).validate(), "validation");

        init();
    }

    private void safeSubmitTask(Runnable task, String taskName) {
        this.getExecutorServiceProvider().submitTask(() -> {
            try {
                task.run();
            } catch (Exception e) {
                getLOGGER().error("Ошибка в задаче " + taskName, e);
            }
        }, taskName);
    }

    @Override
    protected void preInit() {
        this.config = new Config(this);
        System.setProperty("AppDir", System.getenv("APPDATA"));
        System.setProperty("RamAmount", String.valueOf(Runtime.getRuntime().maxMemory() / 45));
        try {
            if (getPreInitHooks().hook(null, null)) {
                LOGGER.info("Pre-init hooks прервали инициализацию");
                return;
            }
        } catch (HookException e) {
            LOGGER.error("Ошибка в pre-init hooks", e);
        }
        this.config.processConfig();
        this.LANG = new LanguageProvider(this, fileProperties.getLocaleFile(), getConfig().getLang());
        this.SOUND = new Sound(this, getClass().getClassLoader().getResourceAsStream(fileProperties.getSoundsFile()));
        this.frameConstructor = new FrameConstructor(this);
        this.serverInfo = new ServerInfo(this);
        this.CRYPTO = new CryptUtils();
        this.setLogLevel(Level.valueOf(((org.foxesworld.launcher.config.Config) config).getLogLevel()));
        this.frameConstructor.setFocusStatusListener(this);
        this.auth = new Auth(this);
    }

    @Override
    public void init() {
        getSOUND().getSoundPlayer().stopAllSounds();
        setupDiscord();

        safeSubmitTask(() -> {

            buildGui(getEngineData().getStyles());
            loadMainPanel(fileProperties.getMainFrame());
        }, "init");
    }

    @Override
    protected void postInit() {
        safeSubmitTask(() -> {
            getGuiBuilder().buildAdditionalPanels();
        }, "postInit");
    }

    private void setupDiscord() {
        safeSubmitTask(() -> {
            this.discord = new Discord(this, "aiden");
            this.discord.setLargeImageText(getLANG().getStringWithKey("general.website",
                    new String[]{"key"},
                    new String[]{getEngineData().getBindUrl()}));
        }, "discordSetUp");
    }

    private void buildGui(String[] styles) {
        setStyleProvider(new StyleProvider(styles));
        setGuiBuilder(new GuiBuilder(this));
        getGuiBuilder().getComponentFactory().setComponentFactoryListener(new InitialValue(this));
        getGuiBuilder().setGuiBuilderListener(this);
        getGuiBuilder().buildGuiAsync(fileProperties.getFrameTpl(), getFrame().getRootPanel());
        this.iconUtils = new IconUtils(this);
    }

    public void logStartupTime(long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        getLOGGER().info(getAppTitle() + " запущен за " + duration + " мс!");
    }

    @Override
    public String appPath() {
        try {
            return URLDecoder.decode(
                    Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(),
                    StandardCharsets.UTF_8
            );
        } catch (URISyntaxException e) {
            getLOGGER().error("Ошибка декодирования пути приложения: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void onPanelsBuilt() {
        if(!isInit()) {
            gammaInit();
            setInit(true);
            logStartupTime(getStartTime());
        }
    }

    public void gammaInit(){
        SwingUtilities.invokeLater(() -> {
            this.loadingManager = new LoadStatus(this, getConfig().getLoaderIndex());
            this.settings = new Settings(this);
            this.actionHandler = new ActionHandler(this);
            setActionHandler(this.actionHandler);
            setUser(new User(this));
        });
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

    public IconUtils getIconUtils() {
        return iconUtils;
    }

    public File getLauncherFile() {
        return launcherFile;
    }

    @Override
    public void updateFocus(boolean hasFocus) {
        // Если вызов не происходит из EDT, перенаправляем выполнение в EDT
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

                // Переносим компоненты из старой панели в новую
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
