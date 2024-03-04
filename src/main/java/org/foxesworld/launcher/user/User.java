package org.foxesworld.launcher.user;

import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.dropBox.DropBoxListener;
import org.foxesworld.engine.gui.components.serverBox.ServerBox;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.utils.ImageUtils;
import org.foxesworld.engine.utils.ServerInfo;
import org.foxesworld.launcher.auth.Auth;
import org.foxesworld.Launcher;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User implements DropBoxListener {
    private final Launcher launcher;
    private final Auth auth;
    private final LanguageProvider lang;
    private final ServerInfo serverInfo;
    private final ServerBox serverBox;
    private String login, password, units, token, uuid, colorScheme;

    public User(Launcher launcher) throws MalformedURLException {
        this.launcher = launcher;
        this.auth = launcher.getAuth();
        this.serverInfo = auth.getEngine().getServerInfo();
        DropBox dropBox = (DropBox) auth.getEngine().getGuiBuilder().getComponentById("serverBox");
        this.serverBox = (ServerBox) auth.getEngine().getGuiBuilder().getComponentById("serverStatusBox");
        dropBox.setScrollBoxListener(this);
        this.lang = auth.getEngine().getLANG();
        if (this.launcher.getAuth().isAuthorised()) {
            setUserSpace();
        } else {
            auth.getEngine().getPanelVisibility().displayPanel("loggedForm->false|newsForm->true|authForm->true");
        }
    }

    public void setUserSpace() throws MalformedURLException {
        auth.getEngine().getPanelVisibility().displayPanel("authForm->false|loggedForm->true|devInfo->true");
        Map<String, Label> userLabels = getLabelsMap(Arrays.asList("userHead", "userGroup"));
        for (Map.Entry<String, String> credentials : auth.getAuthCredentials().entrySet()) {
            try {
                Field field = User.class.getDeclaredField(credentials.getKey());
                field.set(this, credentials.getValue());
            } catch (NoSuchFieldException | IllegalAccessException ignored) {}
        }
        userLabels.get("userHead").setIcon(new ImageIcon(ImageUtils.base64ToBufferedImage(this.getUserHead())));
        userLabels.get("userGroup").setText(this.lang.getString("group.group-" + this.auth.getAuthCredentials("group")));
    }

    private String getUserHead() {
        Map<String, String> skinData = new HashMap<>();
        skinData.put("sysRequest", "skin");
        skinData.put("show", "head");
        skinData.put("login", this.getLogin());
        return this.auth.getEngine().getPOSTrequest().send(this.auth.getEngine().getEngineData().getBindUrl(), skinData);
    }

    private Map<String, Label> getLabelsMap(List<String> labelIds) {
        Map<String, Label> labelsMap = new HashMap<>();
        for (String labelId : labelIds) {
            labelsMap.put(labelId, (Label) this.auth.getEngine().getGuiBuilder().getComponentById(labelId));
        }
        return labelsMap;
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

    @Override
    public void onScrollBoxCreated(int index) {
        updateServer(index);
    }

    @Override
    public void onScrollBoxOpen(int index) {
        System.out.println("Opened " + index);
    }

    @Override
    public void onScrollBoxClose(int index) {
        updateServer(index);
    }

    @Override
    public void onServerHover(int index) {
        System.out.println("Hover " + index);
    }

    private void updateServer(int index) {
        Thread serverPollThread = new Thread(() -> {
            try {
                serverBox.updateBox(lang.getString("server.updating"), serverInfo.genServerIcon(new String[]{null, "0", null}));
                String ip = auth.getUserServersAttributes().get(index).getHost();
                int port = auth.getUserServersAttributes().get(index).getPort();
                String[] status = serverInfo.pollServer(ip, port);
                String text = serverInfo.genServerStatus(status);
                BufferedImage img = serverInfo.genServerIcon(status);
                serverBox.updateBox(text, img);
                auth.getEngine().getLOGGER().info("Refreshing Server done!");
            } catch (Exception e) {
                auth.getEngine().getLOGGER().error("Error refreshing server: " + e.getMessage());
            }
        });
        serverPollThread.setName("Server poll thread");
        serverPollThread.start();
    }
}
