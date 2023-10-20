package org.foxesworld.engine.action.server;

import com.google.gson.Gson;
import org.foxesworld.engine.Engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerParser {

    private Engine engine;

    private List<ServerAttributes> serverList = new ArrayList<>();
    private Map<String, String> request = new HashMap<>();

    public ServerParser(Engine engine) {
        this.engine = engine;
        request.put("sysRequest", "parseServers");
        this.parseServers();
    }

    private void parseServers(){
        String serversList = engine.getPOSTrequest().send(request);
        ServerAttributes[] serversArray = new Gson().fromJson(serversList, ServerAttributes[].class);
        for(ServerAttributes serverAttributes: serversArray){
            this.serverList.add(serverAttributes);
        }
    }

    public List<ServerAttributes> getServerList() {
        return serverList;
    }
}
