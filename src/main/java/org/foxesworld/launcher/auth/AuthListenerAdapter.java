package org.foxesworld.launcher.auth;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;

import java.util.Map;

public class AuthListenerAdapter implements AuthListener {
    private final  Launcher launcher;
    private final  Auth auth;
    public AuthListenerAdapter(Auth auth){
        this.auth = auth;
        this.launcher = auth.getLauncher();
    }
    @Override
    public void onLogin(Map<String, Object> authCredentials) {

    }

    @Override
    public void onLoad(Auth auth, Map<String, Object> authCredentials) {
        launcher.logStartupTime(launcher.getStartTime());
        auth.authTask(authCredentials);

    }

    @Override
    public void onAuthSuccess(Object data) {
        Engine.LOGGER.debug("Auth SUCCESS");
        auth.updateBalance(((AuthResponse)data).getBalance());
        launcher.getSOUND().playSound("other", "loggedIn");
    }

    @Override
    public void onAuthFailure(Object data) {
        // Optional: Do nothing by default
    }

    @Override
    public void onAuthError(Object data) {
        // Optional: Do nothing by default
    }

    @Override
    public void onLogOut(Object data) {
        Engine.LOGGER.debug("LOGOUT");
    }
}
