package org.foxesworld.launcher.game;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.game.GPUInfo;
import org.foxesworld.engine.game.argsReader.ArgsReader;
import org.foxesworld.engine.utils.ImageUtils;
import org.foxesworld.launcher.config.Config;
import org.foxesworld.launcher.gui.ActionHandler;
import org.foxesworld.launcher.user.User;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GameLauncher extends org.foxesworld.engine.game.GameLauncher {
    private final Config config;
    private final String javaBinPath;
    private final Map<String, String> replaceValues = new HashMap<>();
    private final ArgsReader argsReader;
    private final  AuthLib authLib;
    public final Launcher launcher;
    private final ClientType clientType;
    protected final User user;

    public GameLauncher(ActionHandler actionHandler) {
        this.launcher = actionHandler.getLauncher();
        this.config = actionHandler.getLauncher().getConfig();
        this.gameClient = actionHandler.getCurrentServer();
        this.engine = actionHandler.getEngine();
        this.logger = Engine.getLOGGER();
        this.argsReader= new ArgsReader(getArgsFile());
        this.getLogger().debug("#############################");
        this.logger.debug("GameDir " + buildGameDir());
        this.logger.debug("ClientDir " + buildClientDir());
        this.logger.debug("VersionsDir " + buildVersionDir());
        this.logger.debug("JarFile " + buildMinecraftJarPath());
        this.logger.debug("Natives " + buildNativesPath());
        this.logger.debug("Libraries " + buildLibrariesPath());
        this.logger.debug("Assets " + buildAssetsPath());
        this.logger.debug("#############################");
        this.user = launcher.getUser();
        this.clientType = ClientType.getType(this.gameClient.getClient());
        this.intVer = Integer.parseInt(this.gameClient.getServerVersion().replaceAll("\\D", ""));
        javaBinPath = this.buildRuntimeDir() + File.separator + gameClient.getJreVersion() + File.separator + "bin";
        this.authLib = new AuthLib(this);
    }

    @Override
    protected void addArgs(String tweakClassVal) {
        String version = gameClient.getServerVersion();
        if (gameClient.getServerVersion().contains("-")) {
            version = gameClient.getServerVersion().split("-")[0];
        }
        Engine.getLOGGER().debug("Client version " + version);
        processArgs.add("--versionType=release");
        processArgs.add("--username=" + this.user.getLogin());
        if (this.clientType != ClientType.fabricclient) {
            processArgs.add("--version=" + version);
        }
        processArgs.add("--gameDir=" + buildClientDir());
        processArgs.add("--assetsDir=" + buildAssetsPath());
        processArgs.add("--assetIndex=" + version);
        switch (this.clientType) {
            case fmlclient -> {
                if (getIntVer() > 1133) {
                    processArgs.add("--fml.forgeVersion=" + this.gameClient.getForgeVersion());
                    processArgs.add("--fml.mcVersion=" + version);
                    processArgs.add("--launchTarget=" + this.gameClient.getClient());
                    processArgs.add("--fml.forgeGroup=" + this.gameClient.getForgeGroup());
                    processArgs.add("--fml.mcpVersion=" + this.gameClient.getMcpVersion());
                    System.setProperty("org.objectweb.asm.util.traceClassVisitors", "true");
                }
            }


            case fabricclient -> {

            }
        }

        //Optional
        if (config.isFullScreen()) {
            processArgs.add("--fullscreen=true");
        }

        //Optional
        if (config.isAutoEnter()) {
            processArgs.add("--server=" + gameClient.getHost());
            processArgs.add("--port=" + gameClient.getPort());
        }

        //if(this.User.re) Adding multiplayer only to an online User
        //processArgs.add("--disableMultiplayer");
        //processArgs.add("--disableChat");
        processArgs.add(tweakClassVal);
    }

    @Override
    public void launchGame() {
        if (isStarted()) throw new IllegalStateException("Process already started");
        executorService.submit(() -> {

            String mainClass;
            this.checkDangerousParams();
            try {
                String tweakClassVal = "";
                setJre();
                this.classLoader = collectLibraries();
                // Adding --tweakclass only on versions under 1.13.3
                if (getIntVer() == 1710 || getIntVer() == 1122) {
                    tweakClassVal = tweakClass();
                    mainClass = (tweakClassVal != null ? "net.minecraft.launchwrapper.Launch" : "net.minecraft.client.main.Main");
                } else {
                    mainClass = gameClient.getMainClass();
                }

                processArgs.add(mainClass);
                if(this.clientType != ClientType.fabricclient) {
                    authLib.loadAuthLib();
                }

                addArgs(tweakClassVal);

                // Log the command that will be executed
                logger.debug("Launch command: " + String.join(" ", processArgs));
                ProcessBuilder processBuilder = new ProcessBuilder(processArgs);
                processBuilder.directory(new File(this.buildClientDir()));
                processBuilder.redirectErrorStream(true);
                processBuilder.environment().put("JAVA_HOME", buildRuntimeDir().toString());

                // Redirect error stream to the standard output
                processBuilder.inheritIO();

                Process process = processBuilder.start();
                if (process.isAlive()) {
                    gameListener.onGameStart(gameClient);
                }
                engine.getFrame().setVisible(false);

                int exitCode = process.waitFor();
                gameListener.onGameExit(this);
                // Using invokeLater for Swing-related actions
                SwingUtilities.invokeLater(() -> {
                    if (exitCode != 0) {
                        logger.error("Error launching minecraft. Error code: " + exitCode);
                        JOptionPane.showMessageDialog(this.engine.getFrame(), "Exit Code - " + exitCode, "FoxesEngine 1.6 crash", JOptionPane.ERROR_MESSAGE, new ImageIcon(ImageUtils.getLocalImage("assets/ui/icons/bug.png")));
                        System.exit(0);
                    }
                });
            } catch (IOException | InterruptedException | RuntimeException e) {
                throw new RuntimeException(e);
            }
        });
        setStarted(true);
    }

    @Override
    protected void setJre() {
        String gpu = new GPUInfo().getPreferredGPU();
        logger.info("Setting " + gpu + " as preferred card");

        processArgs.add(buildRuntimeDir() + File.separator + this.gameClient.getJreVersion() + File.separator + "bin" + File.separator + "java");
        processArgs.add("-Xmx" + config.getRamAmount() + 'M');

        this.replaceValues.put("natives_directory", buildNativesPath());
        this.replaceValues.put("launcher_name", this.engine.getEngineData().getLauncherBrand());
        this.replaceValues.put("launcher_version", this.engine.getEngineData().getLauncherVersion());
        processArgs.add(String.valueOf(this.argsReader.replaceMask(this.argsReader.getJvmArguments(), this.replaceValues)));
        //processArgs.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
        //processArgs.add("-Dorg.lwjgl.opengl.Display.neededGPUVendor=" + gpu);
    }

    public String getJavaBinPath() {
        return javaBinPath;
    }
}