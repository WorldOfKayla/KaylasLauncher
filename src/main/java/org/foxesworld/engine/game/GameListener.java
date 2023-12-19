package org.foxesworld.engine.game;

import org.foxesworld.launcher.Server.ServerAttributes;

public interface GameListener {
    void onGameStart(ServerAttributes server);
    void onGameExit(int exitCode);
}
