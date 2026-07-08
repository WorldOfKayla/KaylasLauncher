package org.takesome.launcher.server;

/**
 * Legacy HTTP server parser was removed.
 * Server lists are loaded through LauncherBackendClient over WebSocket: SERVERS_REQUEST -> SERVERS.
 */
@Deprecated(forRemoval = true, since = "2.0.0-Anderson")
final class ServerParser {
    private ServerParser() {
    }
}
