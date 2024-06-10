package org.foxesworld.launcher.game;


import org.foxesworld.engine.Engine;

public class AuthLib extends org.foxesworld.engine.game.AuthLib {
    private final GameLauncher gameLauncher;

    public AuthLib(GameLauncher gameLauncher){
        this.gameLauncher = gameLauncher;
    }

    @Override
    protected void loadAuthLib() {
        try {
            gameLauncher.getClassLoader().loadClass("com.mojang.authlib.Agent");
        } catch (ClassNotFoundException e2) {
            Engine.LOGGER.warn("Couldn't load AuthLib!");
            Engine.LOGGER.error(e2);
        }
    }
}