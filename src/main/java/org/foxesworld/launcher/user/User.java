package org.foxesworld.launcher.user;

import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.utils.ImageUtils;
import org.foxesworld.launcher.Auth.Auth;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class User {
    private final Auth auth;

    private String login, password, units, token, uuid;

    public User(Auth auth){
        this.auth = auth;
        this.setUserSpace();
    }

    public void setUserSpace(){
        if(this.auth.getEngine().getAuth().isAuthorised()) {
            auth.getEngine().displayPanel("authForm->false|loggedForm->true|devInfo->true");
            for(Map.Entry<String, String> credentials: auth.getAuthCredentials().entrySet()){
                try {
                    Field field = User.class.getDeclaredField(credentials.getKey());
                    if(field.hashCode()!= 0) {
                        field.set(this, credentials.getValue());
                    }
                } catch (NoSuchFieldException | IllegalAccessException ignored) {}
            }

            try {
                Label headLabel = (Label) this.auth.getEngine().getGuiBuilder().getComponentById("userHead");
                headLabel.setIcon(new ImageIcon(ImageUtils.base64ToBufferedImage(this.getUserHead())));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            auth.getEngine().displayPanel("loggedForm->false|newsForm->true|authForm->true");
        }
    }

    private String getUserHead() throws MalformedURLException {
        Map<String, String> skinData = new HashMap<>();
        skinData.put("sysRequest", "skin");
        skinData.put("show", "head");
        skinData.put("login", this.getLogin());
        String imageBase64 = this.auth.getEngine().getPOSTrequest().send(this.auth.getEngine().getEngineData().bindUrl, skinData);
        return imageBase64;
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
}
