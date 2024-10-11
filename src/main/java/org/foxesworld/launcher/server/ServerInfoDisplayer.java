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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerInfoDisplayer extends ComponentsAccessor implements DropBoxListener {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
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
    }

    @Override
    public void onScrollBoxClose(int index) {
        executorService.submit(() -> {
            user.updateServer(index);
            if (user.getLauncher().getConfig().isLoadNews()) {
                newsPanel.removeAll();
                newsPanel.repaint();
                addNewsFrameToPanel();
            }
        });
    }

    @Override
    public void onServerHover(int index) {
        displayServerInfo(index);
    }

    private void clearNewsPanel() {
        executorService.submit(() -> {
            newsPanel.removeAll();
            newsPanel.repaint();
        });
    }

    private void addNewsFrameToPanel() {
        JPanel newsFrame = guiBuilder.getPanelsMap().get("newsFrame");
        if (newsFrame != null) {
            guiBuilder.getPanelsMap().get("newsForm").add(newsFrame);
            guiBuilder.getPanelsMap().get("newsForm").repaint();
        }
    }

    private void displayServerInfo(int index) {
        clearNewsPanel();
        user.getAuth().getEngine().getPanelVisibility().displayPanel("serverInfo->true");
        newsPanel.add(this.getPanel());

        ServerAttributes thisServer = user.getAuth().getUserServersAttributes().get(index);
        updateServerInfoComponents(thisServer);
        newsPanel.repaint();
    }

    private void updateServerInfoComponents(ServerAttributes thisServer) {
        ((JLabel) getComponent("serverTitle")).setText(thisServer.getServerName() + ' ' + thisServer.getServerVersion());
        ((JLabel) getComponent("serverImg")).setIcon(new ImageIcon(getServerImage(thisServer.getServerImage())));
        ((TextArea) getComponent("serverDescLabel")).setText(thisServer.getServerDescription());
    }

    private BufferedImage getServerImage(String url) {
        return imageUtils.getRoundedImage(
                imageUtils.getScaledImage(
                        imageUtils.getCachedUrlImg(
                                user.getLauncher().getEngineData().getBindUrl() + url,
                                "serverImg",
                                imageUtils.getLocalImage("assets/ui/img/noimg.jpg")),
                        470, 260),
                25);
    }

    private void modsInfoArr(String json) {
        if (json != null && !json.isEmpty()) {
            JsonArray jsonArray = new Gson().fromJson(json, JsonArray.class); // Using a utility method to parse JSON
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
                String modName = jsonObject.get("modName").getAsString();
                String modPicture = jsonObject.get("modPicture").getAsString();
                String modDesc = jsonObject.get("modDesc").getAsString();

                // Here we can process or display mod information as needed
                System.out.printf("Mod Name: %s, Description: %s\n", modName, modDesc);
            }
        }
    }
}