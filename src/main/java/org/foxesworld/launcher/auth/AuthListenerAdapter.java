package org.foxesworld.launcher.auth;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.launcher.user.User;

import javax.swing.*;
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
        launcher.getSOUND().playSound("other", "loggedIn");
        //launcher.getFrame().repaint();
        //launcher.init();
    }

    @Override
    public void onAuthFailure(Object data) {
        Engine.getLOGGER().info("Incorrect password for " + ((AuthResponse)data).getLogin() + "!");
        launcher.getSOUND().playSound("other", "alert");
        launcher.showDialog(((AuthResponse)data).getMessage(), launcher.getLANG().getString("auth.authTitle"), JOptionPane.WARNING_MESSAGE, false);
    }

    @Override
    public void onAuthError(Object data) {
    }

    @Override
    public void onLogOut(Object data) {
        Engine.LOGGER.debug("LOGOUT");
    }
}
