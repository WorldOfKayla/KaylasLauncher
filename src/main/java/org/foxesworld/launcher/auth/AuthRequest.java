package org.foxesworld.launcher.auth;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.HTTP.HttpParam;

public class AuthRequest extends HTTPrequest {

    @HttpParam
    @SuppressWarnings("unused")
    private final String login, password, userAction = "auth";

    public AuthRequest(Engine engine, String login, String password) {
        super(engine, "POST");
        this.login = login;
        this.password = password;
    }
}
