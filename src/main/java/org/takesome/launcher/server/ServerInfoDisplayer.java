package org.takesome.launcher.server;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.gui.componentAccessor.Component;
import org.takesome.kaylasEngine.gui.componentAccessor.ComponentsAccessor;
import org.takesome.kaylasEngine.gui.components.combobox.Combobox;
import org.takesome.kaylasEngine.gui.components.combobox.ComboboxListener;
import org.takesome.kaylasEngine.gui.components.label.Label;
import org.takesome.kaylasEngine.gui.components.textArea.TextArea;
import org.takesome.kaylasEngine.server.ServerAttributes;
import org.takesome.launcher.backend.LauncherBackendClient;
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
    private final AtomicInteger statusRequestSequence = new AtomicInteger();
    private final ServerStatusText statusText;

    @Component
    @SuppressWarnings("unused")
    private Label srvOnline, serverTitle, serverImg, serverRuntime, serverEndpoint;

    @Component
    @SuppressWarnings("unused")
    private TextArea serverDescLabel;

    public ServerInfoDisplayer(User user) {
        super(user.getGuiBuilder(), "serverInfo", List.of(Label.class, TextArea.class));
        this.user = user;
        this.launcher = user.getLauncher();
        this.newsPanel = user.getNewsPanel();
        this.statusText = new ServerStatusText(launcher.getLANG());
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
        int requestSequence = statusRequestSequence.incrementAndGet();
        SwingUtilities.invokeLater(() -> {
            if (requestSequence != statusRequestSequence.get()) {
                return;
            }
            user.getAuth().getEngine().getPanelVisibility().displayPanel("serverInfo->true");
            newsPanel.removeAll();
            newsPanel.add(this.getPanel());
            updateServerInfoComponents(server, requestSequence);
            newsPanel.revalidate();
            newsPanel.repaint();
        });
        requestBackendServerStatus(server, requestSequence);
    }

    private void updateServerInfoComponents(ServerAttributes server, int requestSequence) {
        ServerStatusText.Summary summary = statusText.summarize(server);
        serverTitle.setText(summary.title());
        serverDescLabel.setText(summary.description());
        serverRuntime.setText(summary.runtime());
        serverEndpoint.setText(summary.endpoint());
        srvOnline.setText(statusText.metadataStatus(server));
        serverImg.setIcon(null);

        ServerImageLoader imageLoader = new ServerImageLoader(user, server.getServerImage());
        imageLoader.loadServerImage(image -> SwingUtilities.invokeLater(() -> {
            if (requestSequence == statusRequestSequence.get()) {
                serverImg.setIcon(new ImageIcon(image));
            }
        }));
    }

    private void requestBackendServerStatus(ServerAttributes server, int requestSequence) {
        LauncherBackendClient backendClient = launcher.getBackendClient();
        if (backendClient == null || !backendClient.isBound()) {
            Launcher.LOGGER.debug("Backend is not bound; server status remains metadata-only for {}:{}.",
                    server.getHost(), server.getPort());
            updateStatusLabel(requestSequence, statusText.metadataStatus(server));
            return;
        }

        updateStatusLabel(requestSequence, statusText.pending());
        backendClient.fetchServerStatus(server).whenComplete((status, error) -> {
            if (requestSequence != statusRequestSequence.get()) {
                return;
            }
            if (error != null) {
                Launcher.LOGGER.warn("Unable to load backend server status for {}:{}: {}",
                        server.getHost(), server.getPort(), error.getMessage());
                updateStatusLabel(requestSequence, statusText.unavailable(server));
                return;
            }
            updateStatusLabel(requestSequence, statusText.status(status, server));
        });
    }

    private void updateStatusLabel(int requestSequence, String text) {
        SwingUtilities.invokeLater(() -> {
            if (requestSequence == statusRequestSequence.get()) {
                srvOnline.setText(text);
            }
        });
    }

}
