package org.foxesworld.launcher.auth;

import java.util.Map;

public interface AuthListener {
    void onAuthAttempt(Auth auth, Map<String, Object> authCredentials);
    void onAuthSuccess(Object data);
    void onAuthFailure(Object data);
    void onAuthError(Object data);
    void onLogOut(Object data);
    void onBalanceLoaded(Object data);
}
