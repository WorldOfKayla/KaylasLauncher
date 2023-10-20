package org.foxesworld.engine.action.server;

import com.google.gson.Gson;
import org.foxesworld.engine.Engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerParser {

    private Engine engine;

    private int serversNum = 0;
    private List<ServerAttributes> serverList = new ArrayList<>();
    private Map<String, String> request = new HashMap<>();

    public ServerParser(Engine engine) {
        this.engine = engine;
        request.put("sysRequest", "parseServers");
    }

    public List<ServerAttributes> parseServers(String login){
        request.put("login", login);
        String serversList = engine.getPOSTrequest().send(request);
        ServerAttributes[] serversArray = new Gson().fromJson(serversList, ServerAttributes[].class);
        for(ServerAttributes serverAttributes: serversArray){
            this.serverList.add(serverAttributes);
            this.serversNum+=1;
        }
        return serverList;
    }

    public int getServersNum() {
        return serversNum;
    }
}
