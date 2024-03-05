package org.foxesworld.launcher.auth;

import java.util.Map;

public interface AuthListener {
    void onLogin(Map<String, String> authCredentials);
    void onLoad(Auth auth, Map<String, String> authCredentials);
}
