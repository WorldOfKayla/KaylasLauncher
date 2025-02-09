package org.foxesworld.launcher.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.foxesworld.Launcher;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.componentAccessor.Component;
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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ServerInfoDisplayer extends ComponentsAccessor implements DropBoxListener {
    private final Launcher launcher;
    private final User user;
    private final JPanel newsPanel;
    private final ImageUtils imageUtils;
    private final GuiBuilder guiBuilder;
    @Component
    @SuppressWarnings("unused")
    private Label srvOnline,serverTitle,serverCore,serverImg;
    @Component
    @SuppressWarnings("unused")
    private TextArea serverDescLabel;

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
        String[] values = dropBox.getValues();
        BufferedImage[] icons = new BufferedImage[values.length];

        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            String iconName;

            if (value.contains("-")) {
                String[] parts = value.split("-");
                iconName = parts[parts.length - 1];
            } else {
                iconName = "Vanilla";
            }

            if (iconName != null && !iconName.isEmpty()) {
                icons[i] = this.launcher.getImageUtils().getLocalImage("assets/ui/icons/srvIcons/" + iconName.trim() + ".png");
            }
        }

        dropBox.setIcons(icons);
        this.getPanel().repaint();
    }


    @Override
    public void onScrollBoxOpen(DropBox dropBox) {
        this.getPanel().repaint();
    }

    @Override
    public void onScrollBoxClose(DropBox dropBox) {
        this.launcher.getExecutorServiceProvider().submitTask(() -> {
            if (Arrays.stream(this.user.getAuth().getUserServersArray()).count() == 1) {
                displayServerInfo(0);
            } else {
                displayServerInfo(dropBox.getSelectedIndex());
            }
            System.gc();
        }, "dropBoxClose");
        this.getPanel().repaint();
    }

    @Override
    public void onServerHover(DropBox dropBox, int index) {
        displayServerInfo(index);
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
        if (this.user.getAuth().isAuthorised()) {
            AtomicReference<ServerAttributes> thisServer = new AtomicReference<>();
            this.launcher.getExecutorServiceProvider().submitTask(() -> {
                if (user.getAuth().isAuthorised()) {
                    user.getAuth().getEngine().getPanelVisibility().displayPanel("serverInfo->true");
                    newsPanel.removeAll();
                    newsPanel.add(this.getPanel());
                    thisServer.set(user.getAuth().getUserServersAttributes().get(index));
                    updateServerInfoComponents(thisServer.get());
                    newsPanel.repaint();
                }
                String[] status = this.user.getServerInfo().pollServer(thisServer.get().getHost(), thisServer.get().getPort());
                this.srvOnline.setText(this.user.getServerInfo().genServerStatus(status));
            }, "displayServer-" + index);
        }
    }

    private void updateServerInfoComponents(ServerAttributes thisServer) {
        String[] version = thisServer.getServerVersion().split("-");
        String srvIcon = "";
        if(version[1] != null) {
            srvIcon = "assets/ui/icons/srvIcons/" + version[1] + ".png";
        } else {
            srvIcon = "assets/ui/icons/srvIcons/Vanilla.png";
        }
        this.serverTitle.setText(thisServer.getServerName() + ' ' + version[0]);
        BufferedImage image = (BufferedImage) this.imageUtils.getScaledImage(this.launcher.getImageUtils().getLocalImage(srvIcon), 32, 32);
        this.serverCore.setIcon(new ImageIcon(image));
        this.serverImg.setIcon(new ImageIcon(getServerImage(thisServer.getServerImage())));
        this.serverDescLabel.setText(thisServer.getServerDescription());
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