package org.foxesworld.launcher.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.ComponentsAccessor;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.components.dropBox.DropBoxListener;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.utils.ImageUtils;
import org.foxesworld.launcher.user.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class ServerInfoDisplayer implements DropBoxListener {
    private final User user;
    private final JPanel newsPanel;
    private final GuiBuilder guiBuilder;
    private final ComponentsAccessor componentsAccessor;
    public ServerInfoDisplayer(User user){
        this.user = user;
        this.newsPanel = user.getNewsPanel();
        this.guiBuilder = user.getGuiBuilder();
        this.componentsAccessor = new ComponentsAccessor(this.guiBuilder, "serverInfo");
    }
    @Override
    public void onScrollBoxCreated(int index) {
        user.updateServer(index);
    }

    @Override
    public void onScrollBoxOpen(int index) {
        this.displayServerInfo(index);
    }

    @Override
    public void onScrollBoxClose(int index) {
        newsPanel.removeAll();
        user.updateServer(index);
        guiBuilder.getPanelsMap().get("newsForm").add(user.getGuiBuilder().getPanelsMap().get("newsFrame"));
       guiBuilder.getPanelsMap().get("newsForm").repaint();
    }

    @Override
    public void onServerHover(int index) {
        this.displayServerInfo(index);
    }

    private void displayServerInfo(int index){
        BufferedImage serverImg;
        newsPanel.removeAll();
        user.getAuth().getEngine().getPanelVisibility().displayPanel("serverInfo->true");
        newsPanel.add(guiBuilder.getPanelsMap().get("serverInfo"));
        ServerAttributes thisServer = user.getAuth().getUserServersAttributes().get(index);
        ((JLabel) componentsAccessor.getComponentMap().get("serverTitle")).setText(thisServer.getServerName() + ' ' + thisServer.getServerVersion());
        ((JLabel) componentsAccessor.getComponentMap().get("serverImg")).setIcon(new ImageIcon(ImageUtils.getRoundedImage(ImageUtils.getScaledImage(getServerImage(thisServer.getServerImage()), 470, 260), 25)));
        ((JLabel) guiBuilder.getComponentById("serverDescLabel")).setText("<html>"+thisServer.getServerDescription() + "</html>");
        //modsInfoArr(thisServer.getModsInfo());
        newsPanel.repaint();
    }

    private BufferedImage getServerImage(String url){
       return ImageUtils.getCachedUrlImg(
              user.getAuth().getLauncher().getEngineData().getBindUrl() + url,
               "serverImg",
               ImageUtils.getLocalImage("assets/ui/img/noimg.jpg"));
    }

    private void modsInfoArr(String json){
        if(!json.isEmpty()) {
            Gson gson = new Gson();
            JsonArray jsonArray = gson.fromJson(json, JsonArray.class);

            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
                String modName = jsonObject.get("modName").getAsString();
                String modPicture = jsonObject.get("modPicture").getAsString();
                String modDesc = jsonObject.get("modDesc").getAsString();

                System.out.println("Mod Name: " + modName);
            }
        }
    }

}
