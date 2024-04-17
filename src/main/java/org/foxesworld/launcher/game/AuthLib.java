package org.foxesworld.launcher.game;


import org.foxesworld.engine.Engine;
import org.foxesworld.launcher.user.User;

import java.util.List;

public class AuthLib extends org.foxesworld.engine.game.AuthLib {
    private final GameLauncher gameLauncher;
    private final List<String> processArgs;
    private final User user;

    public AuthLib(GameLauncher gameLauncher){
        this.gameLauncher = gameLauncher;
        this.processArgs = gameLauncher.getProcessArgs();
        this.user = gameLauncher.user;
    }

    @Override
    protected void loadAuthLib() {
        try {
            gameLauncher.getClassLoader().loadClass("com.mojang.authlib.Agent");
            //processArgs.add("--userType=legacy");
            //processArgs.add("--accessToken=" + user.getToken());
            //processArgs.add("--uuid=" + user.getUuid());
            //processArgs.add("--userProperties={}");
        } catch (ClassNotFoundException e2) {
            Engine.LOGGER.warn("Couldn't load AuthLib! ");
            Engine.LOGGER.error(e2);
        }
    }
}