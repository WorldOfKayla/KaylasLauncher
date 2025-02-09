package org.foxesworld.launcher.auth;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.HTTP.HttpParam;

public class AuthRequest extends HTTPrequest {

    @HttpParam(key = "login")
    private final String login;

    @HttpParam(key = "password")
    private final String password;

    @HttpParam(key = "userAction")
    private final String userAction = "auth";

    public AuthRequest(Engine engine, String login, String password) {
        super(engine, "POST");
        this.login = login;
        this.password = password;
    }
}
