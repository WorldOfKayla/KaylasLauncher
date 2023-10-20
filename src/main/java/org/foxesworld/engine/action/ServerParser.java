package org.foxesworld.engine.action;

import org.foxesworld.engine.AppFrame;

import java.util.HashMap;
import java.util.Map;

public class ServerParser {

    private AppFrame appFrame;

    private Map<String, String> request = new HashMap<>();

    public ServerParser(AppFrame appFrame) {
        this.appFrame = appFrame;
        request.put("sysRequest", "parseServers");
    }

    public void parseServers(){

    }
}
