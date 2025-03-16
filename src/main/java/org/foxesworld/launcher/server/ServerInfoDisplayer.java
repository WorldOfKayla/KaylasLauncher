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
import org.foxesworld.engine.gui.components.label.Label;
import org.foxesworld.engine.gui.components.textArea.TextArea;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.utils.ImageUtils;
import org.foxesworld.launcher.user.User;
import org.foxesworld.engine.utils.DataInjector;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import static org.foxesworld.launcher.auth.AuthStatus.UNAUTHORISED;

/**
 * ServerInfoDisplayer is responsible for displaying server information including server images.
 * <p>
 * This class uses the ServerImageLoader to asynchronously download and cache the server image.
 * </p>
 */
public class ServerInfoDisplayer extends ComponentsAccessor implements DropBoxListener {

    private final Launcher launcher;
    private final User user;
    private final JPanel newsPanel;
    private final ImageUtils imageUtils;
    private final GuiBuilder guiBuilder;

    @Component
    @SuppressWarnings("unused")
    private Label srvOnline, serverTitle, serverCore, serverImg;

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
        SwingUtilities.invokeLater(() -> displayServerInfo(this.launcher.getConfig().getSelectedServer()));
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
            this.getPanel().repaint();
        }

        dropBox.setIcons(icons);
    }

    @Override
    public void onScrollBoxOpen(DropBox dropBox) {
        this.getPanel().repaint();
    }

    @Override
    public void onScrollBoxClose(DropBox dropBox) {
        this.launcher.getExecutorServiceProvider().submitTask(() -> {
            if (Arrays.stream(this.user.getAuth().getUserDataLoader().getUserServersArray()).count() == 1) {
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

    public void displayServerInfo(int index) {
        if (this.user.getAuth().getAuthStatus() == UNAUTHORISED) {
            Launcher.LOGGER.warn("User is not authorised. Cannot display server info.");
            return;
        }
        var servers = this.user.getAuth().getUserDataLoader().getUserServersAttributes();
        if (servers == null || servers.isEmpty()) {
            Launcher.LOGGER.warn("User servers attributes are not available.");
            return;
        }
        if (index < 0 || index >= servers.size()) {
            Launcher.LOGGER.warn("Index {} is out of bounds for servers list.", index);
            return;
        }
        ServerAttributes server = servers.get(index);

        // Update UI components on the EDT.
        SwingUtilities.invokeLater(() -> {
            user.getAuth().getEngine().getPanelVisibility().displayPanel("serverInfo->true");
            newsPanel.removeAll();
            newsPanel.add(this.getPanel());
            updateServerInfoComponents(server);
            newsPanel.repaint();
        });

        // Create a DataInjector for obtaining server status.
        DataInjector<String> statusInjector = new DataInjector<>();
        statusInjector.addErrorListener(e -> Launcher.LOGGER.error("Error in statusInjector", e));
        statusInjector.addListener(serverStatus -> SwingUtilities.invokeLater(() -> srvOnline.setText(serverStatus)));

        launcher.getExecutorServiceProvider().submitTask(() -> {
            String[] status = user.getServerInfo().pollServer(server.getHost(), server.getPort());
            String serverStatus = user.getServerInfo().genServerStatus(status);
            statusInjector.setContent(serverStatus);
        }, "displayServer-" + index);
    }

    private void updateServerInfoComponents(ServerAttributes thisServer) {
        String[] version = thisServer.getServerVersion().split("-");
        String srvIcon;
        if (version.length > 1 && version[1] != null && !version[1].isEmpty()) {
            srvIcon = "assets/ui/icons/srvIcons/" + version[1] + ".png";
        } else {
            srvIcon = "assets/ui/icons/srvIcons/Vanilla.png";
        }
        this.serverTitle.setText(thisServer.getServerName() + ' ' + version[0]);
        BufferedImage iconImage = (BufferedImage) imageUtils.getScaledImage(launcher.getImageUtils().getLocalImage(srvIcon), 32, 32);
        this.serverCore.setIcon(new ImageIcon(iconImage));
        this.serverDescLabel.setText(thisServer.getServerDescription());

        // Use ServerImageLoader to load the server image asynchronously.
        ServerImageLoader imageLoader = new ServerImageLoader(user, thisServer.getServerImage());
        imageLoader.loadServerImage(image -> SwingUtilities.invokeLater(() ->
                serverImg.setIcon(new ImageIcon(image))
        ));
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
