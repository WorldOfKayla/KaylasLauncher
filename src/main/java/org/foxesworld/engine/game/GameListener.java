package org.foxesworld.engine.game;

import org.foxesworld.launcher.server.ServerAttributes;

public interface GameListener {
    void onGameStart(ServerAttributes server);

    void onGameExit(int exitCode);

}
