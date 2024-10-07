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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public class User extends org.foxesworld.engine.user.User {
    private final Auth auth;
    private final UserServers userServers;
    private final Launcher launcher;
    private final ServerInfoDisplayer serverInfoDisplayer;
    private final LanguageProvider lang;
    private final ServerInfo serverInfo;
    private final ServerBox serverBox;
    private final GuiBuilder guiBuilder;
    private final UserAttributes userAttributes;
    private JPanel newsPanel;

    public User(Launcher launcher) {
        super(launcher.getGuiBuilder(), "userPane", List.of(Label.class));
        this.launcher = launcher;
        this.auth = launcher.getAuth();
        this.userServers = new UserServers(launcher.getGuiBuilder(), "loggedForm", List.of(ServerBox.class, DropBox.class));
        this.engine = launcher.getEngine();
        this.serverInfo = engine.getServerInfo();
        this.serverInfo.setServerStatusImg(this.engine.getImageUtils().getLocalImage("assets/ui/components/icons/status.png"));
        this.serverBox = userServers.getServerBox();
        this.lang = launcher.getLANG();
        this.guiBuilder = launcher.getGuiBuilder();
        this.userAttributes = new UserAttributes(this);

        initializeUser();
        this.serverInfoDisplayer = new ServerInfoDisplayer(this);
        setDropBoxData(this.userServers.getServerListBox());
    }

    private void initializeUser() {
        if (auth.isAuthorised()) {
            setUserSpace();
            this.newsPanel = guiBuilder.getPanelsMap().get("newsForm");
            launcher.getDiscord().setSmallImageText(this.launcher.getLANG().getString("general.launcher"));
            launcher.getDiscord().discordRpcStart(
                    lang.getStringWithKey("game.login", new String[]{"login"}, new String[]{auth.getAuthCredentials("login")}),
                    launcher.getAppTitle(),
                    "launcher"
            );
            this.getGuiBuilder().getNotification().show(Notification.Type.SUCCESS, new Rectangle(10, this.serverBox.getY() + 80, 340, 45), 3000,
                    this.launcher.getLANG().getStringWithKey("auth.loggedIn", new String[]{"login"}, new String[]{this.getLogin()}));
        } else {
            engine.getPanelVisibility().displayPanel("loggedForm->false|newsForm->true|authForm->true");
        }
    }

    private void setDropBoxData(DropBox dropBox) {
        dropBox.setValues(auth.getUserServersArray());
        dropBox.setSelectedIndex(this.auth.getLauncher().getConfig().getSelectedServer());
        dropBox.setScrollBoxListener(serverInfoDisplayer);
    }

    @Override
    protected void setUserSpace() {
        auth.getEngine().getPanelVisibility().displayPanel("authForm->false|loggedForm->true");
        for (Map.Entry<String, Object> credentials : auth.getAuthCredentials().entrySet()) {
            try {
                Field field = this.userAttributes.getClass().getDeclaredField(credentials.getKey());
                field.set(this.userAttributes, credentials.getValue());
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }

        ImageIcon icon = new ImageIcon(this.engine.getImageUtils().getRoundedImage(this.engine.getImageUtils().base64ToBufferedImage(this.getUserHead(this.getLogin())), 5));
        ((JLabel) this.getComponent("userHead")).setIcon(icon);
        ((JLabel) this.getComponent("userGroup")).setText(this.lang.getString("group.group-" + this.auth.getAuthCredentials("group")));
        engine.getGuiBuilder().getPanelsMap().get("userPane").setForeground(Color.BLUE);
        //showUserInformationWindow();
    }

    public String getLogin() {
        return this.userAttributes.login;
    }

    public String getPassword() {
        return this.userAttributes.password;
    }

    public String getUnits() {
        return this.userAttributes.units;
    }

    public String getToken() {
        return this.userAttributes.token;
    }

    public String getColorScheme() {
        return this.userAttributes.colorScheme;
    }

    public Object getUserGroup() {
        return this.userAttributes.group;
    }

    public String getUuid() {
        return this.userAttributes.uuid;
    }

    public Auth getAuth() {
        return auth;
    }

    public void updateServer(int index) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                serverBox.updateBox(lang.getString("server.updating"), serverInfo.genServerIcon(new String[]{null, "0", null}));
                String ip = auth.getUserServersAttributes().get(index).getHost();
                int port = auth.getUserServersAttributes().get(index).getPort();
                String[] status = serverInfo.pollServer(ip, port);
                String text = serverInfo.genServerStatus(status);
                BufferedImage img = serverInfo.genServerIcon(status);
                serverBox.updateBox(text, img);
            } catch (Exception e) {
                Engine.getLOGGER().error("Error refreshing server: " + e.getMessage());
            }
        });
        executor.shutdown();
    }

    private void showUserInformationWindow() {
        JFrame window = new JFrame("User Information");
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setSize(350, 200);
        window.setLocation(120, 120);

        JPanel panel = new JPanel(new GridLayout(0, 2));

        for (Map.Entry<String, Object> entry : auth.getAuthCredentials().entrySet()) {
            JLabel keyLabel = new JLabel(entry.getKey());
            keyLabel.setForeground(Color.BLACK);
            keyLabel.setFont(this.launcher.getFONTUTILS().getFont("mcfontBold", 11));
            panel.add(keyLabel);

            JTextArea valueLabel = new JTextArea(String.valueOf(entry.getValue()));
            valueLabel.setForeground(Color.GRAY);
            valueLabel.setFont(this.launcher.getFONTUTILS().getFont("mcfont", 11));
            panel.add(valueLabel);
        }

        window.add(panel);
        window.pack();
        window.setResizable(false);
        window.setVisible(true);
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
}
