package org.takesome.launcher.game;


import org.takesome.kaylasEngine.Engine;

public class AuthLib extends org.takesome.kaylasEngine.game.AuthLib {
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