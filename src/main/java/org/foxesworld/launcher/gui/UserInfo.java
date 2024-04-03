package org.foxesworld.launcher.gui;

import com.google.gson.Gson;
import org.foxesworld.Launcher;
import org.foxesworld.engine.gui.ComponentsAccessor;
import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.gui.components.textfield.TextField;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.ImageUtils;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class UserInfo extends ComponentsAccessor {

    private final Launcher launcher;
    private final Map<String, Label> components = new HashMap<>();
    private final HTTPrequest POSTrequest;
    private Map<String, String> requestBody;
    private UserAttributes[] userAttributes;

    public UserInfo(Launcher launcher){
        super(launcher.getGuiBuilder(), "test");
        this.launcher = launcher;
        this.POSTrequest = launcher.getPOSTrequest();
        this.getComponents();
    }

    public void sendRequest(){
        this.setRequest();
        requestBody.put("selectValue", ((TextField) this.getComponent("userInfoLogin")).getText());
        String response = POSTrequest.send(launcher.getEngineData().getBindUrl(), requestBody);
        this.userAttributes = new Gson().fromJson(response, UserAttributes[].class);
        if(this.userAttributes.length > 0) {
            this.updateInfo();
        }
    }

    public void updateInfo(){
        BufferedImage userPic = ImageUtils.loadImageFromUrl(this.launcher.getEngineData().getBindUrl() + this.userAttributes[0].getProfilePhoto());
        this.components.get("titleText").setText(this.userAttributes[0].getRealname());
        this.components.get("statusText").setText(this.userAttributes[0].getUserStatus());
        this.components.get("userInfoHead").setIcon(new ImageIcon(ImageUtils.getRoundedImage(ImageUtils.getScaledImage(userPic, 128, 128), 50)));
    }

    private void getComponents(){
        for(JComponent component : this.launcher.getGuiBuilder().getComponentsMap().get("userInfoTable")){
            this.components.put(component.getName(), (Label) component);
        }
    }

    private void setRequest(){
        requestBody = new HashMap<>();
        requestBody.put("sysRequest", "selectUsers");
        requestBody.put("selectKey", "login");
    }

    public static class UserAttributes {
        private  String realname, profilePhoto, userStatus, land, colorScheme;

        public String getRealname() {
            return realname;
        }

        public String getProfilePhoto() {
            return profilePhoto;
        }

        public String getUserStatus() {
            return userStatus;
        }

        public String getLand() {
            return land;
        }

        public String getColorScheme() {
            return colorScheme;
        }
    }
}
