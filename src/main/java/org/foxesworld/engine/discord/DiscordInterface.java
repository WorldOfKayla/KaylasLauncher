package org.foxesworld.engine.discord;

import club.minnced.discord.rpc.DiscordRPC;

public interface DiscordInterface {
    DiscordRPC getDiscordLib();
    void discordRpcStart(String state, String details, String icon);
}
