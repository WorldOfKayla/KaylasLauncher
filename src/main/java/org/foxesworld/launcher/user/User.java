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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final ExecutorService executor;


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
        this.executor = Executors.newCachedThreadPool();

        initializeUser();
        setDropBoxData(userServers.getServerListBox());
    }

    private void initializeUser() {
        if (auth.isAuthorised()) {
           this.setUserSpace();
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
        setUserHeadIcon();
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

    private void setUserHeadIcon() {
        BufferedImage userHeadImage = engine.getImageUtils().base64ToBufferedImage(getUserHead(getLogin()));
        ImageIcon icon = new ImageIcon(engine.getImageUtils().getRoundedImage(userHeadImage, 5));
        SwingUtilities.invokeLater(() -> ((JLabel) getComponent("userHead")).setIcon(icon));
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

    public String getColorScheme() {
        return userAttributes.colorScheme;
    }

    public Object getUserGroup() {
        return userAttributes.group;
    }

    public String getUuid() {
        return userAttributes.uuid;
    }

    public Auth getAuth() {
        return auth;
    }

    public void updateServer(int index) {
        executor.submit(() -> {
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

    private void showUserInformationWindow() {
        JFrame window = createUserInfoWindow();
        window.setVisible(true);
    }

    private JFrame createUserInfoWindow() {
        JFrame window = new JFrame("User Information");
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setSize(350, 200);
        window.setLocation(120, 120);

        JPanel panel = new JPanel(new GridLayout(0, 2));
        auth.getAuthCredentials().forEach((key, value) -> addUserInfoRow(panel, key, value));

        window.add(panel);
        window.pack();
        window.setResizable(false);
        return window;
    }

    private void addUserInfoRow(JPanel panel, String key, Object value) {
        JLabel keyLabel = new JLabel(key);
        keyLabel.setForeground(Color.BLACK);
        keyLabel.setFont(launcher.getFONTUTILS().getFont("mcfontBold", 11));
        panel.add(keyLabel);

        JTextArea valueLabel = new JTextArea(String.valueOf(value));
        valueLabel.setForeground(Color.GRAY);
        valueLabel.setFont(launcher.getFONTUTILS().getFont("mcfont", 11));
        valueLabel.setEditable(false);
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
    public void shutdown() {
        executor.shutdownNow();
    }
}
