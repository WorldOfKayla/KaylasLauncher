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
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class User extends org.foxesworld.engine.user.User {
    private final Auth auth;
    private final UserServers userServers;
    private final Launcher launcher;
    private final LanguageProvider lang;
    private final ServerInfo serverInfo;
    private final ServerBox serverBox;
    private final GuiBuilder guiBuilder;
    private final UserAttributes userAttributes;
    private final JPanel newsPanel;
    private final ServerInfoDisplayer serverInfoDisplayer;
    private JFrame taskMgrFrame;

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
        this.serverInfoDisplayer = new ServerInfoDisplayer(this);

        initializeUser();
    }

    private void initializeUser() {
        if (auth.isAuthorised()) {
            setUserSpace();
            setDropBoxData(userServers.getServerListBox());
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
        dropBox.setScrollBoxListener(this.serverInfoDisplayer);
    }

    @Override
    protected void setUserSpace() {
        auth.getEngine().getPanelVisibility().displayPanel("authForm->false|loggedForm->true");
        updateUserAttributes(auth.getAuthCredentials());
        setUserHeadIcon(getLogin());
        setUserGroupLabel();
        setupDiscordRpc();
        notifyUserLoggedIn();
    }

    private void setupDiscordRpc() {
        if(this.launcher.getConfig().isDiscordRPC()) {
            launcher.getDiscord().setSmallImageText(launcher.getLANG().getString("general.launcher"));
            launcher.getDiscord().discordRpcStart(
                    lang.getStringWithKey("game.login", new String[]{"login"}, new String[]{auth.getAuthCredentials("login")}),
                    launcher.getAppTitle(),
                    "launcher"
            );
        }
    }

    private void notifyUserLoggedIn() {
        String message = lang.getStringWithKey("auth.loggedIn", new String[]{"login"}, new String[]{getLogin()});
        guiBuilder.getNotification().show(Notification.Type.SUCCESS, new Rectangle(10, serverBox.getY() + 80, 340, 45), 3000, message);
    }

    public void updateUserAttributes(Map<String, Object> attributes) {
        attributes.forEach(this::setUserAttribute);
    }

    public void updateServer(int index) {
        this.launcher.getExecutorServiceProvider().submitTask(() -> {
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
        }, "updateServer"+index);
    }

    public void setUserAttribute(String attributeName, Object attributeValue) {
        try {
            Field field = UserAttributes.class.getDeclaredField(attributeName);
            field.setAccessible(true);

            // Convert the value to the appropriate type
            Object value = convertValue(field.getType(), attributeValue);
            field.set(userAttributes, value);  // Update the userAttributes object
        } catch (NoSuchFieldException e) {
            Engine.getLOGGER().warn("No such field: " + attributeName);
        } catch (IllegalAccessException e) {
            Engine.getLOGGER().error("Cannot access field: " + e.getMessage());
        } catch (Exception e) {
            Engine.getLOGGER().error("Error setting user attribute: " + e.getMessage());
        }
    }

    private void setUserHeadIcon(String login) {
        getUserHeadAsync(login, userHeadBase64 -> {
            if (userHeadBase64 != null) {
                try {
                    BufferedImage userHeadImage = engine.getImageUtils().base64ToBufferedImage(userHeadBase64);
                    ImageIcon icon = new ImageIcon(engine.getImageUtils().getRoundedImage(userHeadImage, 5));

                    // Update the icon on the Event Dispatch Thread (EDT)
                    SwingUtilities.invokeLater(() -> ((JLabel) getComponent("userHead")).setIcon(icon));
                } catch (Exception e) {
                    Engine.getLOGGER().error("Error setting user head icon: {}", e.getMessage(), e);
                }
            } else {
                Engine.getLOGGER().warn("User head base64 string is null for login: {}", login);
            }
        }, e -> {
            Engine.getLOGGER().error("Failed to retrieve user head for login: {}. Error: {}", login, e.getMessage());
        });
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
        SwingUtilities.invokeLater(() -> {
            newsPanel.removeAll();
            newsData.forEach(this::addNewsItem);
            newsPanel.revalidate();
            newsPanel.repaint();
        });
    }

    public void showTaskMgr(){
        if (this.getUserGroup() == 1) {
            SwingUtilities.invokeLater(() -> {
                taskMgrFrame = this.launcher.getExecutorServiceProvider().getExecutorProgress().getStatusFrame();
                taskMgrFrame.setIconImage(this.launcher.getImageUtils().getLocalImage("assets/ui/icons/threadBolt.png"));
                taskMgrFrame.setResizable(false);
                Point parentLocation = this.launcher.getFrame().getLocationOnScreen();
                int parentX = parentLocation.x;
                int parentY = parentLocation.y;
                taskMgrFrame.setLocation(parentX + this.launcher.getFrame().getWidth(), parentY);
                taskMgrFrame.setVisible(true);
            });
        }
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

    private Object convertValue(Class<?> fieldType, Object value) {
        if (fieldType == boolean.class || fieldType == Boolean.class) {
            return Boolean.parseBoolean(value.toString());
        } else if (fieldType == int.class || fieldType == Integer.class) {
            return Integer.parseInt(value.toString());
        } else if (fieldType == long.class || fieldType == Long.class) {
            return Long.parseLong(value.toString());
        } else if (fieldType == double.class || fieldType == Double.class) {
            return Double.parseDouble(value.toString());
        } else if (fieldType == float.class || fieldType == Float.class) {
            return Float.parseFloat(value.toString());
        }
        return value;  // Return the value as is if no conversion is needed
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

    public ServerInfoDisplayer getServerInfoDisplayer() {
        return serverInfoDisplayer;
    }

    public int getUserGroup() {
        return Integer.parseInt(String.valueOf(this.userAttributes.group));
    }
}