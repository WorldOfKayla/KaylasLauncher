package org.foxesworld.launcher.server;

import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.components.dropBox.DropBoxListener;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.utils.ImageUtils;
import org.foxesworld.launcher.user.User;

import javax.swing.*;

public class ServerInfoDisplayer implements DropBoxListener {

    private final User user;
    private final JPanel newsPanel;
    private final GuiBuilder guiBuilder;
    public ServerInfoDisplayer(User user){
        this.user = user;
        this.newsPanel = user.getNewsPanel();
        this.guiBuilder = user.getGuiBuilder();
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
        newsPanel.removeAll();
        user.getAuth().getEngine().getPanelVisibility().displayPanel("serverInfo->true");
        newsPanel.add(guiBuilder.getPanelsMap().get("serverInfo"));
        ServerAttributes thisServer = user.getAuth().getUserServersAttributes().get(index);
        guiBuilder.setLabelText("serverTitle", thisServer.getServerName() + ' ' + thisServer.getServerVersion());
        guiBuilder.setLabelIcon("serverImg", new ImageIcon(
                ImageUtils.getRoundedImage(ImageUtils.getScaledImage(
                        ImageUtils.loadImageFromUrl(
                                user.getAuth().getLauncher().getEngineData().getBindUrl() + thisServer.getServerImage()), 470, 260), 25)));
        guiBuilder.setLabelText("serverDescLabel", thisServer.getServerDescription(), true);
        newsPanel.repaint();
    }
}
