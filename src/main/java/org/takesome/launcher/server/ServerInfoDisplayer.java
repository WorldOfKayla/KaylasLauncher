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
import org.takesome.launcher.backend.LauncherBackendClient;
import org.takesome.launcher.backend.LauncherServerStatus;
import org.takesome.launcher.user.User;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.takesome.launcher.auth.AuthStatus.UNAUTHORISED;

/**
 * Displays selected backend-managed Minecraft server metadata.
 */
public class ServerInfoDisplayer extends ComponentsAccessor implements ComboboxListener {

    private final Launcher launcher;
    private final User user;
    private final JPanel newsPanel;
    private final GuiBuilder guiBuilder;
    private final AtomicInteger statusRequestSequence = new AtomicInteger();

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
            int selectedIndex = Arrays.stream(
                    this.user.getAuth().getUserDataLoader().getUserServersArray()
            ).count() == 1 ? 0 : combobox.getSelectedIndex();
            displayServerInfo(selectedIndex);

            List<ServerAttributes> servers = this.user.getAuth()
                    .getUserDataLoader()
                    .getUserServersAttributes();
            if (servers != null && selectedIndex >= 0 && selectedIndex < servers.size()) {
                launcher.getDiscordPresence().showServerSelection(
                        servers.get(selectedIndex),
                        user.getLogin()
                );
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
        requestBackendServerStatus(server);
    }

    private void updateServerInfoComponents(ServerAttributes thisServer) {
        String serverName = safe(thisServer.getServerName());
        String serverVersion = safe(thisServer.getServerVersion());
        this.serverTitle.setText((serverName + ' ' + serverVersion).trim());
        this.serverDescLabel.setText(safe(thisServer.getServerDescription()));
        this.srvOnline.setText(formatServerMetadata(thisServer));

        ServerImageLoader imageLoader = new ServerImageLoader(user, thisServer.getServerImage());
        imageLoader.loadServerImage(image -> SwingUtilities.invokeLater(() ->
                serverImg.setIcon(new ImageIcon(image))
        ));
    }

    private void requestBackendServerStatus(ServerAttributes server) {
        int requestSequence = statusRequestSequence.incrementAndGet();
        LauncherBackendClient backendClient = launcher.getBackendClient();
        if (backendClient == null || !backendClient.isBound()) {
            Launcher.LOGGER.debug("Backend is not bound; server status remains metadata-only for {}:{}.",
                    server.getHost(), server.getPort());
            return;
        }

        backendClient.fetchServerStatus(server).whenComplete((status, error) -> {
            if (requestSequence != statusRequestSequence.get()) {
                return;
            }
            if (error != null) {
                Launcher.LOGGER.warn("Unable to load backend server status for {}:{}: {}",
                        server.getHost(), server.getPort(), error.getMessage());
                return;
            }
            SwingUtilities.invokeLater(() -> srvOnline.setText(formatServerStatus(status, server)));
        });
    }

    private String formatServerStatus(LauncherServerStatus status, ServerAttributes fallback) {
        if (status == null) {
            return formatServerMetadata(fallback);
        }
        if (status.isOnline()) {
            return formatOnlineStatus(status);
        }
        String offline = launcher.getLANG().getString("server.serverOff");
        if (!"server.serverOff".equals(offline)) {
            return offline;
        }
        String message = safe(status.getMessage());
        return message.isEmpty() ? "Server off" : message;
    }

    private String formatOnlineStatus(LauncherServerStatus status) {
        if (status.getPlayersOnline() >= 0 && status.getPlayersMax() >= 0) {
            String template = launcher.getLANG().getString("server.serverOn");
            if (!"server.serverOn".equals(template)) {
                return template
                        .replace("%%", String.valueOf(status.getPlayersOnline()))
                        .replace("##", String.valueOf(status.getPlayersMax()));
            }
            return status.getPlayersOnline() + " / " + status.getPlayersMax();
        }
        String message = safe(status.getMessage());
        return message.isEmpty() ? "Online" : message;
    }

    private String formatServerMetadata(ServerAttributes server) {
        String client = safe(server.getClient());
        String address = formatAddress(server);
        if (!client.isEmpty() && !address.isEmpty()) {
            return client + " | " + address;
        }
        if (!client.isEmpty()) {
            return client;
        }
        if (!address.isEmpty()) {
            return address;
        }
        return "Backend managed";
    }

    private String formatAddress(ServerAttributes server) {
        String host = safe(server.getHost());
        int port = server.getPort();
        if (host.isEmpty()) {
            return port > 0 ? String.valueOf(port) : "";
        }
        return port > 0 ? host + ':' + port : host;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
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
