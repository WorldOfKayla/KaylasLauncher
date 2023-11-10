package org.foxesworld.launcher.user;

import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.gui.components.scrollBox.ScrollBox;
import org.foxesworld.engine.gui.components.scrollBox.ScrollBoxListener;
import org.foxesworld.engine.gui.components.serverBox.ServerBox;
import org.foxesworld.engine.locale.LanguageProvider;
import org.foxesworld.engine.utils.ImageUtils;
import org.foxesworld.engine.utils.ServerInfo;
import org.foxesworld.launcher.Auth.Auth;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User implements ScrollBoxListener {
    private final Auth auth;
    private final LanguageProvider lang;
    private final ServerInfo serverInfo;
    private final ServerBox serverBox;

    private String login, password, units, token, uuid;

    public User(Auth auth){
        this.serverInfo = auth.getEngine().getServerInfo();
        ScrollBox scrollBox = (ScrollBox) auth.getEngine().getGuiBuilder().getComponentById("serverBox");
        serverBox = (ServerBox) auth.getEngine().getGuiBuilder().getComponentById("serverStatusBox");
        scrollBox.setScrollBoxListener(this);
        this.auth = auth;
        this.lang = auth.getEngine().getLANG();
        try {
            this.setUserSpace();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    public void setUserSpace() throws MalformedURLException {
        if(this.auth.getEngine().getAuth().isAuthorised()) {
            auth.getEngine().displayPanel("authForm->false|loggedForm->true|devInfo->true");
            List<String> userLabelsIds = Arrays.asList("userHead", "userGroup");
            Map<String, Label> userLabels = getLabelsMap(userLabelsIds);
            for(Map.Entry<String, String> credentials: auth.getAuthCredentials().entrySet()){
                try {
                    Field field = User.class.getDeclaredField(credentials.getKey());
                    if(field.hashCode()!= 0) {
                        field.set(this, credentials.getValue());
                    }
                } catch (NoSuchFieldException | IllegalAccessException ignored) {}
            }

            userLabels.get("userHead").setIcon(new ImageIcon(ImageUtils.base64ToBufferedImage(this.getUserHead())));
            userLabels.get("userGroup").setText(this.auth.getEngine().getLANG().getString("group.group-"+this.auth.getAuthCredentials("group")));
        } else {
            auth.getEngine().displayPanel("loggedForm->false|newsForm->true|authForm->true");
        }
    }

    private String getUserHead() {
        Map<String, String> skinData = new HashMap<>();
        skinData.put("sysRequest", "skin");
        skinData.put("show", "head");
        skinData.put("login", this.getLogin());
        return this.auth.getEngine().getPOSTrequest().send(this.auth.getEngine().getEngineData().bindUrl, skinData);
    }

    private Map<String, Label> getLabelsMap(List<String> labeldIds){
        Map<String, Label> labelsMap = new HashMap<>();
        for(String labelId: labeldIds){
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

    public String getUuid() {
        return uuid;
    }

    @Override
    public void onScrollBoxCreated(int index) {
        this.updateServer(index);
    }

    @Override
    public void onScrollBoxOpen(int index) {
        this.auth.getEngine().getLOGGER().debug("Opened "+index);
    }

    @Override
    public void onScrollBoxClose(int index) {
        this.updateServer(index);
    }


    @Override
    public void onServerHover(int index) {
        this.auth.getEngine().getLOGGER().debug("Hover " + this.auth.getUserServersAttributes().get(index).getServerName());
    }

    private  void  updateServer(int index){
        final Thread[] serverPollThread = {new Thread()};
        try {
            serverPollThread[0].interrupt();
            serverPollThread[0] = null;
        } catch (Exception ignored) {
        }

        this.auth.getEngine().getLOGGER().info("Refreshing server state... (" + this.auth.getUserServersArray()[index] + ")");
        serverPollThread[0] = new Thread(() -> {
            serverBox.updateBox(lang.getString("server.updating"), serverInfo.genServerIcon(new String[] { null, "0", null }));
            String ip = auth.getUserServersAttributes().get(index).getHost();
            int port = auth.getUserServersAttributes().get(index).getPort();
            String[] status = serverInfo.pollServer(ip, port);
            String text = serverInfo.genServerStatus(status);
            BufferedImage img = serverInfo.genServerIcon(status);
            serverBox.updateBox(text, img);

            serverPollThread[0].interrupt();
            serverPollThread[0] = null;
            this.auth.getEngine().getLOGGER().info("Refreshing server done!");
        });
        serverPollThread[0].setName("Server poll thread");
        serverPollThread[0].start();
    }

}
