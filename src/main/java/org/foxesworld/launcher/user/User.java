package org.foxesworld.launcher.user;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.ComponentsAccessor;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.serverBox.ServerBox;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.utils.ServerInfo;
import org.foxesworld.launcher.auth.Auth;
import org.foxesworld.launcher.server.ServerInfoDisplayer;
import raven.toast.Notifications;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public class User extends org.foxesworld.engine.user.User {
    private final Auth auth;
    private final Launcher launcher;
    private final ServerInfoDisplayer serverInfoDisplayer;
    private final LanguageProvider lang;
    private final ServerInfo serverInfo;
    private final ServerBox serverBox;
    private final GuiBuilder guiBuilder;
    private final ComponentsAccessor componentsAccessor;
    private final UserAttributes userAttributes;
    private JPanel newsPanel;

    public User(Launcher launcher) {
        this.launcher = launcher;
        this.auth = launcher.getAuth();
        this.engine = launcher.getEngine();
        this.serverInfo = engine.getServerInfo();
        this.serverInfo.setServerStatusImg(this.engine.getImageUtils().getLocalImage("assets/ui/icons/status.png"));
        this.serverBox = (ServerBox) engine.getGuiBuilder().getComponentById("serverStatusBox");
        this.lang = launcher.getLANG();
        this.guiBuilder = launcher.getGuiBuilder();
        this.componentsAccessor = new ComponentsAccessor(this.guiBuilder, "userPane");
        this.userAttributes = new UserAttributes(this);

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
            this.getGuiBuilder().getNotifications().show(Notifications.Type.SUCCESS, Notifications.Location.BOTTOM_LEFT, this.launcher.getLANG().getString("auth.loggedIn") + this.getLogin());
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
        for (Map.Entry<String, String> credentials : auth.getAuthCredentials().entrySet()) {
            try {
                Field field = this.userAttributes.getClass().getDeclaredField(credentials.getKey());
                field.set(this.userAttributes, credentials.getValue());
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }
        ImageIcon icon = new ImageIcon(this.engine.getImageUtils().base64ToBufferedImage(this.getUserHead(this.getLogin())));
        ((JLabel) this.componentsAccessor.getComponentMap().get("userHead")).setIcon(icon);
        ((JLabel) this.componentsAccessor.getComponentMap().get("userGroup")).setText(this.lang.getString("group.group-" + this.auth.getAuthCredentials("group")));
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

    public GuiBuilder getGuiBuilder() {
        return guiBuilder;
    }

    public JPanel getNewsPanel() {
        return newsPanel;
    }
}
