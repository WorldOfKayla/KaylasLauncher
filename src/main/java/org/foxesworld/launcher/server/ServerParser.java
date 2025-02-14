package org.foxesworld.launcher.server;

import com.google.gson.Gson;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.engine.utils.HTTP.HttpParam;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ServerParser extends org.foxesworld.engine.server.ServerParser {

    @HttpParam
    private final String sysRequest = "parseServers";
    @HttpParam
    private String login = "";

    public ServerParser(Engine engine) {
        super(engine, "POST");
        this.engine = engine;
    }

    @Override
    public List<ServerAttributes> parseServers(String login) {
        this.login = login;
        CompletableFuture<List<ServerAttributes>> future = new CompletableFuture<>();
        this.sendAsync(Map.of(),
                response -> {
                    try {
                        ServerAttributes[] serversArray = new Gson().fromJson(String.valueOf(response), ServerAttributes[].class);
                        for (ServerAttributes serverAttributes : serversArray) {
                            this.serverList.add(serverAttributes);
                            serversNum++;
                        }
                        Engine.getLOGGER().debug("Loading " + serversNum + " servers for User " + login);
                        future.complete(serverList);
                    } catch (Exception e) {
                        Engine.getLOGGER().error("Error parsing server response: " + e.getMessage());
                        future.completeExceptionally(e);
                    }
                },
                error -> {
                    Engine.getLOGGER().error("Unexpected error during server parsing: " + error.getMessage());
                    future.completeExceptionally(error);
                }
        );

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            Engine.getLOGGER().error("Error completing server parsing task: " + e.getMessage());
            return List.of();
        }
    }

    public int getServersNum() {
        return serversNum;
    }
}
