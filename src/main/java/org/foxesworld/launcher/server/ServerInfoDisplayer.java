package org.foxesworld.launcher.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.foxesworld.Launcher;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.componentAccessor.ComponentsAccessor;
import org.foxesworld.engine.gui.components.dropBox.DropBox;
import org.foxesworld.engine.gui.components.dropBox.DropBoxListener;
import org.foxesworld.engine.gui.components.dropBox.State;
import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.gui.components.textArea.TextArea;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.utils.ImageUtils;
import org.foxesworld.launcher.user.User;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public class ServerInfoDisplayer extends ComponentsAccessor implements DropBoxListener {
    private final Launcher launcher;
    private final User user;
    private final JPanel newsPanel;
    private final ImageUtils imageUtils;
    private final GuiBuilder guiBuilder;

    public ServerInfoDisplayer(User user) {
        super(user.getGuiBuilder(), "serverInfo", List.of(Label.class, TextArea.class));
        this.user = user;
        this.launcher = user.getLauncher();
        this.newsPanel = user.getNewsPanel();
        this.guiBuilder = user.getGuiBuilder();
        this.imageUtils = this.guiBuilder.getEngine().getImageUtils();
    }

    @Override
    public void onScrollBoxCreated(DropBox dropBox) {
        user.updateServer(dropBox.getSelectedIndex());
    }

    @Override
    public void onScrollBoxOpen(DropBox dropBox) {

    }

    @Override
    public void onScrollBoxClose(DropBox dropBox) {
        if(Arrays.stream(this.user.getAuth().getUserServersArray()).count() == 1) {
            displayServerInfo(0);
        }
        this.launcher.getExecutorServiceProvider().submitTask(() -> user.updateServer(dropBox.getSelectedIndex()), "dropBoxClose");
        if (dropBox.getState().equals(State.CLOSED)) {
            if (user.getLauncher().getConfig().isLoadNews()) {
                newsPanel.removeAll();
                addNewsFrameToPanel();
            }
        }

    }

    @Override
    public void onServerHover(DropBox dropBox, int index) {
        displayServerInfo(index);
        //System.gc();
    }

    private void clearNewsPanel() {
        this.launcher.getExecutorServiceProvider().submitTask(() -> {
            newsPanel.removeAll();
            newsPanel.repaint();
        }, "clearNewsPanel");
    }

    private void addNewsFrameToPanel() {
        JPanel newsFrame = guiBuilder.getPanelsMap().get("newsFrame");
        if (newsFrame != null) {
            guiBuilder.getPanelsMap().get("newsForm").add(newsFrame);
            guiBuilder.getPanelsMap().get("newsForm").repaint();
        }
    }

    public void displayServerInfo(int index) {
        if(user.getAuth().isAuthorised()) {
            user.getAuth().getEngine().getPanelVisibility().displayPanel("serverInfo->true");
            newsPanel.removeAll();
            newsPanel.add(this.getPanel());
            ServerAttributes thisServer = user.getAuth().getUserServersAttributes().get(index);
            updateServerInfoComponents(thisServer);
            newsPanel.repaint();
        }
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
            JsonArray jsonArray = new Gson().fromJson(json, JsonArray.class);
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
                String modName = jsonObject.get("modName").getAsString();
                String modPicture = jsonObject.get("modPicture").getAsString();
                String modDesc = jsonObject.get("modDesc").getAsString();

                System.out.printf("Mod Name: %s, Description: %s\n", modName, modDesc);
            }
        }
    }
}