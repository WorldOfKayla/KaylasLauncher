package org.foxesworld.launcher;

import com.google.gson.Gson;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.utils.md5Func;
import org.foxesworld.launcher.Auth.Auth;
import org.foxesworld.launcher.User.User;
import org.foxesworld.launcher.action.ActionHandler;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Launcher {
    private static Launcher launcher;
    private Engine engine;
    private Auth auth;
    private User user;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Engine engine;
            engine = new Engine("init.json");
            if (!isLauncherValid(engine)) {
                JOptionPane.showMessageDialog(new JFrame(), "Invalid MD5!", engine.getAppTitle(), JOptionPane.WARNING_MESSAGE);
                System.exit(0);
            }
            launcher = new Launcher(engine);
            engine.setActionHandler(new ActionHandler(launcher));
        });
    }

    public Launcher(Engine engine) {
        //SOME UNWANTED VARs should be removed
        this.engine = engine;
        this.auth = new Auth(engine);
        engine.setAuth(this.auth);
        engine.initialize(this.auth.getAuthCredentials("login"));
        this.user = new User(this.auth);
        engine.setUser(user);
    }
    private static boolean isLauncherValid(Engine engine) {
        Map<String, String> launcherRequest = new HashMap<>();
        launcherRequest.put("sysRequest", "downloadLatest");
        String selfMd5 = md5Func.md5(engine.appPath());
        LauncherAttributes launcherAttributes = new Gson().fromJson(engine.getPOSTrequest().send(engine.getEngineData().getBindUrl(), launcherRequest), LauncherAttributes.class);
        if (!selfMd5.equals("IDE")) {
            return Objects.equals(selfMd5, launcherAttributes.getFileMd5());
        } else {
            return true;
        }
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public Engine getEngine() {
        return engine;
    }

    public Auth getAuth() {
        return auth;
    }

    public User getUser() {
        return user;
    }
}
