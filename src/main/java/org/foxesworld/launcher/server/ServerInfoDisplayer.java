package org.foxesworld.launcher.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.componentAccessor.ComponentsAccessor;
import org.foxesworld.engine.gui.components.dropBox.DropBoxListener;
import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.gui.components.textArea.TextArea;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.utils.ImageUtils;
import org.foxesworld.launcher.user.User;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class ServerInfoDisplayer extends ComponentsAccessor implements DropBoxListener {
    private final User user;
    private final JPanel newsPanel;
    private final ImageUtils imageUtils;
    private final GuiBuilder guiBuilder;

    public ServerInfoDisplayer(User user) {
        super(user.getGuiBuilder(), "serverInfo", List.of(Label.class, TextArea.class));
        this.user = user;
        this.newsPanel = user.getNewsPanel();
        this.guiBuilder = user.getGuiBuilder();
        this.imageUtils = this.guiBuilder.getEngine().getImageUtils();
    }

    @Override
    public void onScrollBoxCreated(int index) {
        user.updateServer(index);
    }

    @Override
    public void onScrollBoxOpen(int index) {
        //this.displayServerInfo(index);
    }

    @Override
    public void onScrollBoxClose(int index) {
        newsPanel.removeAll();
        user.updateServer(index);
        if (this.user.getLauncher().getConfig().isLoadNews()) {
            guiBuilder.getPanelsMap().get("newsForm").add(user.getGuiBuilder().getPanelsMap().get("newsFrame"));
            guiBuilder.getPanelsMap().get("newsForm").repaint();
        }
    }

    @Override
    public void onServerHover(int index) {
        this.displayServerInfo(index);
    }

    private void displayServerInfo(int index) {
        newsPanel.removeAll();
        user.getAuth().getEngine().getPanelVisibility().displayPanel("serverInfo->true");
        newsPanel.add(guiBuilder.getPanelsMap().get("serverInfo"));
        ServerAttributes thisServer = user.getAuth().getUserServersAttributes().get(index);
        ((JLabel) this.getComponent("serverTitle")).setText(thisServer.getServerName() + ' ' + thisServer.getServerVersion());
        ((JLabel) this.getComponent("serverImg")).setIcon(new ImageIcon(getServerImage(thisServer.getServerImage())));
        TextArea textArea = ((TextArea) this.getComponent("serverDescLabel"));
        textArea.setWrapStyleWord(true);
        textArea.setText(thisServer.getServerDescription());
        //modsInfoArr(thisServer.getModsInfo());
        newsPanel.repaint();
    }

    private BufferedImage getServerImage(String url) {
        return imageUtils.getRoundedImage(imageUtils.getScaledImage(
                imageUtils.getCachedUrlImg(this.user.getLauncher().getEngineData().getBindUrl() + url, "serverImg", imageUtils.getLocalImage("assets/ui/img/noimg.jpg"))
                , 470, 260), 25);
    }

    private void modsInfoArr(String json) {
        if (!json.isEmpty()) {
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
