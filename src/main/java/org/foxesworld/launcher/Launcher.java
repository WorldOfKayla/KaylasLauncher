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
public class Launcher extends Engine {
    private final Engine engine;
    private final Auth auth;
    private User user;
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Launcher::new);
    }
    public Launcher() {
        super("config,internal/engine");
        this.engine = this;
        this.auth = new Auth(this);
        if (!isLauncherValid(this)) {
            JOptionPane.showMessageDialog(new JFrame(), "Invalid MD5!", engine.getAppTitle(), JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        } else {
            initialize(this);
            setActionHandler(new ActionHandler(this));
            getLOGGER().debug("Launcher started!");
        }
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
    /*
    * NOW WE CAN OVERRIDE EACH METHOD FROM ENGINE ;)
    * */
    @Override
    public void onPanelsBuilt() {
        if (!isInit()) {
            getSOUND().playSound("mus/loginMus.ogg", true);
        }
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
    public void setUser(User user) {
        this.user = user;
    }
}