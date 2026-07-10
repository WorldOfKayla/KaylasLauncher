package org.takesome.launcher.discord;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.discord.Discord;
import org.takesome.kaylasEngine.server.ServerAttributes;

import java.util.Locale;
import java.util.Objects;

/**
 * Converts launcher and game lifecycle events into precise Discord Rich Presence snapshots.
 *
 * <p>The last desired snapshot is retained so it can be replayed if GUI initialization wins the race
 * against the native Discord RPC initialization task.</p>
 */
public final class LauncherDiscordPresence {
    private final Launcher launcher;
    private final String launcherImageKey;
    private volatile PresenceSnapshot lastSnapshot;

    public LauncherDiscordPresence(Launcher launcher, String launcherImageKey) {
        this.launcher = Objects.requireNonNull(launcher, "launcher");
        this.launcherImageKey = normalizeAssetKey(launcherImageKey);
    }

    public void showLauncher(String login) {
        publish(
                "general.launcher",
                "In launcher",
                null,
                login,
                false
        );
    }

    public void showServerSelection(ServerAttributes server, String login) {
        publish(
                "discord.selecting",
                "Choosing server {server}",
                server,
                login,
                false
        );
    }

    public void showPreparing(ServerAttributes server, String login) {
        publish(
                "discord.preparing",
                "Preparing {server}",
                server,
                login,
                true
        );
    }

    public void showDownloading(ServerAttributes server, String login) {
        publish(
                "discord.downloading",
                "Downloading files for {server}",
                server,
                login,
                true
        );
    }

    public void showVerifying(ServerAttributes server, String login) {
        publish(
                "discord.verifying",
                "Verifying client for {server}",
                server,
                login,
                true
        );
    }

    public void showLaunching(ServerAttributes server, String login) {
        publish(
                "discord.launching",
                "Launching {server}",
                server,
                login,
                true
        );
    }

    public void showPlaying(ServerAttributes server, String login) {
        publish(
                "game.playing",
                "Playing on {server}",
                server,
                login,
                true
        );
    }

    public void showLaunchFailed(ServerAttributes server, String login) {
        publish(
                "discord.failed",
                "Launch failed for {server}",
                server,
                login,
                true
        );
    }

    public void showCancelled(ServerAttributes server, String login) {
        publish(
                "discord.cancelled",
                "Launch cancelled for {server}",
                server,
                login,
                true
        );
    }

    /** Replays the most recent desired state after Discord RPC becomes available. */
    public void refresh() {
        PresenceSnapshot snapshot = lastSnapshot;
        if (snapshot != null && enabled()) {
            apply(snapshot);
        }
    }

    private void publish(String localeKey,
                         String fallbackTemplate,
                         ServerAttributes server,
                         String login,
                         boolean resetTimestamp) {
        if (!enabled()) {
            return;
        }

        String serverName = serverDisplayName(server);
        PresenceSnapshot snapshot = new PresenceSnapshot(
                buildState(server, login),
                localize(localeKey, fallbackTemplate, serverName),
                server == null ? launcherImageKey : resolveServerImageKey(server),
                server == null ? launcherTitle() : serverImageText(server),
                server == null ? "" : launcherImageKey,
                server == null ? "" : launcherTitle(),
                resetTimestamp
        );
        lastSnapshot = snapshot;
        apply(snapshot);
    }

    private void apply(PresenceSnapshot snapshot) {
        Discord discord = launcher.getDiscord();
        if (discord == null || !discord.isAvailable()) {
            return;
        }
        discord.updatePresence(
                snapshot.state(),
                snapshot.details(),
                snapshot.largeImageKey(),
                snapshot.largeImageText(),
                snapshot.smallImageKey(),
                snapshot.smallImageText(),
                snapshot.resetTimestamp()
        );
    }

    private boolean enabled() {
        return launcher.getConfig() != null && launcher.getConfig().isDiscordRPC();
    }

    private String localize(String key, String fallbackTemplate, String serverName) {
        String resolved;
        try {
            resolved = launcher.getLANG().getStringWithKey(
                    key,
                    new String[]{"server"},
                    new String[]{serverName}
            );
        } catch (RuntimeException error) {
            resolved = null;
        }
        if (resolved == null || resolved.isBlank() || key.equals(resolved)) {
            resolved = fallbackTemplate.replace("{server}", serverName);
        }
        return resolved;
    }

    private String buildState(ServerAttributes server, String login) {
        StringBuilder state = new StringBuilder();
        appendPart(state, safe(login));
        if (server != null) {
            String version = safe(server.getServerVersion());
            if (!version.isEmpty()) {
                appendPart(state, "Minecraft " + version);
            }
            appendPart(state, safe(server.getCoreType()));
            String client = safe(server.getClient());
            if (!client.isEmpty() && !client.equalsIgnoreCase(server.getCoreType())) {
                appendPart(state, client);
            }
            appendPart(state, formatAddress(server));
        }
        return state.isEmpty() ? launcherTitle() : state.toString();
    }

    private void appendPart(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(" • ");
        }
        builder.append(value.trim());
    }

    private String resolveServerImageKey(ServerAttributes server) {
        String explicitKey = normalizeAssetKey(server.getDiscordImageKey());
        if (!explicitKey.isEmpty()) {
            return explicitKey;
        }

        String imageReference = safe(server.getServerImage());
        String referenceKey = prefixedAssetKey(imageReference);
        if (!referenceKey.isEmpty()) {
            return referenceKey;
        }

        String generatedKey = normalizeAssetKey(server.getServerName());
        return generatedKey.isEmpty() ? launcherImageKey : generatedKey;
    }

    private String prefixedAssetKey(String imageReference) {
        if (imageReference.isEmpty()) {
            return "";
        }
        String lower = imageReference.toLowerCase(Locale.ROOT);
        for (String prefix : new String[]{"discord:", "discord://", "asset:", "asset://"}) {
            if (lower.startsWith(prefix)) {
                return normalizeAssetKey(imageReference.substring(prefix.length()));
            }
        }
        return "";
    }

    private String formatAddress(ServerAttributes server) {
        String host = safe(server.getHost());
        if (host.isEmpty()) {
            return "";
        }
        return server.getPort() > 0 ? host + ':' + server.getPort() : host;
    }

    private String serverImageText(ServerAttributes server) {
        String name = serverDisplayName(server);
        String description = safe(server.getServerDescription());
        if (description.isEmpty()) {
            return name;
        }
        return name + " — " + description;
    }

    private String serverDisplayName(ServerAttributes server) {
        if (server == null) {
            return launcher.getAppTitle();
        }
        String name = safe(server.getServerName());
        return name.isEmpty() ? launcher.getAppTitle() : name;
    }

    private String launcherTitle() {
        String brand = launcher.getEngineData() == null
                ? launcher.getAppTitle()
                : safe(launcher.getEngineData().getLauncherBrand());
        String version = launcher.getEngineData() == null
                ? ""
                : safe(launcher.getEngineData().getLauncherVersion());
        if (brand.isEmpty()) {
            brand = launcher.getAppTitle();
        }
        return version.isEmpty() ? brand : brand + ' ' + version;
    }

    private String normalizeAssetKey(String value) {
        String normalized = safe(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.length() > 32) {
            normalized = normalized.substring(0, 32).replaceAll("_+$", "");
        }
        return normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record PresenceSnapshot(String state,
                                    String details,
                                    String largeImageKey,
                                    String largeImageText,
                                    String smallImageKey,
                                    String smallImageText,
                                    boolean resetTimestamp) {
    }
}
