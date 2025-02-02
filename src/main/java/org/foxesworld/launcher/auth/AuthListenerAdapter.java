package org.foxesworld.launcher.auth;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.launcher.user.User;

import java.util.Map;

public class AuthListenerAdapter implements AuthListener {
    private final  Launcher launcher;
    private final  Auth auth;
    public AuthListenerAdapter(Auth auth){
        this.auth = auth;
        this.launcher = auth.getLauncher();
    }


    @Override
    public void onAuthAttempt(Auth auth, Map<String, Object> authCredentials) {
        launcher.logStartupTime(launcher.getStartTime());
        auth.authTask(authCredentials);

    }

    @Override
    public void onAuthSuccess(Object data) {
        auth.updateBalance(((AuthResponse)data).getBalance());
        launcher.getSOUND().playSound("other", "loggedIn");
        System.out.println(((AuthResponse)data).getUserFullName());
    }

    @Override
    public void onAuthFailure(Object data) {
    }

    @Override
    public void onAuthError(Object data) {
    }

    @Override
    public void onLogOut(Object data) {
        Engine.LOGGER.debug("LOGOUT");
    }
}
