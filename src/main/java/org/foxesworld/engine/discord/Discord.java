package org.foxesworld.engine.discord;

import club.minnced.discord.rpc.*;
import org.foxesworld.engine.Engine;

public class Discord implements DiscordInterface {

    private Engine engine;
    private DiscordRPC lib;
    private DiscordRichPresence presence;
    private String applicationId;
    public Discord(Engine engine){
     this.engine = engine;
     applicationId = engine.getEngineData().appId;
     lib = DiscordRPC.INSTANCE;
        String steamId = "";
        DiscordEventHandlers handlers = new DiscordEventHandlers();
        handlers.ready = (user) -> {
            //Instance.setUserLogin(user.username);
            System.out.println(user.discriminator);
        };
        lib.Discord_Initialize(applicationId, handlers, true, steamId);
        presence = new DiscordRichPresence();

        lib.Discord_UpdatePresence(presence);
    }


    @Override
    public void discordRpcStart(String state, String details, String icon) {
        presence.startTimestamp = System.currentTimeMillis() / 1000;
        presence.details   = details;
        presence.state     = state;
        presence.largeImageKey = icon;
        lib.Discord_UpdatePresence(presence);
        Thread t = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                lib.Discord_RunCallbacks();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    lib.Discord_Shutdown();
                    break;
                }
            }
        }, "RPC-Callback-Handler");
        t.start();
    }

    @Override
    public DiscordRPC getDiscordLib() {
        return lib;
    }
}