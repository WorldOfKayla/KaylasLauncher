package org.foxesworld.launcher.user;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.ComponentsAccessor;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.serverBox.ServerBox;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.utils.ImageUtils;
import org.foxesworld.engine.utils.ServerInfo;
import org.foxesworld.launcher.auth.Auth;
import org.foxesworld.launcher.auth.Balance;
import org.foxesworld.launcher.server.ServerInfoDisplayer;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class User extends org.foxesworld.engine.user.User {
    private final Auth auth;
    private final Launcher launcher;
    private Balance userBalance;
    private final ServerInfoDisplayer serverInfoDisplayer;
    private final LanguageProvider lang;
    private final ServerInfo serverInfo;
    private final ServerBox serverBox;
    private final GuiBuilder guiBuilder;
    private String login, password, units, token, uuid, colorScheme;
    private final ComponentsAccessor componentsAccessor;
    private JPanel newsPanel;

    public User(Launcher launcher) {
        this.launcher = launcher;
        this.auth = launcher.getAuth();
        this.engine = launcher.getEngine();
        this.serverInfo = engine.getServerInfo();
        this.serverInfo.setServerStatusImg(ImageUtils.getLocalImage("assets/ui/icons/status.png"));
        this.serverBox = (ServerBox) engine.getGuiBuilder().getComponentById("serverStatusBox");
        this.lang = launcher.getLANG();
        this.guiBuilder = launcher.getGuiBuilder();
        this.componentsAccessor = new ComponentsAccessor(this.guiBuilder, "userPane");

        initializeUser();
        this.serverInfoDisplayer = new ServerInfoDisplayer(this);
        setDropBoxData((DropBox) engine.getGuiBuilder().getComponentById("serverBox"));
    }

    private void initializeUser() {
        if (auth.isAuthorised()) {
            setUserSpace();
            this.newsPanel = guiBuilder.getPanelsMap().get("newsForm");
            launcher.getDiscord().discordRpcStart(
                    lang.getString("game.login") + auth.getAuthCredentials("login"),
                    launcher.getAppTitle(),
                    "aiden"
            );
        } else {
            engine.getPanelVisibility().displayPanel("loggedForm->false|newsForm->true|authForm->true");
        }
    }

    private void setDropBoxData(DropBox dropBox) {
        dropBox.setValues(auth.getUserServersArray());
        dropBox.setSelectedIndex(launcher.getConfig().getSelectedServer());
        dropBox.setScrollBoxListener(serverInfoDisplayer);
    }

    @Override
    protected void setUserSpace() {
        engine.getPanelVisibility().displayPanel("authForm->false|loggedForm->true");
        populateUserCredentials();
        setUserHeadIcon();
        setUserGroupLabel();
    }

    private void populateUserCredentials() {
        for (Map.Entry<String, String> credentials : auth.getAuthCredentials().entrySet()) {
            try {
                Field field = User.class.getDeclaredField(credentials.getKey());
                field.set(this, credentials.getValue());
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }
    }

    private void setUserHeadIcon() {
        BufferedImage userHead = ImageUtils.base64ToBufferedImage(getUserHead(getLogin()));
        ImageIcon icon = new ImageIcon(userHead);
        JLabel userHeadLabel = (JLabel) componentsAccessor.getComponentMap().get("userHead");
        userHeadLabel.setIcon(icon);
    }

    private void setUserGroupLabel() {
        String userGroupKey = "group.group-" + auth.getAuthCredentials("group");
        String userGroup = lang.getString(userGroupKey);
        JLabel userGroupLabel = (JLabel) componentsAccessor.getComponentMap().get("userGroup");
        userGroupLabel.setText(userGroup);
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getUnits() {
        return units;
    }

    public String getToken() {
        return token;
    }

    public String getColorScheme() {
        return colorScheme;
    }

    public String getUuid() {
        return uuid;
    }

    public Auth getAuth() {
        return auth;
    }

    public void updateServer(int index) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                updateServerBox(index);
            } catch (Exception e) {
                Engine.getLOGGER().error("Error refreshing server: " + e.getMessage());
            }
        });
        executor.shutdown();
    }

    private void updateServerBox(int index) {
        serverBox.updateBox(lang.getString("server.updating"), serverInfo.genServerIcon(new String[]{null, "0", null}));
        String ip = auth.getUserServersAttributes().get(index).getHost();
        int port = auth.getUserServersAttributes().get(index).getPort();
        String[] status = serverInfo.pollServer(ip, port);
        String text = serverInfo.genServerStatus(status);
        BufferedImage img = serverInfo.genServerIcon(status);
        serverBox.updateBox(text, img);
    }

    public GuiBuilder getGuiBuilder() {
        return guiBuilder;
    }

    public JPanel getNewsPanel() {
        return newsPanel;
    }

    public void setUserBalance(Balance userBalance) {
        this.userBalance = userBalance;
    }
}
