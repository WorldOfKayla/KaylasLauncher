package org.foxesworld.launcher.game;


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
            processArgs.add("--userType=legacy");
            processArgs.add("--accessToken=" + user.getToken());
            processArgs.add("--uuid=" + user.getUuid());
            processArgs.add("--userProperties={}");
        } catch (ClassNotFoundException e2) {
            //if AuthLib was not found (Old versions under 1.7.3)
            processArgs.add("--session=" + user.getToken());
        }
    }
}