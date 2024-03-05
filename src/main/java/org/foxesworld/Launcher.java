package org.foxesworld;

import com.google.gson.Gson;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.gui.GuiBuilder;
import org.foxesworld.engine.gui.components.frame.OptionGroups;
import org.foxesworld.engine.gui.styles.StyleProvider;
import org.foxesworld.engine.news.News;
import org.foxesworld.engine.utils.md5Func;
import org.foxesworld.launcher.auth.Auth;
import org.foxesworld.launcher.config.Config;
import org.foxesworld.launcher.user.User;
import org.foxesworld.launcher.gui.ActionHandler;
import org.foxesworld.launcher.gui.components.Components;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
        super("config");
        this.engine = this;
        this.auth = new Auth(this);
        if (!isLauncherValid(this)) {
            JOptionPane.showMessageDialog(new JFrame(), "Invalid MD5!", engine.getAppTitle(), JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        } else {
            initialize(this);
            setActionHandler(new ActionHandler(this));
            new Config(this);
            getLOGGER().debug("Launcher started!");
        }
    }
    private boolean isLauncherValid(Engine engine) {
        Map<String, String> launcherRequest = new HashMap<>();
        launcherRequest.put("sysRequest", "downloadLatest");
        String selfMd5 = md5Func.md5(this.appPath());
        LauncherAttributes launcherAttributes = new Gson().fromJson(engine.getPOSTrequest().send(engine.getEngineData().getBindUrl(), launcherRequest), LauncherAttributes.class);
        if (!selfMd5.equals("IDE")) {
            return Objects.equals(selfMd5, launcherAttributes.getFileMd5());
        } else {
            return true;
        }
    }
    @Override
    public void onPanelsBuilt() {
        if (!isInit()) {
            getSOUND().playSound("mus/loginMus.ogg", true);
        }
    }
    @Override
    public void onPanelBuild(Map<String, OptionGroups> groups, String componentGroup, JPanel parentPanel) {
        parentPanel.updateUI();
        parentPanel.repaint();
        parentPanel.revalidate();
        parentPanel.setDoubleBuffered(true);
        LOGGER.debug("Built panel {} with parent {}", componentGroup, parentPanel.getName());
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        actionHandler.handleAction(e);
    }

    @Override
    public void initialize(Engine engine) {
        getDiscord().discordRpcStart(this.getLANG().getString("game.login") + this.getAuth().getAuthCredentials("login"), getAppTitle(), "aiden");
        setStyleProvider(new StyleProvider(this));
        setGuiBuilder(new GuiBuilder(this));
        this.getGuiBuilder().getComponentFactory().setComponentFactoryListener(new Components(this));
        getGuiBuilder().setGuiBuilderListener(this);
        setNews(new News(this));
        this.getGuiBuilder().buildGui(this.getGuiProperties().getFrameTpl(), this.getFrame().getRootPanel());
        loadMainPanel(this.getGuiProperties().getMainFrame());

        //ALL PANELS ARE BUILT
        this.getGuiBuilder().buildAdditionalPanels();
        this.setUser(new User(this));
        setInit(true);
        //if(!this.getCONFIG().isLoadNews()) {
        //    this.getFrame().setFrameSize(350, 500);
        //}
    }

    @Override
    public String appPath() {
        try {
            return URLDecoder.decode(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(), StandardCharsets.UTF_8);
        } catch (URISyntaxException var2) {
            return null;
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