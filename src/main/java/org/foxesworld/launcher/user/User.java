package org.foxesworld.launcher.user;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.utils.ServerInfo;
import org.foxesworld.launcher.auth.Auth;
import org.foxesworld.launcher.auth.Balance;
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
    private final GuiBuilder guiBuilder;
    private final UserAttributes userAttributes;
    private final JPanel newsPanel;
    private final ServerInfoDisplayer serverInfoDisplayer;
    private JFrame taskMgrFrame;

    public User(Launcher launcher) {
        super(launcher.getGuiBuilder(), "userPane", List.of(Label.class));
        this.launcher = launcher;
        this.auth = launcher.getAuth();
        this.userServers = new UserServers(launcher.getGuiBuilder(), "loggedForm", List.of(DropBox.class));
        this.engine = launcher.getEngine();
        this.serverInfo = engine.getServerInfo();
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

        } else {
            displayLoginPanel();
        }
        //this.showTaskMgr();
    }

    public void setBalance(List<Map<String,Integer>> balance){
        ((Label)this.getComponent("crystalsField")).setText(balance.get(0).toString());
    }

    private void displayLoginPanel() {
        engine.getPanelVisibility().displayPanel("loggedForm->false|newsForm->true|authForm->true");
    }

    private void setDropBoxData(DropBox dropBox) {
        dropBox.setValues(auth.getUserServersArray());
        dropBox.setSelectedIndex(this.launcher.getConfig().getSelectedServer());
        dropBox.setScrollBoxListener(this.serverInfoDisplayer);
    }

    @Override
    protected void setUserSpace() {
        auth.getEngine().getPanelVisibility().displayPanel("authForm->false|loggedForm->true");
        updateUserAttributes(auth.getAuthCredentials());
        setDropBoxData(userServers.getServerListBox());
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
        guiBuilder.getNotification().show(Notification.Type.SUCCESS, new Rectangle(10, userServers.getServerListBox().getY() + 140, 340, 45), 3000, message);
    }

    public void updateUserAttributes(Map<String, Object> attributes) {
        attributes.forEach(this::setUserAttribute);
    }

    public void updateServer(int index) {
        this.launcher.getExecutorServiceProvider().submitTask(() -> {
            try {
                String ip = auth.getUserServersAttributes().get(index).getHost();
                int port = auth.getUserServersAttributes().get(index).getPort();
                //serverBox.updateBox(lang.getString("server.updating"), serverInfo.genServerIcon(new String[]{null, "0", null}));

                String[] status = serverInfo.pollServer(ip, port);
                String text = serverInfo.genServerStatus(status);
                BufferedImage img = serverInfo.genServerIcon(status);

                //SwingUtilities.invokeLater(() -> serverBox.updateBox(text, img));
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
        if (login == null || login.isEmpty()) {
            Engine.getLOGGER().warn("Login is null or empty. Cannot set user head icon.");
            return;
        }

        getUserHeadAsync(login, userHeadBase64 -> {
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

                ImageIcon icon = new ImageIcon(engine.getImageUtils().getRoundedImage(userHeadImage, 5));
                if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
                    Engine.getLOGGER().warn("Generated icon is invalid for login: {}", login);
                    return;
                }

                SwingUtilities.invokeLater(() -> {
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
        }, e -> Engine.getLOGGER().error("Failed to retrieve user head for login: {}. Error: {}", login, e.getMessage(), e));
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
        if(auth.isAuthorised()) {
            if (this.getUserGroup() == UserGroup.ADMIN) {
                SwingUtilities.invokeLater(() -> {
                    this.launcher.getExecutorServiceProvider().getExecutorProgress().showTaskMgr();
                    taskMgrFrame = this.launcher.getExecutorServiceProvider().getExecutorProgress().getStatusFrame();
                    if(taskMgrFrame != null) {
                        taskMgrFrame.setIconImage(this.launcher.getImageUtils().getLocalImage("assets/ui/icons/threadBolt.png"));
                        taskMgrFrame.setResizable(false);
                        Point parentLocation = this.launcher.getFrame().getLocationOnScreen();
                        int parentX = parentLocation.x;
                        int parentY = parentLocation.y;
                        taskMgrFrame.setLocation(parentX + this.launcher.getFrame().getWidth(), parentY);
                        taskMgrFrame.setVisible(true);
                    }
                });
            }
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

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public ServerInfoDisplayer getServerInfoDisplayer() {
        return serverInfoDisplayer;
    }

    public UserGroup getUserGroup() {
        if(auth.isAuthorised()) {
            return UserGroup.fromGroupId(Integer.parseInt((String) this.userAttributes.group));
        } else return UserGroup.GUEST;
    }

    public int getUserIntGroup(){
        return  Integer.parseInt((String) this.userAttributes.group);
    }

    public String getUserFullName(){
        return this.userAttributes.userFullName;
    }

    public String getColorScheme(){
        return this.userAttributes.colorScheme;
    }

}