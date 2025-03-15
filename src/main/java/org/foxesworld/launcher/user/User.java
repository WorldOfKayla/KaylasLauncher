package org.foxesworld.launcher.user;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.HTTP.OnFailure;
import org.foxesworld.engine.utils.HTTP.OnSuccess;
import org.foxesworld.engine.utils.ServerInfo;
import org.foxesworld.launcher.auth.Auth;
import org.foxesworld.launcher.server.ServerInfoDisplayer;
import org.foxesworld.launcher.server.ServerParser;
import org.foxesworld.notification.Notification;
import org.foxesworld.engine.utils.DataInjector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class User extends org.foxesworld.engine.user.User {
    private final Auth auth;
    private final LoggedForm loggedForm;
    private final Launcher launcher;
    private final LanguageProvider lang;
    private final ServerInfo serverInfo;
    private final GuiBuilder guiBuilder;
    private final UserAttributes userAttributes;
    private final JPanel newsPanel;
    private final ServerInfoDisplayer serverInfoDisplayer;
    private final SkinLoader skinLoader;
    private String[] userServersArray;
    private List<ServerAttributes> userServersAttributes;
    private JFrame taskMgrFrame;

    @org.foxesworld.engine.gui.componentAccessor.Component
    @SuppressWarnings("unused")
    private Label userGroup, userLogin, crystalsField, unitsField;

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
        this.serverInfoDisplayer = new ServerInfoDisplayer(this);
        this.skinLoader = new SkinLoader(this);
        SwingUtilities.invokeLater(this::initializeUser);
    }

    public void loadUserServers(final String login, final DataInjector<List<ServerAttributes>> serversInjector) {
        if (login == null || login.isEmpty()) {
            Engine.getLOGGER().warn("Empty login provided, aborting loadUserServers.");
            return;
        }
        ServerParser serverParser = new ServerParser(engine);
        List<ServerAttributes> loadedServers = serverParser.parseServers(login);
        Engine.getLOGGER().info("Loaded {} servers", loadedServers.size());
        serversInjector.setContent(loadedServers);
    }

    public void initializeUser() {
        if (auth.isAuthorised()) {
            engine.getPanelVisibility().displayPanel("loggedForm->true|newsForm->true|authForm->false");
            setUserSpace();
            serverInfoDisplayer.displayServerInfo(launcher.getConfig().getSelectedServer());
        } else {
            engine.getPanelVisibility().displayPanel("loggedForm->false|newsForm->true|authForm->true");
        }
        getPanel().repaint();
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
        setUserHeadIcon(getLogin());
        setUserGroupLabel();
        setupDiscordRpc();
        auth.getUserDataLoader().getBalanceInjector().addListener(this::setBalance);
        loggedForm.getGreetUser().setText(lang.getStringWithKey("logged.greet", new String[]{"login"}, new String[]{getLogin()}));
        skinLoader.loadSkin(skins -> {
            BufferedImage front = skins.get("front");
            BufferedImage back = skins.get("back");
            Label skinLabel = loggedForm.getUserSkin();
            skinLabel.setIcon(new ImageIcon(front));
            if (skinLabel.isEnabled()) {
                skinLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        skinLabel.setIcon(new ImageIcon(back));
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        skinLabel.setIcon(new ImageIcon(front));
                    }
                });
            }
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
                String text = serverInfo.genServerStatus(status);
                BufferedImage img = serverInfo.genServerIcon(status);
            } catch (Exception e) {
                Engine.getLOGGER().error("Error refreshing server: {}", e.getMessage());
            }
        }, "updateServer" + index);
    }

    @Override
    protected void getUserHeadAsync(String login, OnSuccess<String> onSuccess, OnFailure onFailure) {
        if (login == null || login.isEmpty()) {
            Engine.getLOGGER().warn("Login is null or empty in getUserHead");
            if (onFailure != null) {
                onFailure.onFailure(new IllegalArgumentException("Login cannot be null or empty"));
            }
            return;
        }
        Map<String, Object> skinData = new HashMap<>();
        skinData.put("sysRequest", "userHead");
        skinData.put("login", login);
        HTTPrequest httpRequest = new HTTPrequest(launcher, "GET");
        httpRequest.sendAsync(skinData, response -> {
            if (response != null && !response.toString().isEmpty()) {
                onSuccess.onSuccess((String) response);
            } else {
                Engine.getLOGGER().warn("Received empty or null response for user head request for login: {}", login);
                if (onFailure != null) {
                    onFailure.onFailure(new Exception("Received empty or null response"));
                }
            }
        }, e -> {
            Engine.getLOGGER().error("Error while sending user head request for login: {}", login, e);
            if (onFailure != null) {
                onFailure.onFailure(e);
            }
        });
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
                BufferedImage userHeadImage = engine.getImageUtils().base64ToBufferedImage(userHeadBase64);
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
                    try {
                        Component component = getComponent("userHead");
                        if (component instanceof JLabel) {
                            ((JLabel) component).setIcon(icon);
                        } else {
                            Engine.getLOGGER().warn("Component 'userHead' is not a JLabel for login: {}", login);
                        }
                    } catch (Exception e) {
                        Engine.getLOGGER().error("Error updating user head icon on UI for login: {}. Error: {}", login, e.getMessage(), e);
                    }
                });
            } catch (Exception e) {
                Engine.getLOGGER().error("Error processing user head icon for login: {}. Error: {}", login, e.getMessage(), e);
            }
        });
        getUserHeadAsync(login, headInjector::setContent, e -> Engine.getLOGGER().error("Failed to retrieve user head for login: {}. Error: {}", login, e.getMessage(), e));
    }

    private void setUserGroupLabel() {
        runOnEDT(() -> {
            String groupKey = "group.group-" + auth.getAuthCredentials("group");
            userGroup.setText(lang.getString(groupKey));
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
        if (auth.isAuthorised() && getUserGroup() == UserGroup.ADMIN) {
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

    public LoggedForm getUserServers() {
        return loggedForm;
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

    public ServerInfoDisplayer getServerInfoDisplayer() {
        return serverInfoDisplayer;
    }

    public UserGroup getUserGroup() {
        if (auth.isAuthorised()) {
            return UserGroup.fromGroupId(Integer.parseInt((String) userAttributes.group));
        }
        return UserGroup.GUEST;
    }

    public int getUserIntGroup() {
        return Integer.parseInt((String) userAttributes.group);
    }

    public String getUserFullName() {
        return userAttributes.userFullName;
    }

    public String getColorScheme() {
        return userAttributes.colorScheme;
    }

    // Вспомогательный метод для сокращения вызовов SwingUtilities.invokeLater
    private void runOnEDT(Runnable task) {
        SwingUtilities.invokeLater(task);
    }

    public String[] getUserServersArray() {
        return userServersArray;
    }

    public List<ServerAttributes> getUserServersAttributes() {
        return userServersAttributes;
    }
}