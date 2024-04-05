package org.foxesworld.launcher.game;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.game.ClientType;
import org.foxesworld.engine.game.GPUInfo;
import org.foxesworld.engine.game.GameListener;
import org.foxesworld.engine.game.TweakClasses;
import org.foxesworld.engine.game.argsReader.ArgsReader;
import org.foxesworld.engine.utils.ImageUtils;
import org.foxesworld.engine.utils.LibraryScanner;
import org.foxesworld.launcher.config.Config;
import org.foxesworld.launcher.gui.ActionHandler;
import org.foxesworld.launcher.user.User;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class GameLauncher extends org.foxesworld.engine.game.GameLauncher {

    private final Config config;
    public final Launcher launcher;
    private final Map<String, String> replaceValues = new HashMap<>();
    private final ClientType clientType;
    protected final User user;

    public GameLauncher(ActionHandler actionHandler) {
        this.launcher = actionHandler.getLauncher();
        this.config = actionHandler.getLauncher().getConfig();
        this.gameClient = actionHandler.getCurrentServer();
        this.engine = actionHandler.getEngine();
        this.pathBuilders = new pathBuilders(this);
        this.logger = Engine.getLOGGER();
        this.printDebug();
        this.user = actionHandler.getLauncher().getUser();
        this.clientType = ClientType.getType(this.gameClient.getClient());
        this.intVer = Integer.parseInt(this.gameClient.getServerVersion().replaceAll("\\D", ""));
        if(this.getArgsFile() != null){
            argsReader = new ArgsReader(this.getArgsFile());
        }
    }
    @Override
    protected StringBuilder collectLibraries() {
        AtomicInteger num = new AtomicInteger();
        StringBuilder sb = new StringBuilder();
        List<URL> libraryURLs = new LinkedList<>();

        new LibraryScanner(this.engine).findLibraryPaths(getPathBuilders().buildLibrariesPath()).forEach(libraryPathString -> {
            Path libraryPath = Paths.get(libraryPathString);
            sb.append(libraryPath.toAbsolutePath()).append(File.pathSeparator);
            if (libraryPath.toFile().isFile()) {
                try {
                    URL libraryURL = libraryPath.toUri().toURL();
                    libraryURLs.add(libraryURL);
                } catch (MalformedURLException e) {
                    logger.error("Error creating URL for library: " + libraryPath, e);
                }
            }
            num.getAndIncrement();
        });
        sb.append(getPathBuilders().buildMinecraftJarPath()).append(File.pathSeparator);
        classLoader = createClassLoader(libraryURLs);
        this.logger.debug(num.get() + " libraries found");
        return sb;
    }
    @Override
    protected void setJreArgs() {
        String gpu = new GPUInfo().getPreferredGPU();
        logger.info("Setting " + gpu + " as preferred card");
        processArgs.add(getPathBuilders().buildRuntimeDir() + File.separator + this.gameClient.getJreVersion() + File.separator + "bin" + File.separator + "java");
        processArgs.add("-Xmx" + config.getRamAmount() + 'M');
        processArgs.addAll(getJvmArgs());
    }

    @Override
    protected void setGameArgs() {
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
        processArgs.add("--gameDir=" + getPathBuilders().buildClientDir());
        processArgs.add("--assetsDir=" + getPathBuilders().buildAssetsPath());
        processArgs.add("--assetIndex=" + version);
        switch (this.clientType) {
            case forgeclient:
            case fmlclient:
                if (getIntVer() > 1133) {
                    processArgs.add("--fml.forgeVersion=" + this.gameClient.getForgeVersion());
                    processArgs.add("--fml.mcVersion=" + version);
                    processArgs.add("--launchTarget=" + this.gameClient.getClient());
                    processArgs.add("--fml.forgeGroup=" + this.gameClient.getForgeGroup());
                    processArgs.add("--fml.mcpVersion=" + this.gameClient.getMcpVersion());
                    System.setProperty("org.objectweb.asm.util.traceClassVisitors", "true");
                break;
            }


            case fabricclient:
                break;
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
        //processArgs.add("--width=" + this.config.getWidth());
        //processArgs.add("--height=" + this.config.getHeight());

        //if(this.User.re) Adding multiplayer only to an online User
        //processArgs.add("--disableMultiplayer");
        //processArgs.add("--disableChat");
        //processArgs.add(tweakClassVal);
    }

    @Override
    public void launchGame() {
        if (isStarted()) throw new IllegalStateException("Process already started");
        executorService.submit(() -> {
            String mainClass, tweakClassVal = "";
            this.checkDangerousParams();
            setJreArgs();
            try {
                // Adding --tweakclass only on versions under 1.13.3
                if (getIntVer() == 1710 || getIntVer() == 1122) {
                    tweakClassVal = addTweakClass();
                    mainClass = (tweakClassVal != null ? "net.minecraft.launchwrapper.Launch" : "net.minecraft.client.main.Main");
                } else {
                    mainClass = gameClient.getMainClass();
                }

                processArgs.add(mainClass);
                processArgs.add(tweakClassVal);
                if(this.clientType != ClientType.fabricclient) {
                    loadAuthLib(this.user.getToken(), this.user.getUuid(), "");
                }
                setGameArgs();

                // Log the command that will be executed
                logger.debug("Launch command: " + String.join(" ", processArgs));
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
                gameListener.onGameExit(this);
                // Using invokeLater for ssdd-related actions
                SwingUtilities.invokeLater(() -> {
                    if (exitCode != 0) {
                        logger.error("Error launching minecraft. Error code: " + exitCode);
                        JOptionPane.showMessageDialog(this.engine.getFrame(), "Exit Code - " + exitCode, "FoxesEngine 1.6 crash", JOptionPane.ERROR_MESSAGE, new ImageIcon(ImageUtils.getLocalImage("assets/ui/icons/bug.png")));
                        System.exit(0);
                    }
                });
            } catch (IOException | InterruptedException | RuntimeException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
        setStarted(true);
    }
    @Override
    protected String addTweakClass() {
        String tweakClassVal;
        List<TweakClasses> tweakClasses = this.engine.getEngineData().getTweakClasses();
        for (TweakClasses aClass : tweakClasses) {
            String className = aClass.classPath;
            Engine.getLOGGER().debug("Searching " + className);
            try {
                classLoader.loadClass(className);
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
    private List<String> getJvmArgs(){
        this.replaceValues.put("natives_directory", getPathBuilders().buildNativesPath());
        this.replaceValues.put("library_directory", getPathBuilders().buildLibrariesPath());
        this.replaceValues.put("launcher_name", this.engine.getEngineData().getLauncherBrand());
        this.replaceValues.put("launcher_version", this.engine.getEngineData().getLauncherVersion());
        this.replaceValues.put("classpath_separator", File.pathSeparator);
        this.replaceValues.put("classpath", collectLibraries().toString());
        this.replaceValues.put("version_name", this.gameClient.getServerVersion());
        return this.argsReader.replaceMask(this.argsReader.getJvmArguments(), this.replaceValues);
    }
    public  String getJreBin(){
        return getPathBuilders().buildRuntimeDir().toString() + File.separator + getCurrentJre() +  File.separator +  "bin";
    }
}