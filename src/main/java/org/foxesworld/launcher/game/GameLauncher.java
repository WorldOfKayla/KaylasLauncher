package org.foxesworld.launcher.game;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.game.argsReader.ArgsReader;
import org.foxesworld.launcher.config.Config;
import org.foxesworld.launcher.gui.ActionHandler;
import org.foxesworld.launcher.user.User;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameLauncher extends org.foxesworld.engine.game.GameLauncher {

    private final Config config;
    private String mainClass, tweakClassVal = "";
    public final Launcher launcher;
    private Map<String, String> replaceValues = new HashMap<>();
    //private final ClientType clientType;
    protected final User user;
    private final AuthLib authLib;

    public GameLauncher(ActionHandler actionHandler) {
        this.launcher = actionHandler.getLauncher();
        this.config = actionHandler.getLauncher().getConfig();
        this.gameClient = actionHandler.getCurrentServer();
        this.engine = actionHandler.getEngine();
        this.pathBuilders = new pathBuilders(this);
        this.logger = Engine.getLOGGER();
        this.printDebug();
        this.user = actionHandler.getLauncher().getUser();
        this.authLib = new AuthLib(this);
        //this.clientType = ClientType.getType(this.gameClient.getClient());
        this.intVer = Integer.parseInt(this.gameClient.getServerVersion().replaceAll("\\D", ""));
        if(this.pathBuilders.getArgsFile() != null){
            argsReader = new ArgsReader(this);
        }
    }

    @Override
    protected void setJreArgs() {
        processArgs.add(getPathBuilders().buildRuntimeDir() + File.separator + this.gameClient.getJreVersion() + File.separator + "bin" + File.separator + "java");
        processArgs.add("-Xmx" + config.getRamAmount() + 'M');
        List<String> jvmArgs = getJvmArgs();
        this.addArgsToProcess(jvmArgs);
    }

    @Override
    protected void setGameArgs() {
        Engine.getLOGGER().debug("Client version " + getVersion());
        List<String> gameArgs = getGameArgs();
        logger.debug("GameArgs " + gameArgs.toString());
        this.addArgsToProcess(gameArgs);
        /*
        switch (this.clientType) {
            case forgeclient:
            case fmlclient:
                break;
            case fabricclient:
                break;
        }
        */
        //Optional
        if (config.isFullScreen()) {
            processArgs.add("--fullscreen=true");

        }
        //Optional
        if (config.isAutoEnter()) {
            processArgs.add("--server=" + gameClient.getHost());
            processArgs.add("--port=" + gameClient.getPort());
        }
        //processArgs.add("--width=" + this.config.getWidth());
        //processArgs.add("--height=" + this.config.getHeight());

        //if(this.User.re) Adding multiplayer only to an online User
        //processArgs.add("--disableMultiplayer");
        //processArgs.add("--disableChat");
        //processArgs.add(tweakClassVal);
    }

    @Override
    public void launchGame() {
        if (this.launcher.getEngine().getLoadingManager().getLoadingTimer().isRunning()) {
            //this.launcher.getEngine().getLoadingManager();
        }
        executorService.submit(() -> {
            this.checkDangerousParams();
            setJreArgs();
            try {
                // Adding --tweakclass only on versions under 1.13.3
                if (getIntVer() == 1710 || getIntVer() == 1122) {
                    tweakClassVal = addTweakClass();
                    mainClass = (tweakClassVal != null ? "net.minecraft.launchwrapper.Launch" : "net.minecraft.client.main.Main");
                }

                if (this.argsReader.getMainClass() != null) {
                    mainClass = argsReader.getMainClass();
                }

                processArgs.add(mainClass);
                if(Boolean.valueOf(this.argsReader.isAuthLib()).equals(true)) {
                    authLib.loadAuthLib();
                } else {
                    Engine.LOGGER.info("Launching without AuthLib loaded!");
                }
                setGameArgs();

                // Log the command that will be executed
                ProcessBuilder processBuilder = new ProcessBuilder(processArgs);
                processBuilder.directory(new File(getPathBuilders().buildClientDir()));
                processBuilder.redirectErrorStream(true);
                processBuilder.environment().put("JAVA_HOME", getPathBuilders().buildRuntimeDir().toString());

                // Redirect error stream to the standard output
                processBuilder.inheritIO();

                Process process = processBuilder.start();
                if (process.isAlive()) {
                    gameListener.onGameStart(gameClient);
                }
                engine.getFrame().setVisible(false);

                int exitCode = process.waitFor();
                gameListener.onGameExit(this.gameClient);
                // Using invokeLater for ssdd-related actions
                SwingUtilities.invokeLater(() -> {
                    if (exitCode != 0) {
                        logger.error("Error launching minecraft. Error code: " + exitCode);
                        JOptionPane.showMessageDialog(this.engine.getFrame(), "Exit Code - " + exitCode, this.launcher.getAppTitle() + " Crash", JOptionPane.ERROR_MESSAGE, this.launcher.getIconUtils().getVectorIcon("assets/ui/icons/bug.svg", 64, 64));
                        System.exit(0);
                    }
                });
            } catch (IOException | InterruptedException | RuntimeException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }
    @Override
    protected String addTweakClass() {
        String tweakClassVal;
        String[] tweakClasses = this.engine.getEngineData().getTweakClasses();
        for (String className : tweakClasses) {
            Engine.getLOGGER().debug("Searching " + className);
            try {
                System.out.println("Trying... " + className);
                getClassLoader().loadClass(className);
                tweakClassVal = "--tweakClass=" + className;
                this.logger.debug("TweakClass " + className + " was found!");
                System.setProperty("fml.ignoreInvalidMinecraftCertificates", "true");
                System.setProperty("fml.ignorePatchDiscrepancies", "true");
                return tweakClassVal;
            } catch (ClassNotFoundException classNotFoundException) {
                Engine.getLOGGER().debug("TweakClass " + className + " not found");
            }
        }
        return "";
    }

    private List<String> getGameArgs(){
        this.replaceValues = new HashMap<>();
        this.replaceValues.put("tweakClass", this.tweakClassVal);
        this.replaceValues.put("auth_player_name", this.user.getLogin());
        this.replaceValues.put("version_name", this.getVersion());
        this.replaceValues.put("game_directory", getPathBuilders().buildClientDir());
        this.replaceValues.put("assets_root", getPathBuilders().buildAssetsPath());
        this.replaceValues.put("assets_index_name", this.getVersion());
        this.replaceValues.put("auth_uuid", this.user.getUuid());
        this.replaceValues.put("auth_access_token", this.user.getToken());
        this.replaceValues.put("user_type", "legacy");
        this.replaceValues.put("version_type", "release");
        return this.argsReader.replaceMask(this.argsReader.getGameArguments(), this.replaceValues);
    }
    private List<String> getJvmArgs() {
        this.replaceValues = new HashMap<>();
        this.replaceValues.put("natives_directory", getPathBuilders().buildNativesPath());
        this.replaceValues.put("library_directory", getPathBuilders().buildLibrariesPath());
        this.replaceValues.put("launcher_name", this.engine.getEngineData().getLauncherBrand());
        this.replaceValues.put("launcher_version", this.engine.getEngineData().getLauncherVersion());
        this.replaceValues.put("classpath_separator", File.pathSeparator);
        String cp = this.argsReader.getLibraryReader().getLibrariesAsString(this.pathBuilders.buildLibrariesPath()) + this.pathBuilders.buildMinecraftJarPath();
        this.replaceValues.put("classpath", cp);
        this.replaceValues.put("version_name", this.gameClient.getServerVersion());
        return this.argsReader.replaceMask(this.argsReader.getJvmArguments(), this.replaceValues);
    }

    public String getJreBin() {
        return getPathBuilders().buildRuntimeDir().toString() + File.separator + getCurrentJre() + File.separator + "bin";
    }

    @Override
    public org.foxesworld.engine.game.GameLauncher.pathBuilders getPathBuilders() {
        return this.pathBuilders;
    }
}