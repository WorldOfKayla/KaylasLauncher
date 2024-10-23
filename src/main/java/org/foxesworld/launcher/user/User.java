package org.foxesworld.launcher.user;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.gui.components.serverBox.ServerBox;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.utils.ServerInfo;
import org.foxesworld.launcher.auth.Auth;
import org.foxesworld.launcher.server.ServerInfoDisplayer;
import org.foxesworld.notification.Notification;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public class User extends org.foxesworld.engine.user.User {
    private final Auth auth;
    private final UserServers userServers;
    private final Launcher launcher;
    private final ExecutorService executorService;
    private final LanguageProvider lang;
    private final ServerInfo serverInfo;
    private final ServerBox serverBox;
    private final GuiBuilder guiBuilder;
    private final UserAttributes userAttributes;
    private final JPanel newsPanel;
    private volatile ServerInfoDisplayer serverInfoDisplayer;

    public User(Launcher launcher) {
        super(launcher.getGuiBuilder(), "userPane", List.of(Label.class));
        this.launcher = launcher;
        this.auth = launcher.getAuth();
        this.userServers = new UserServers(launcher.getGuiBuilder(), "loggedForm", List.of(ServerBox.class, DropBox.class));
        this.engine = launcher.getEngine();
        this.serverInfo = engine.getServerInfo();
        this.serverInfo.setServerStatusImg(engine.getImageUtils().getLocalImage("assets/ui/components/icons/status.png"));
        this.serverBox = userServers.getServerBox();
        this.lang = launcher.getLANG();
        this.guiBuilder = launcher.getGuiBuilder();
        this.userAttributes = new UserAttributes(this);
        this.newsPanel = guiBuilder.getPanelsMap().get("newsForm");
        this.executorService = Executors.newCachedThreadPool();

        initializeUser();
        setDropBoxData(userServers.getServerListBox());
    }

    private void initializeUser() {
        if (auth.isAuthorised()) {
            setUserSpace();
        } else {
            displayLoginPanel();
        }
    }

    private void displayLoginPanel() {
        engine.getPanelVisibility().displayPanel("loggedForm->false|newsForm->true|authForm->true");
    }

    private void setDropBoxData(DropBox dropBox) {
        dropBox.setValues(auth.getUserServersArray());
        dropBox.setSelectedIndex(auth.getLauncher().getConfig().getSelectedServer());
        dropBox.setScrollBoxListener(new ServerInfoDisplayer(this));
    }

    @Override
    protected void setUserSpace() {
        auth.getEngine().getPanelVisibility().displayPanel("authForm->false|loggedForm->true");
        updateUserAttributes();
        setUserHeadIcon(getLogin());
        setUserGroupLabel();
        setupDiscordRpc();
        notifyUserLoggedIn();
    }

    private void setupDiscordRpc() {
        launcher.getDiscord().setSmallImageText(launcher.getLANG().getString("general.launcher"));
        launcher.getDiscord().discordRpcStart(
                lang.getStringWithKey("game.login", new String[]{"login"}, new String[]{auth.getAuthCredentials("login")}),
                launcher.getAppTitle(),
                "launcher"
        );
    }

    private void notifyUserLoggedIn() {
        String message = lang.getStringWithKey("auth.loggedIn", new String[]{"login"}, new String[]{getLogin()});
        guiBuilder.getNotification().show(Notification.Type.SUCCESS, new Rectangle(10, serverBox.getY() + 80, 340, 45), 3000, message);
    }

    private void updateUserAttributes() {
        auth.getAuthCredentials().forEach(this::setUserAttribute);
    }

    public void updateServer(int index) {
        executorService.submit(() -> {
            try {
                String ip = auth.getUserServersAttributes().get(index).getHost();
                int port = auth.getUserServersAttributes().get(index).getPort();
                serverBox.updateBox(lang.getString("server.updating"), serverInfo.genServerIcon(new String[]{null, "0", null}));

                String[] status = serverInfo.pollServer(ip, port);
                String text = serverInfo.genServerStatus(status);
                BufferedImage img = serverInfo.genServerIcon(status);

                SwingUtilities.invokeLater(() -> serverBox.updateBox(text, img));
            } catch (Exception e) {
                Engine.getLOGGER().error("Error refreshing server: {}", e.getMessage());
            }
        });
    }

    private void setUserAttribute(String key, Object value) {
        try {
            Field field = userAttributes.getClass().getDeclaredField(key);
            field.setAccessible(true);
            synchronized (userAttributes) {
                field.set(userAttributes, value);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Engine.getLOGGER().error("Error setting user attribute: {}", e.getMessage());
        }
    }

    private void setUserHeadIcon(String login) {
        String userHeadBase64 = getUserHead(login);
        if (userHeadBase64 != null) {
            try {
                BufferedImage userHeadImage = engine.getImageUtils().base64ToBufferedImage(userHeadBase64);
                ImageIcon icon = new ImageIcon(engine.getImageUtils().getRoundedImage(userHeadImage, 5));
                ((JLabel) getComponent("userHead")).setIcon(icon);
            } catch (Exception e) {
                Engine.getLOGGER().error("Error setting user head icon: {}", e.getMessage(), e);
            }
        } else {
            Engine.getLOGGER().warn("User head base64 string is null for login: {}", login);
        }
    }

    protected String getUserHead(String login) {
        if (login == null || login.isEmpty()) {
            Engine.getLOGGER().warn("Login is null or empty in getUserHead");
            return null;
        }

        Map<String, Object> skinData = new HashMap<>();
        skinData.put("sysRequest", "skin");
        skinData.put("show", "head");
        skinData.put("login", login);

        String response = null;
        try {
            response = this.engine.getPOSTrequest().send(skinData);
            if (response == null || response.isEmpty()) {
                Engine.getLOGGER().warn("Received empty or null response for user head request for login: {}", login);
            }
        } catch (Exception e) {
            Engine.getLOGGER().error("Error while sending user head request for login: {}", login, e);
        }

        return response;
    }

    private void setUserGroupLabel() {
        String groupKey = "group.group-" + auth.getAuthCredentials("group");
        SwingUtilities.invokeLater(() -> ((JLabel) getComponent("userGroup")).setText(lang.getString(groupKey)));
    }

    public String getLogin() {
        return userAttributes.login;
    }

    public String getPassword() {
        return userAttributes.password;
    }

    public String getUnits() {
        return userAttributes.units;
    }

    public String getToken() {
        return userAttributes.token;
    }

    public void setNewsData(List<Map<String, String>> newsData) {
        newsPanel.removeAll();
        newsData.forEach(this::addNewsItem);
        newsPanel.revalidate();
        newsPanel.repaint();
    }

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

    public UserServers getUserServers() {
        return userServers;
    }

    public String getUuid() {
        return userAttributes.uuid;
    }

    public Auth getAuth() {
        return auth;
    }

    public int getUserGroup() {
        return (int) this.userAttributes.group;
    }
}