package org.foxesworld.launcher.server;

import com.google.gson.Gson;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.server.ServerAttributes;

import java.util.List;
public class ServerParser extends org.foxesworld.engine.server.ServerParser {

    public ServerParser(Engine engine) {
        this.engine = engine;
        request.put("sysRequest", "parseServers");
    }
    @Override
    public List<ServerAttributes> parseServers(String login) {
        request.put("login", login);
        String serversList = engine.getPOSTrequest().send(engine.getEngineData().getBindUrl(), request);
        ServerAttributes[] serversArray = new Gson().fromJson(serversList, ServerAttributes[].class);
        for (ServerAttributes serverAttributes : serversArray) {
            this.serverList.add(serverAttributes);
            serversNum++;
        }
        Engine.getLOGGER().debug("Loading " + serversNum + " servers for User " + login);
        return serverList;
    }
    public int getServersNum() {
        return serversNum;
    }
}