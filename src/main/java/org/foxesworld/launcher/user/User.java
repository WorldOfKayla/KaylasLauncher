package org.foxesworld.launcher.user;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.componentAccessor.Component;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.utils.HTTP.RequestState;
import org.foxesworld.engine.utils.ServerInfo;
import org.foxesworld.launcher.auth.Auth;
import org.foxesworld.launcher.auth.AuthStatus;
import org.foxesworld.launcher.gui.BlendedImageIcon;
import org.foxesworld.launcher.gui.GifPlayerSwing;
import org.foxesworld.launcher.server.ServerInfoDisplayer;
import org.foxesworld.launcher.user.loader.*;
import org.foxesworld.launcher.utils.SvgUtils;
import org.foxesworld.notification.Notification;
import org.foxesworld.engine.utils.DataInjector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.foxesworld.engine.utils.FontUtils.hexToColor;

public class User extends org.foxesworld.engine.user.User {
    private final Auth auth;
    private final LoggedForm loggedForm;
    private final Launcher launcher;
    private final LanguageProvider lang;
    private final ServerInfo serverInfo;
    private final GuiBuilder guiBuilder;
    private final UserAttributes userAttributes;
    private final JPanel newsPanel;
    private ServerInfoDisplayer serverInfoDisplayer;
    private SkinLoader skinLoader;
    private HeadLoader headLoader;
    private GroupLoader groupLoader;
    private JFrame taskMgrFrame;

    @Component
    @SuppressWarnings("unused")
    private Label userGroup, userLogin, crystalsField, unitsField, userHead;

    public User(Launcher launcher) {
        super(launcher.getGuiBuilder(), "userPane", List.of(Label.class));
        this.launcher = launcher;
        this.auth = launcher.getAuth();
        this.userAttributes = new UserAttributes(this);
        this.loggedForm = new LoggedForm(launcher.getGuiBuilder(), "loggedForm", List.of(DropBox.class, Label.class));
        this.engine = launcher.getEngine();
        this.serverInfo = engine.getServerInfo();
        this.lang = launcher.getLANG();
        this.guiBuilder = launcher.getGuiBuilder();
        this.newsPanel = guiBuilder.getPanelsMap().get("newsForm");
        SwingUtilities.invokeLater(this::initializeUser);
    }

    public void initializeUser() {
        if (auth.getAuthRequest() != null) {
            RequestState status = auth.getAuthRequest().getRequestState();
            if (status != RequestState.FAILED) {
                if (status == RequestState.PENDING) {
                    engine.getPanelVisibility().displayPanel("loggedForm->false|newsForm->true|authForm->true");
                    waitForAuthorization();
                    return;
                }

                if (status == RequestState.SUCCESS) {
                    engine.getPanelVisibility().displayPanel("loggedForm->true|newsForm->true|authForm->false");
                    this.serverInfoDisplayer = new ServerInfoDisplayer(this);
                    this.skinLoader = new SkinLoader(this);
                    setUserSpace();
                    serverInfoDisplayer.displayServerInfo(launcher.getConfig().getSelectedServer());
                } else {
                    engine.getPanelVisibility().displayPanel("loggedForm->false|newsForm->true|authForm->true");
                }
                getPanel().repaint();
            }
        }
    }

    /**
     * Ожидает завершения авторизации без блокировки основного потока.
     * спользуется Swing Timer для периодической проверки статуса.
     */
    private void waitForAuthorization() {
        Timer timer = new Timer(2000, e -> {
            switch (auth.getAuthStatus()) {
                case AUTHORISED -> {
                    ((Timer) e.getSource()).stop();
                    SwingUtilities.invokeLater(this::initializeUser);
                }

                case UNAUTHORISED -> {
                    launcher.getSOUND().playSound("other", "alert");
                }
            }
        });
        timer.setRepeats(true);
        timer.start();
    }


    public void setBalance(Map<String, AtomicInteger> balance) {
        runOnEDT(() -> {
            String crystals = balance.get("crystals") != null ? balance.get("crystals").toString() : "0";
            String units = balance.get("units") != null ? balance.get("units").toString() : "0";
            crystalsField.setText(crystals);
            unitsField.setText(units);
        });
    }

    public void setUserSpace() {
        setDropBoxData(loggedForm.getServerBox());
        setupDiscordRpc();
        auth.getUserDataLoader().getBalanceInjector().addListener(this::setBalance);
        groupLoader = new GroupLoader(this);
        loggedForm.getGreetUser().setText(lang.getStringWithKey("logged.greet", new String[]{"login"}, new String[]{getLogin()}));
        headLoader = new HeadLoader(this, "GET");
        setUserHeadIcon(getLogin());

        BadgeLoader badgeLoader = new BadgeLoader(this.getLauncher(), this.userAttributes.login);
        badgeLoader.loadBadgesAsync(
                // --- onSuccess ---
                // --- onSuccess ---
                (List<BadgeObject> badges) -> {
                    System.out.println("Значки для пользователя " + this.userAttributes.login + " успешно загружены!");

                    SwingUtilities.invokeLater(() -> {
                        JPanel badgePanel = launcher.getGuiBuilder().getPanelsMap().get("userBadges");
                        if (badgePanel == null) {
                            System.err.println("Панель 'userBadges' не найдена!");
                            return;
                        }

                        badgePanel.removeAll();

                        if (badges.isEmpty()) {
                            badgePanel.setVisible(false);
                        } else {
                            badgePanel.setVisible(true);

                            // *** ГЛАВНОЕ ИЗМЕНЕНИЕ ЗДЕСЬ ***
                            // Устанавливаем FlowLayout с горизонтальным отступом 5 пикселей
                            // и вертикальным отступом 0. Выравнивание по левому краю.
                            badgePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 4));

                            for (BadgeObject badge : badges) {
                                try {
                                    URL badgeUrl = new URL(this.launcher.getEngineData().getBindUrl() + badge.getBadgeImg());
                                    BufferedImage image = SvgUtils.renderToImage(badgeUrl, 25, 25);

                                    JLabel label = new JLabel(new ImageIcon(image));
                                    label.setToolTipText(badge.getDescription());
                                    badgePanel.add(label);

                                } catch (Exception e) {
                                    System.err.println("Не удалось загрузить значок " + badge.getBadgeName() + ": " + e.getMessage());
                                }
                            }
                        }

                        badgePanel.revalidate();
                        badgePanel.repaint();
                    });
                },

// --- onFailure ---
                (Throwable error) -> {
                    System.err.println("Не удалось загрузить значки для " + this.userAttributes.login + ". Причина: " + error.getMessage());
                }
        );

        skinLoader.loadSkin(skins -> {
            BufferedImage front = skins.get("front");
            BufferedImage back = skins.get("back");
            JLabel skinLabel = loggedForm.getUserSkin();
            skinLabel.setIcon(new ImageIcon(front));

            // Если компонент активен, устанавливаем слушатели для плавной смены скина
            if (skinLabel.isEnabled()) {
                // Параметры анимации
                final int animationDuration = 1; // общая длительность анимации в мс
                final int animationSteps = 15;       // количество шагов обновления
                final int delay = animationDuration / animationSteps; // интервал между шагами в мс

                skinLabel.addMouseListener(new MouseAdapter() {
                    Timer timer;
                    float alpha = 0f;

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        if (timer != null && timer.isRunning()) {
                            timer.stop();
                        }
                        alpha = 0f;
                        timer = new Timer(delay, null);
                        timer.addActionListener(ae -> {
                            alpha += 1.0f / animationSteps;
                            if (alpha >= 1f) {
                                alpha = 1f;
                                timer.stop();
                            }
                            skinLabel.setIcon(new BlendedImageIcon(front, back, alpha));
                        });
                        timer.start();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        if (timer != null && timer.isRunning()) {
                            timer.stop();
                        }
                        alpha = 1f;
                        timer = new Timer(delay, null);
                        timer.addActionListener(ae -> {
                            alpha -= 1.0f / animationSteps;
                            if (alpha <= 0f) {
                                alpha = 0f;
                                timer.stop();
                            }
                            skinLabel.setIcon(new BlendedImageIcon(front, back, alpha));
                        });
                        timer.start();
                    }
                });
            }
            showTaskMgr();
        });
        notifyUserLoggedIn();
    }


    private void setDropBoxData(DropBox dropBox) {
        String[] servers = auth.getUserDataLoader().getUserServersArray();
        if (servers == null || servers.length == 0) {
            Launcher.LOGGER.warn("User servers array is null or empty. Setting empty values for dropBox.");
            dropBox.setValues(new String[0]);
            return;
        }
        dropBox.setValues(servers);
        runOnEDT(() -> {
            int selectedIndex = launcher.getConfig().getSelectedServer();
            if (selectedIndex < 0 || selectedIndex >= servers.length) {
                Launcher.LOGGER.warn("Selected server index {} is out of bounds. Defaulting to index 0.", selectedIndex);
                selectedIndex = 0;
            }
            dropBox.setSelectedIndex(selectedIndex);
            dropBox.setScrollBoxListener(serverInfoDisplayer);
        });
    }

    private void setupDiscordRpc() {
        if (launcher.getConfig().isDiscordRPC()) {
            launcher.getDiscord().setSmallImageText(lang.getString("general.launcher"));
            launcher.getDiscord().discordRpcStart(
                    lang.getStringWithKey("game.login", new String[]{"login"}, new String[]{auth.getAuthCredentials("login")}),
                    launcher.getAppTitle(),
                    "launcher"
            );
        }
    }

    private void notifyUserLoggedIn() {
        String message = lang.getStringWithKey("auth.loggedIn", new String[]{"login"}, new String[]{getLogin()});
        guiBuilder.getNotification().show(Notification.Type.SUCCESS,
                new Rectangle(10, loggedForm.getServerBox().getY() + 40, 340, 45), 3000, message);
    }

    @Deprecated
    public void updateServer(int index) {
        launcher.getExecutorServiceProvider().submitTask(() -> {
            try {
                var serverAttr = auth.getUserDataLoader().getUserServersAttributes().get(index);
                String ip = serverAttr.getHost();
                int port = serverAttr.getPort();
                String[] status = serverInfo.pollServer(ip, port);
                // Дополнительная логика обработки статуса сервера
                //String text = serverInfo.genServerStatus(status);
                //BufferedImage img = serverInfo.genServerIcon(status);
            } catch (Exception e) {
                Engine.getLOGGER().error("Error refreshing server: {}", e.getMessage());
            }
        }, "updateServer" + index);
    }

    private void setUserHeadIcon(String login) {
        if (login == null || login.isEmpty()) {
            Engine.getLOGGER().warn("Login is null or empty. Cannot set user head icon.");
            return;
        }
        DataInjector<String> headInjector = new DataInjector<>();
        headInjector.addListener(userHeadBase64 -> {
            if (userHeadBase64 == null) {
                Engine.getLOGGER().warn("User head base64 string is null for login: {}", login);
                return;
            }
            try {
                BufferedImage userHeadImage = engine.getImageUtils().fromBase64(userHeadBase64).get();
                if (userHeadImage == null) {
                    Engine.getLOGGER().warn("Decoded user head image is null for login: {}", login);
                    return;
                }
                ImageIcon icon = new ImageIcon(engine.getImageUtils().getRoundedImage(
                        engine.getImageUtils().getScaledImage(userHeadImage, 72, 72), 80));
                if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
                    Engine.getLOGGER().warn("Generated icon is invalid for login: {}", login);
                    return;
                }
                runOnEDT(() -> {
                    userHead.setIcon(icon);
                });
            } catch (Exception e) {
                Engine.getLOGGER().error("Error processing user head icon for login: {}. Error: {}", login, e.getMessage(), e);
            }
        });
        headLoader.getUserHeadAsync(login, headInjector::setContent, e -> Engine.getLOGGER().error("Failed to retrieve user head for login: {}. Error: {}", login, e.getMessage(), e));
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
        if (auth.getAuthStatus() == AuthStatus.AUTHORISED && this.userAttributes.getGroup().equals("1")) {
            runOnEDT(() -> {
                launcher.getExecutorServiceProvider().getExecutorProgress().showTaskMgr();
                taskMgrFrame = launcher.getExecutorServiceProvider().getExecutorProgress().getStatusFrame();
                if (taskMgrFrame != null) {
                    taskMgrFrame.setIconImage(launcher.getImageUtils().getLocalImage("assets/ui/icons/threadBolt.png"));
                    taskMgrFrame.setResizable(false);
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
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        newsPanel.add(panel);

        JLabel keyLabel = new JLabel(key);
        keyLabel.setForeground(Color.BLACK);
        keyLabel.setFont(launcher.getFONTUTILS().getFont("mcfontBold", 11));
        panel.add(keyLabel);

        JTextArea valueLabel = new JTextArea(String.valueOf(value));
        valueLabel.setForeground(Color.GRAY);
        valueLabel.setFont(launcher.getFONTUTILS().getFont("mcfont", 11));
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
        SwingUtilities.invokeLater(task);
    }

    public UserAttributes getUserAttributes() {
        return userAttributes;
    }

    public GroupLoader getGroupLoader() {
        return groupLoader;
    }
}