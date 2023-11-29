package org.foxesworld.engine.discord;

import club.minnced.discord.rpc.*;
import org.foxesworld.engine.Engine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Discord implements DiscordListener {

    private Engine engine;
    private final DiscordRPC lib;
    private final DiscordRichPresence presence;
    private final ExecutorService rpcExecutorService = Executors.newSingleThreadExecutor();
    public Discord(Engine engine){
     this.engine = engine;
        String applicationId = engine.getEngineData().getAppId();
     lib = DiscordRPC.INSTANCE;
        String steamId = "";
        DiscordEventHandlers handlers = new DiscordEventHandlers();
        handlers.ready = (user) -> {
            //Instance.setUserLogin(user.username);
        };
        lib.Discord_Initialize(applicationId, handlers, true, steamId);
        presence = new DiscordRichPresence();

        lib.Discord_UpdatePresence(presence);
    }

    public void discordRpcStart(String state, String details, String icon) {
        presence.startTimestamp = System.currentTimeMillis() / 1000;
        presence.details = details;
        presence.state = state;
        presence.largeImageKey = icon;
        lib.Discord_UpdatePresence(presence);

        rpcExecutorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                lib.Discord_RunCallbacks();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    lib.Discord_Shutdown();
                    break;
                }
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(rpcExecutorService::shutdownNow));
    }

    @Override
    public DiscordRPC getDiscordLib() {
        return lib;
    }

    public ExecutorService getRpcExecutorService() {
        return rpcExecutorService;
    }
}