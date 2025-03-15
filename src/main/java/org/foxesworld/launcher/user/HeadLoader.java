package org.foxesworld.launcher.user;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.HTTP.HttpParam;

public class HeadLoader extends HTTPrequest {
    @HttpParam
    private String login;
    @HttpParam
    private final String sysRequest = "userHead";
    public HeadLoader(Engine engine, String requestMethod, String login) {
        super(engine, requestMethod);
        this.login = login;
    }
}
