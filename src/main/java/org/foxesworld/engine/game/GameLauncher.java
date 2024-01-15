package org.foxesworld.engine.game;

import org.apache.logging.log4j.Logger;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.config.Config;
import org.foxesworld.engine.utils.ImageUtils;
import org.foxesworld.engine.utils.LibraryScanner;
import org.foxesworld.engine.utils.helper.JVMHelper;
import org.foxesworld.launcher.Launcher;
import org.foxesworld.launcher.Server.ServerAttributes;
import org.foxesworld.launcher.User.User;
import org.foxesworld.launcher.action.ActionHandler;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class GameLauncher {
    private Launcher launcher;
    private final ServerAttributes gameClient;
    private  GameListener gameListener;
    private final User user;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Engine engine;
    private final Logger logger;
    private final Config config;
    private final int intVer;
    private URLClassLoader classLoader;
    private final List<String> processArgs = new ArrayList<>();
    private boolean isStarted;

    public GameLauncher(ActionHandler actionHandler) {
        this.launcher = actionHandler.getLauncher();
        this.config = actionHandler.getEngine().getCONFIG();
        this.gameClient = actionHandler.getCurrentServer();
        this.engine = actionHandler.getEngine();
        this.logger = this.engine.getLOGGER();
        this.logger.debug("#############################");
        this.logger.debug("GameDir " + buildGameDir());
        this.logger.debug("ClientDir " + buildClientDir());
        this.logger.debug("VersionsDir " + buildVersionDir());
        this.logger.debug("JarFile " + buildMinecraftJarPath());
        this.logger.debug("Natives " + buildNativesPath());
        this.logger.debug("Libraries " + buildLibrariesPath());
        this.logger.debug("Assets " + buildAssetsPath());
        this.logger.debug("#############################");
        this.user = launcher.getUser();
        this.intVer = Integer.parseInt(this.gameClient.getServerVersion().replaceAll("[^0-9]", ""));
    }

    private void collectLibraries() {
        AtomicInteger num = new AtomicInteger();
        processArgs.add("-cp");

        StringBuilder sb = new StringBuilder();
        List<URL> libraryURLs = new LinkedList<>();

        new LibraryScanner(this.engine).findLibraryPaths(buildLibrariesPath()).forEach(libraryPathString -> {
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

        sb.append(buildMinecraftJarPath()).append(File.pathSeparator);
        processArgs.add(sb.toString());

        classLoader = createClassLoader(libraryURLs);
        this.logger.debug(num.get() + " libraries found");
    }

    private URLClassLoader createClassLoader(List<URL> libraryURLs) {
        URL[] urls = libraryURLs.toArray(new URL[0]);
        return new URLClassLoader(urls, getClass().getClassLoader());
    }

    private void loadAuthLib() {
        try {
            classLoader.loadClass("com.mojang.authlib.Agent");
            processArgs.add("--userType=legacy");
            processArgs.add("--accessToken=" + this.user.getToken());
            processArgs.add("--uuid=" + this.user.getUuid());
            processArgs.add("--userProperties={}");
        } catch (ClassNotFoundException e2) {
            e2.printStackTrace();
            //if AuthLib was not found (Old versions under 1.7.3)
            processArgs.add("--session=" + this.user.getToken());
        }
    }

    private void addArgs(String tweakClassVal) {
        processArgs.add("--versionType=release");
        processArgs.add("--username=" + this.user.getLogin());
        processArgs.add("--version=" + gameClient.getServerVersion());
        processArgs.add("--gameDir=" + buildClientDir());
        processArgs.add("--assetsDir=" + buildAssetsPath());
        processArgs.add("--assetIndex=" + gameClient.getServerVersion());

        if (getIntVer() > 1133) {
            processArgs.add("--fml.forgeVersion="+this.gameClient.getForgeVersion());
            processArgs.add("--fml.mcVersion="+this.gameClient.getServerVersion());
            processArgs.add("--launchTarget="+this.gameClient.getClient());
            processArgs.add("--fml.forgeGroup="+this.gameClient.getForgeGroup());
            processArgs.add("--fml.mcpVersion="+this.gameClient.getMcpVersion());
            System.setProperty("org.objectweb.asm.util.traceClassVisitors", "true");
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

    public void launchGame() {

        if (isStarted()) throw new IllegalStateException("Process already started");

        executorService.submit(() -> {
            String mainClass;
            try {
                String tweakClassVal = "";
                setJre();
                collectLibraries();
                // Adding --tweakclass only on versions under 1.13.3
                if (getIntVer() == 1710 || getIntVer() == 1122) {
                    tweakClassVal = addTweakClass();
                    mainClass = (tweakClassVal != null ? "net.minecraft.launchwrapper.Launch" : "net.minecraft.client.main.Main");
                } else {
                    mainClass = gameClient.getMainClass();
                }

                processArgs.add(mainClass);
                loadAuthLib();
                addArgs(tweakClassVal);

                // Log the command that will be executed
                logger.debug("Launching command: " + String.join(" ", processArgs));
                ProcessBuilder processBuilder = new ProcessBuilder(processArgs);
                processBuilder.directory(new File(this.buildClientDir()));
                processBuilder.redirectErrorStream(true);
                processBuilder.environment().put("JAVA_HOME", buildRuntimeDir().toString());

                // Redirect error stream to the standard output
                processBuilder.inheritIO();

                Process process = processBuilder.start();
                if(process.isAlive()){
                    gameListener.onGameStart(gameClient);
                }
                engine.getFrame().setVisible(false);

                int exitCode = process.waitFor();
                gameListener.onGameExit(exitCode);
                // Using invokeLater for Swing-related actions
                SwingUtilities.invokeLater(() -> {
                    if (exitCode != 0) {
                        logger.error("Error launching minecraft. Error code: " + exitCode);
                        engine.getSOUND().playSound("exit.ogg", false);
                        JOptionPane.showMessageDialog(this.engine.getFrame(), "Exit Code - " + exitCode, "FoxesEngine 1.6 crash",JOptionPane.ERROR_MESSAGE, new ImageIcon(ImageUtils.getLocalImage("assets/ui/icons/bug.png")));
                        System.exit(0);
                    }
                });
            } catch (IOException | InterruptedException | RuntimeException e) {
                // Output StackTrace to the console of the current application
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
        setStarted(true);
    }

    private String addTweakClass() {
        String tweakClassVal;
        List<TweakClasses> tweakClasses = this.engine.getEngineData().getTweakClasses();
        for (TweakClasses aClass : tweakClasses) {
            String className = aClass.classPath;
            this.engine.getLOGGER().debug("Searching " + className);
            try {
                classLoader.loadClass(className);
                tweakClassVal = "--tweakClass=" + className;
                this.logger.debug("TweakClass " + className + " was found!");
                System.setProperty("fml.ignoreInvalidMinecraftCertificates", "true");
                System.setProperty("fml.ignorePatchDiscrepancies", "true");
                return tweakClassVal;
            } catch (ClassNotFoundException classNotFoundException) {
                this.engine.getLOGGER().debug("TweakClass " + className + " not found");
            }
        }
        return "";
    }

    public String buildGameDir() {
        return config.getFullPath();
    }

    private void setJre() {
        String gpu = new GPUInfo().getPreferredGPU();
        logger.info("Setting "+gpu + " as preferred card");
        processArgs.add(buildRuntimeDir() + File.separator + this.gameClient.getJreVersion() + File.separator + "bin" + File.separator + "java");
        processArgs.add("-Xmx" + config.getRamAmount() + 'M');
        processArgs.add(JVMHelper.jvmProperty("java.library.path", buildNativesPath()));
        processArgs.add(JVMHelper.jvmProperty("minecraft.launcher.brand", this.engine.getEngineData().getLauncherBrand()));
        processArgs.add(JVMHelper.jvmProperty("minecraft.launcher.version", this.engine.getEngineData().getLauncherVersion()));
        processArgs.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
        processArgs.add("-Dorg.lwjgl.opengl.Display.neededGPUVendor=" + gpu);
    }

    public String buildVersionDir() {
        return buildGameDir() + "versions" + File.separator + gameClient.getServerVersion();
    }

    public String buildLibrariesPath() {
        return buildVersionDir() + File.separator + "libraries";
    }

    private String buildMinecraftJarPath() {
        return buildVersionDir() + File.separator + gameClient.getServerVersion() + ".jar";
    }

    public String buildNativesPath() {
        return buildVersionDir() + File.separator + "natives";
    }

    public String buildClientDir() {
        File clientDir = new File(buildGameDir() + "clients" + File.separator + gameClient.getServerName());
        if (!clientDir.isDirectory()) {
            this.engine.getLOGGER().debug("Creating " + gameClient.getServerName() + " directory");
            clientDir.mkdirs();
        }
        return clientDir.toString();
    }

    private String buildAssetsPath() {
        return buildGameDir() + "assets";
    }

    public File buildRuntimeDir() {
        File runtimeDir = new File(buildGameDir() + "runtime");
        if (!runtimeDir.isDirectory()) {
            runtimeDir.mkdirs();
        }
        return runtimeDir;
    }

    public ServerAttributes getGameClient() {
        return gameClient;
    }

    public String getCurrentJre() {
        return this.gameClient.getJreVersion();
    }

    public Logger getLogger() {
        return logger;
    }

    public void setStarted(boolean started) {
        isStarted = started;
        if (!isStarted) {
            executorService.shutdown();
        }
    }

    public boolean isStarted() {
        return isStarted;
    }

    public void setGameListener(GameListener gameListener) {
        this.gameListener = gameListener;
    }

    public int getIntVer() {
        return intVer;
    }

    public Engine getEngine() {
        return engine;
    }
}