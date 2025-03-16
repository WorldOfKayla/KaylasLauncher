package org.foxesworld.launcher.server;

import com.google.gson.Gson;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.HTTP.HttpParam;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ServerParser extends HTTPrequest {

    private int serverNum;
    @HttpParam
    private final String sysRequest = "parseServers";
    @HttpParam
    private String login = "";

    public ServerParser(Engine engine) {
        super(engine, "POST");
    }

    public CompletableFuture<List<?>> parseServers(String login) {
        this.login = login;

        return this.sendAsyncCF(Map.of())
                .thenApply(response -> {
                    try {
                        ServerAttributes[] serversArray = new Gson().fromJson(String.valueOf(response), ServerAttributes[].class);

                        // Приведение типа List<?> → List<ServerAttributes>
                        List<ServerAttributes> parsedServers = List.of(serversArray);

                        serverNum = parsedServers.size();
                        Engine.getLOGGER().debug("Loaded {} servers for User {}", serverNum, login);

                        return parsedServers;
                    } catch (Exception e) {
                        Engine.getLOGGER().error("Error parsing server response: {}", e.getMessage(), e);
                        return List.of();
                    }
                })
                .exceptionally(error -> {
                    Engine.getLOGGER().error("Unexpected error during server parsing: {}", error.getMessage(), error);
                    return List.of();
                });
    }

    public int getServerNum() {
        return serverNum;
    }
}
