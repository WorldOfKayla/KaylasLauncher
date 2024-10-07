package org.foxesworld.launcher.auth;

import java.util.Map;

public interface AuthListener {
    void onLogin(Map<String, Object> authCredentials);

    void onLoad(Auth auth, Map<String, Object> authCredentials);
}
