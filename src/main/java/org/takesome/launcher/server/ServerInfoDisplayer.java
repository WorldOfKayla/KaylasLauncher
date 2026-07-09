package org.takesome.launcher.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.takesome.Launcher;
import org.takesome.kaylasEngine.gui.GuiBuilder;
import org.takesome.kaylasEngine.gui.componentAccessor.Component;
import org.takesome.kaylasEngine.gui.componentAccessor.ComponentsAccessor;
import org.takesome.kaylasEngine.gui.components.combobox.Combobox;
import org.takesome.kaylasEngine.gui.components.combobox.ComboboxListener;
import org.takesome.kaylasEngine.gui.components.label.Label;
import org.takesome.kaylasEngine.gui.components.textArea.TextArea;
import org.takesome.kaylasEngine.server.ServerAttributes;
import org.takesome.launcher.user.User;
import org.takesome.kaylasEngine.utils.DataInjector;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

import static org.takesome.launcher.auth.AuthStatus.UNAUTHORISED;

/**
 * Displays selected Minecraft server metadata and status.
 */
public class ServerInfoDisplayer extends ComponentsAccessor implements ComboboxListener {

    private final Launcher launcher;
    private final User user;
    private final JPanel newsPanel;
    private final GuiBuilder guiBuilder;

    @Component
    @SuppressWarnings("unused")
    private Label srvOnline, serverTitle, serverImg;

    @Component
    @SuppressWarnings("unused")
    private TextArea serverDescLabel;

    public ServerInfoDisplayer(User user) {
        super(user.getGuiBuilder(), "serverInfo", List.of(Label.class, TextArea.class));
        this.user = user;
        this.launcher = user.getLauncher();
        this.newsPanel = user.getNewsPanel();
        this.guiBuilder = user.getGuiBuilder();
        SwingUtilities.invokeLater(() -> displayServerInfo(this.launcher.getConfig().getSelectedServer()));
    }

    @Override
    public void onComboboxCreated(Combobox combobox) {
        combobox.repaint();
    }

    @Override
    public void onComboboxOpen(Combobox combobox) {
        this.getPanel().repaint();
    }

    @Override
    public void onComboboxClose(Combobox combobox) {
        this.launcher.getExecutorServiceProvider().submitTask(() -> {
            if (Arrays.stream(this.user.getAuth().getUserDataLoader().getUserServersArray()).count() == 1) {
                displayServerInfo(0);
            } else {
                displayServerInfo(combobox.getSelectedIndex());
            }
        }, "comboboxClose");
        this.getPanel().repaint();
    }

    @Override
    public void onComboboxHover(Combobox combobox, int index) {
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
        SwingUtilities.invokeLater(() -> {
            user.getAuth().getEngine().getPanelVisibility().displayPanel("serverInfo->true");
            newsPanel.removeAll();
            newsPanel.add(this.getPanel());
            updateServerInfoComponents(server);
            newsPanel.repaint();
        });

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
        String serverName = thisServer.getServerName() == null ? "" : thisServer.getServerName();
        String serverVersion = thisServer.getServerVersion() == null ? "" : thisServer.getServerVersion();
        this.serverTitle.setText((serverName + ' ' + serverVersion).trim());
        this.serverDescLabel.setText(thisServer.getServerDescription());

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
