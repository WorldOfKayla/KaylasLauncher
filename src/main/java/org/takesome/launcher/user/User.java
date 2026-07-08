package org.takesome.launcher.user;

import org.foxesworld.notification.Notification;
import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.gui.GuiBuilder;
import org.takesome.kaylasEngine.gui.componentAccessor.Component;
import org.takesome.kaylasEngine.gui.components.dropBox.DropBox;
import org.takesome.kaylasEngine.gui.components.label.Label;
import org.takesome.kaylasEngine.locale.LanguageProvider;
import org.takesome.kaylasEngine.utils.HTTP.RequestState;
import org.takesome.kaylasEngine.utils.ServerInfo;
import org.takesome.launcher.auth.Auth;
import org.takesome.launcher.auth.AuthStatus;
import org.takesome.launcher.gui.BlendedImageIcon;
import org.takesome.launcher.gui.LauncherUserUiConfig;
import org.takesome.launcher.server.ServerInfoDisplayer;
import org.takesome.launcher.user.loader.BadgeLoader;
import org.takesome.launcher.user.loader.BadgeObject;
import org.takesome.launcher.user.loader.GroupLoader;
import org.takesome.launcher.user.loader.GroupObject;
import org.takesome.launcher.user.loader.HeadLoader;
import org.takesome.launcher.user.loader.SkinLoader;
import org.takesome.launcher.utils.SvgUtils;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.takesome.kaylasEngine.utils.FontUtils.hexToColor;

public class User extends org.takesome.kaylasEngine.user.User {
    private static final String SKIN_HOVER_LISTENER_PROPERTY = "launcher.user.skinHover.listener";
    private static final String SKIN_HOVER_TIMER_PROPERTY = "launcher.user.skinHover.timer";

    private final Auth auth;
    private final LoggedForm loggedForm;
    private final Launcher launcher;
    private final LanguageProvider lang;
    private final ServerInfo serverInfo;
    private final GuiBuilder guiBuilder;
    private final UserAttributes userAttributes;
    private final JPanel newsPanel;
    private final LauncherUserUiConfig userUi;

    private ServerInfoDisplayer serverInfoDisplayer;
    private SkinLoader skinLoader;
    private HeadLoader headLoader;
    private GroupLoader groupLoader;
    private JFrame taskMgrFrame;
    private Timer authorizationWaitTimer;

    @Component
    @SuppressWarnings("unused")
    private Label userGroup, userLogin, crystalsField, unitsField, userHead;

    public User(Launcher launcher) {
        super(launcher.getGuiBuilder(), LauncherUserUiConfig.rootPanelId(), List.of(Label.class));
        this.launcher = launcher;
        this.auth = launcher.getAuth();
        this.userUi = LauncherUserUiConfig.load(launcher);
        this.userAttributes = new UserAttributes(this);
        this.loggedForm = new LoggedForm(launcher.getGuiBuilder(), userUi.panels().loggedForm(), List.of(DropBox.class, Label.class));
        this.engine = launcher.getEngine();
        this.serverInfo = engine.getServerInfo();
        this.lang = launcher.getLANG();
        this.guiBuilder = launcher.getGuiBuilder();
        this.newsPanel = guiBuilder.getPanelsMap().get(userUi.panels().newsForm());
        SwingUtilities.invokeLater(this::initializeUser);
    }

    public void initializeUser() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::initializeUser);
            return;
        }

        AuthStatus status = auth.getAuthStatus();
        if (status == AuthStatus.AUTHORISED) {
            stopAuthorizationWaitTimer();
            showAuthorisedUi();
            return;
        }

        if (status == AuthStatus.PENDING) {
            showPendingAuthUi();
            waitForAuthorization();
            return;
        }

        RequestState requestState = auth.getAuthRequest() == null ? null : auth.getAuthRequest().getRequestState();
        if (requestState == RequestState.SUCCESS) {
            stopAuthorizationWaitTimer();
            showAuthorisedUi();
            return;
        }

        if (requestState == RequestState.PENDING) {
            showPendingAuthUi();
            waitForAuthorization();
            return;
        }

        stopAuthorizationWaitTimer();
        showUnauthorisedUi();
    }

    private void showAuthorisedUi() {
        displayPanel(userUi.panels().authorisedSpec());
        this.serverInfoDisplayer = new ServerInfoDisplayer(this);
        this.skinLoader = new SkinLoader(this);
        setUserSpace();

        if (auth.getUserDataLoader().getUserServersAttributes() != null
                && !auth.getUserDataLoader().getUserServersAttributes().isEmpty()) {
            serverInfoDisplayer.displayServerInfo(launcher.getConfig().getSelectedServer());
        } else {
            Launcher.LOGGER.warn("No backend-managed servers are available for {}. Server info panel will stay hidden.", getLogin());
        }

        refreshFrame();
    }

    private void showPendingAuthUi() {
        displayPanel(userUi.panels().pendingSpec());
        refreshFrame();
    }

    private void showUnauthorisedUi() {
        displayPanel(userUi.panels().unauthorisedSpec());
        refreshFrame();
    }

    private void displayPanel(String spec) {
        engine.getPanelVisibility().displayPanel(spec);
    }

    private void refreshFrame() {
        if (getPanel() != null) {
            getPanel().revalidate();
            getPanel().repaint();
        }
        if (launcher.getFrame() != null) {
            launcher.getFrame().revalidate();
            launcher.getFrame().repaint();
        }
    }

    /**
     * Waits for authorization completion without blocking the EDT.
     */
    private void waitForAuthorization() {
        if (authorizationWaitTimer != null && authorizationWaitTimer.isRunning()) {
            return;
        }

        authorizationWaitTimer = new Timer(userUi.auth().waitIntervalMs(), e -> {
            switch (auth.getAuthStatus()) {
                case AUTHORISED -> {
                    stopAuthorizationWaitTimer();
                    initializeUser();
                }
                case UNAUTHORISED -> {
                    stopAuthorizationWaitTimer();
                    showUnauthorisedUi();
                }
                case PENDING -> {
                    // Keep waiting.
                }
            }
        });
        authorizationWaitTimer.setRepeats(true);
        authorizationWaitTimer.start();
    }

    private void stopAuthorizationWaitTimer() {
        if (authorizationWaitTimer != null) {
            authorizationWaitTimer.stop();
            authorizationWaitTimer = null;
        }
    }

    public void setBalance(Map<String, AtomicInteger> balance) {
        runOnEDT(() -> {
            String crystals = balanceText(balance, userUi.balance().crystalsKey());
            String units = balanceText(balance, userUi.balance().unitsKey());
            crystalsField.setText(crystals);
            unitsField.setText(units);
        });
    }

    private String balanceText(Map<String, AtomicInteger> balance, String key) {
        AtomicInteger value = balance == null ? null : balance.get(key);
        return value == null ? userUi.balance().fallbackAmount() : value.toString();
    }

    public void setUserSpace() {
        setDropBoxData(loggedForm.getServerBox());
        setupDiscordRpc();
        auth.getUserDataLoader().getBalanceInjector().addListener(this::setBalance);
        setBalance(auth.getUserDataLoader().getBalanceMap());

        groupLoader = new GroupLoader(this);
        loggedForm.getGreetUser().setText(lang.getStringWithKey(
                userUi.greet().localeKey(),
                new String[]{userUi.greet().loginPlaceholder()},
                new String[]{getLogin()}
        ));

        headLoader = new HeadLoader(this, userUi.loaders().headRequestMethod());
        setUserHeadIcon(getLogin());
        configureBadges();
        configureSkin();
        notifyUserLoggedIn();
    }

    private void configureBadges() {
        BadgeLoader badgeLoader = new BadgeLoader(this.getLauncher(), this.userAttributes.login);
        badgeLoader.loadBadgesAsync(
                badges -> SwingUtilities.invokeLater(() -> renderBadges(badges)),
                error -> Engine.getLOGGER().warn(lang.getStringWithKey(
                        userUi.badges().allBadgesFailureKey(),
                        new String[]{"login", "error"},
                        new String[]{this.userAttributes.login, error.getMessage()}
                ))
        );
    }

    private void renderBadges(List<BadgeObject> badges) {
        JPanel badgePanel = launcher.getGuiBuilder().getPanelsMap().get(userUi.panels().userBadges());
        if (badgePanel == null) {
            return;
        }

        badgePanel.removeAll();
        if (badges == null || badges.isEmpty()) {
            badgePanel.setVisible(false);
            badgePanel.revalidate();
            badgePanel.repaint();
            return;
        }

        badgePanel.setVisible(true);
        badgePanel.setLayout(new FlowLayout(FlowLayout.LEFT, userUi.badges().hgap(), userUi.badges().vgap()));

        for (BadgeObject badge : badges) {
            renderBadge(badgePanel, badge);
        }

        badgePanel.revalidate();
        badgePanel.repaint();
    }

    private void renderBadge(JPanel badgePanel, BadgeObject badge) {
        try {
            URL badgeUrl = new URL(this.launcher.getEngineData().getBindUrl() + badge.getBadgeImg());
            BufferedImage image = SvgUtils.renderToImage(
                    badgeUrl,
                    userUi.badges().iconWidth(),
                    userUi.badges().iconHeight()
            );
            JLabel label = new JLabel(new ImageIcon(image));
            label.setToolTipText(badge.getDescription());
            badgePanel.add(label);
        } catch (Exception error) {
            Engine.getLOGGER().warn(lang.getStringWithKey(
                    userUi.badges().singleBadgeFailureKey(),
                    new String[]{"badge", "error"},
                    new String[]{badge.getBadgeName(), error.getMessage()}
            ));
        }
    }

    private void configureSkin() {
        skinLoader.loadSkin(skins -> {
            BufferedImage front = skins.get("front");
            BufferedImage back = skins.get("back");
            JLabel skinLabel = loggedForm.getUserSkin();
            skinLabel.setIcon(new ImageIcon(front));
            configureSkinHover(skinLabel, front, back);
            showTaskMgr();
        });
    }

    private void configureSkinHover(JLabel skinLabel, BufferedImage front, BufferedImage back) {
        if (!skinLabel.isEnabled()) {
            return;
        }
        if (Boolean.TRUE.equals(skinLabel.getClientProperty(SKIN_HOVER_LISTENER_PROPERTY))) {
            return;
        }

        skinLabel.putClientProperty(SKIN_HOVER_LISTENER_PROPERTY, Boolean.TRUE);
        skinLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                runSkinFade(skinLabel, front, back, 0f, 1f);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                runSkinFade(skinLabel, front, back, 1f, 0f);
            }
        });
    }

    private void runSkinFade(JLabel skinLabel, BufferedImage front, BufferedImage back, float fromAlpha, float toAlpha) {
        Object previousTimer = skinLabel.getClientProperty(SKIN_HOVER_TIMER_PROPERTY);
        if (previousTimer instanceof Timer timer && timer.isRunning()) {
            timer.stop();
        }

        int steps = Math.max(1, userUi.skinHover().steps());
        int delay = userUi.skinHover().frameDelayMs();
        final int[] frame = {0};
        Timer timer = new Timer(delay, null);
        timer.addActionListener(event -> {
            frame[0]++;
            float progress = Math.min(1f, frame[0] / (float) steps);
            float alpha = fromAlpha + (toAlpha - fromAlpha) * progress;
            skinLabel.setIcon(new BlendedImageIcon(front, back, alpha));
            if (progress >= 1f) {
                timer.stop();
            }
        });
        skinLabel.putClientProperty(SKIN_HOVER_TIMER_PROPERTY, timer);
        timer.start();
    }

    private void setDropBoxData(DropBox dropBox) {
        String[] servers = auth.getUserDataLoader().getUserServersArray();
        if (servers == null || servers.length == 0) {
            Launcher.LOGGER.warn("User servers array is null or empty. Disabling server dropBox.");
            dropBox.setScrollBoxListener(null);
            dropBox.setValues(new String[0]);
            dropBox.setIcons(new BufferedImage[0]);
            dropBox.setEnabled(false);
            dropBox.revalidate();
            dropBox.repaint();
            return;
        }

        dropBox.setEnabled(true);
        dropBox.setValues(servers);
        runOnEDT(() -> {
            int selectedIndex = launcher.getConfig().getSelectedServer();
            if (selectedIndex < 0 || selectedIndex >= servers.length) {
                Launcher.LOGGER.warn("Selected server index {} is out of bounds. Defaulting to index 0.", selectedIndex);
                selectedIndex = 0;
            }
            dropBox.setSelectedIndex(selectedIndex);
            dropBox.setScrollBoxListener(serverInfoDisplayer);
            launcher.getGuiBuilder()
                    .getComponentFactory()
                    .getLuaUiScriptEngine()
                    .emitComponentEvent(userUi.serverBox().valuesChangedEvent(),
                            dropBox,
                            Map.of("reason", userUi.serverBox().valuesChangedReason()));
            dropBox.revalidate();
            dropBox.repaint();
        });
    }

    private void setupDiscordRpc() {
        if (launcher.getConfig().isDiscordRPC()) {
            launcher.getDiscord().setSmallImageText(lang.getString(userUi.discord().launcherLocaleKey()));
            launcher.getDiscord().discordRpcStart(
                    lang.getStringWithKey(
                            userUi.discord().loginLocaleKey(),
                            new String[]{userUi.discord().loginPlaceholder()},
                            new String[]{auth.getAuthCredentials("login")}
                    ),
                    launcher.getAppTitle(),
                    userUi.discord().iconKey()
            );
        }
    }

    private void notifyUserLoggedIn() {
        String message = lang.getStringWithKey(
                userUi.loginNotification().localeKey(),
                new String[]{userUi.loginNotification().loginPlaceholder()},
                new String[]{getLogin()}
        );
        guiBuilder.getNotification().show(
                Notification.Type.SUCCESS,
                new Rectangle(
                        userUi.loginNotification().x(),
                        loggedForm.getServerBox().getY() + userUi.loginNotification().yOffset(),
                        userUi.loginNotification().width(),
                        userUi.loginNotification().height()
                ),
                userUi.loginNotification().durationMs(),
                message
        );
    }

    @Deprecated
    public void updateServer(int index) {
        launcher.getExecutorServiceProvider().submitTask(() -> {
            try {
                var serverAttr = auth.getUserDataLoader().getUserServersAttributes().get(index);
                String[] status = serverInfo.pollServer(serverAttr.getHost(), serverAttr.getPort());
                Engine.getLOGGER().debug("Server status polled for {}:{} -> {}", serverAttr.getHost(), serverAttr.getPort(), (Object) status);
            } catch (Exception e) {
                Engine.getLOGGER().error("Error refreshing server: {}", e.getMessage());
            }
        }, userUi.tasks().updateServerPrefix() + index);
    }

    private void setUserHeadIcon(String login) {
        if (login == null || login.isEmpty()) {
            Engine.getLOGGER().warn("Login is null or empty. Cannot set user head icon.");
            return;
        }
        headLoader.getUserHeadAsync(login, userHeadImage -> {
            if (userHeadImage == null) {
                Engine.getLOGGER().warn("User head image is null for login: {}", login);
                return;
            }
            try {
                ImageIcon icon = new ImageIcon(engine.getImageUtils().getRoundedImage(
                        engine.getImageUtils().getScaledImage(
                                userHeadImage,
                                userUi.headIcon().size(),
                                userUi.headIcon().size()
                        ),
                        userUi.headIcon().radius()
                ));
                if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
                    Engine.getLOGGER().warn("Generated icon is invalid for login: {}", login);
                    return;
                }
                runOnEDT(() -> userHead.setIcon(icon));
            } catch (Exception e) {
                Engine.getLOGGER().error("Error processing user head icon for login: {}. Error: {}", login, e.getMessage(), e);
            }
        }, e -> Engine.getLOGGER().error("Failed to retrieve user head for login: {}. Error: {}", login, e.getMessage(), e));
    }

    public void setUserGroupLabel(GroupObject groupObject) {
        runOnEDT(() -> {
            userGroup.setText(lang.getString(groupObject.getGroupName()));
            userGroup.setTextColor(hexToColor(groupObject.getGroupColor()));
            userLogin.setText(userAttributes.userFullName);
        });
    }

    public String getLogin() {
        return userAttributes.login;
    }

    public String getToken() {
        return userAttributes.token;
    }

    @Deprecated
    public String getPassword() {
        return userAttributes.password;
    }

    @Deprecated
    public void setNewsData(List<Map<String, String>> newsData) {
        runOnEDT(() -> {
            newsPanel.removeAll();
            newsData.forEach(this::addNewsItem);
            newsPanel.revalidate();
            newsPanel.repaint();
        });
    }

    @Deprecated
    public synchronized Object getUserAttribute(String attributeName) {
        try {
            Field field = UserAttributes.class.getDeclaredField(attributeName);
            field.setAccessible(true);
            return field.get(userAttributes);
        } catch (Exception e) {
            return null;
        }
    }

    public void showTaskMgr() {
        if (auth.getAuthStatus() == AuthStatus.AUTHORISED
                && userUi.taskManager().adminGroup().equals(String.valueOf(this.userAttributes.getGroup()))) {
            runOnEDT(() -> {
                launcher.getExecutorServiceProvider().getExecutorProgress().showTaskMgr();
                taskMgrFrame = launcher.getExecutorServiceProvider().getExecutorProgress().getStatusFrame();
                if (taskMgrFrame != null) {
                    taskMgrFrame.setIconImage(launcher.getImageUtils().getLocalImage(userUi.taskManager().iconPath()));
                    taskMgrFrame.setResizable(userUi.taskManager().resizable());
                    Point parentLocation = launcher.getFrame().getLocationOnScreen();
                    int parentX = parentLocation.x;
                    int parentY = parentLocation.y;
                    taskMgrFrame.setLocation(parentX + launcher.getFrame().getWidth(), parentY);
                    taskMgrFrame.setVisible(true);
                }
            });
        }
    }

    @Deprecated
    private void addNewsItem(Map<String, String> newsItem) {
        String key = newsItem.get("key");
        String value = newsItem.get("value");

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(
                userUi.newsItem().insetTop(),
                userUi.newsItem().insetLeft(),
                userUi.newsItem().insetBottom(),
                userUi.newsItem().insetRight()
        ));
        newsPanel.add(panel);

        JLabel keyLabel = new JLabel(key);
        keyLabel.setForeground(Color.decode(userUi.newsItem().keyColor()));
        keyLabel.setFont(launcher.getFONTUTILS().getFont(userUi.newsItem().keyFont(), userUi.newsItem().fontSize()));
        panel.add(keyLabel);

        JTextArea valueLabel = new JTextArea(String.valueOf(value));
        valueLabel.setForeground(Color.decode(userUi.newsItem().valueColor()));
        valueLabel.setFont(launcher.getFONTUTILS().getFont(userUi.newsItem().valueFont(), userUi.newsItem().fontSize()));
        valueLabel.setEditable(false);
        valueLabel.setOpaque(false);
        valueLabel.setLineWrap(true);
        valueLabel.setWrapStyleWord(true);
        panel.add(valueLabel);
    }

    public GuiBuilder getGuiBuilder() {
        return guiBuilder;
    }

    public JPanel getNewsPanel() {
        return newsPanel;
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public String getUuid() {
        return userAttributes.uuid;
    }

    public Auth getAuth() {
        return auth;
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    private void runOnEDT(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    public UserAttributes getUserAttributes() {
        return userAttributes;
    }

    public GroupLoader getGroupLoader() {
        return groupLoader;
    }
}
