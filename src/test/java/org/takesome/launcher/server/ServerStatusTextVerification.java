package org.takesome.launcher.server;

import com.google.gson.Gson;
import org.takesome.kaylasEngine.server.ServerAttributes;
import org.takesome.launcher.backend.LauncherServerStatus;

import java.util.Map;

/** Executable verification for the server-card presentation model. */
public final class ServerStatusTextVerification {
    private static final Gson GSON = new Gson();

    private ServerStatusTextVerification() {
    }

    public static void main(String[] args) {
        Map<String, String> translations = Map.of(
                "server.updating", "Updating...",
                "server.serverOn", "%% of ##",
                "server.serverOff", "Offline",
                "server.serverErr", "Unavailable"
        );
        ServerStatusText text = new ServerStatusText(
                key -> translations.getOrDefault(key, key)
        );
        ServerAttributes server = GSON.fromJson(
                """
                {
                  "serverName": "Kinetic",
                  "serverVersion": "1.20.1",
                  "jreVersion": "Java 17",
                  "coreType": "Fabric",
                  "client": "Fabric",
                  "host": "play.example.test",
                  "port": 25565,
                  "serverDescription": "A backend-managed test server."
                }
                """,
                ServerAttributes.class
        );

        ServerStatusText.Summary summary = text.summarize(server);
        require("Kinetic 1.20.1".equals(summary.title()), "Unexpected server title");
        require("Fabric | Java 17".equals(summary.runtime()), "Runtime details were not normalized");
        require("play.example.test:25565".equals(summary.endpoint()), "Endpoint was not formatted");
        require("A backend-managed test server.".equals(summary.description()), "Description was lost");
        require("Updating...".equals(text.pending()), "Pending status was not localized");
        require("Fabric".equals(text.metadataStatus(server)), "Metadata fallback is incorrect");
        require("Unavailable | Fabric".equals(text.unavailable(server)), "Unavailable status is incorrect");

        LauncherServerStatus online = new LauncherServerStatus();
        online.setOnline(true);
        online.setPlayersOnline(12);
        online.setPlayersMax(50);
        online.setLatencyMs(42);
        require("12 of 50 | 42 ms".equals(text.status(online, server)),
                "Online status is incorrect");

        LauncherServerStatus offline = new LauncherServerStatus();
        offline.setOnline(false);
        require("Offline".equals(text.status(offline, server)), "Offline status is incorrect");

        System.out.println("Launcher server presentation verification passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
