package org.foxesworld.launcher.game;

import org.apache.logging.log4j.Logger;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.game.GPUInfo;
import org.foxesworld.engine.game.GameListener;
import org.foxesworld.engine.game.TweakClasses;
import org.foxesworld.engine.server.ServerAttributes;
import org.foxesworld.launcher.config.Config;
import org.foxesworld.launcher.gui.ActionHandler;
import org.foxesworld.engine.utils.ImageUtils;
import org.foxesworld.engine.utils.LibraryScanner;
import org.foxesworld.engine.utils.helper.JVMHelper;
import org.foxesworld.Launcher;
import org.foxesworld.launcher.user.User;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GameLauncher extends org.foxesworld.engine.game.GameLauncher {

    private final Config config;
    public final Launcher launcher;
    private final ClientType clientType;
    protected final User user;

    public GameLauncher(ActionHandler actionHandler) {
        this.launcher = actionHandler.getLauncher();
        this.config = actionHandler.getLauncher().getConfig();
        this.gameClient = actionHandler.getCurrentServer();
        this.engine = actionHandler.getEngine();
        this.logger = Engine.getLOGGER();
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
    }

    @Override
    protected void collectLibraries() {
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

    @Override
    protected URLClassLoader createClassLoader(List<URL> libraryURLs) {
        URL[] urls = libraryURLs.toArray(new URL[0]);
        return new URLClassLoader(urls, getClass().getClassLoader());
    }

    @Override
    protected void loadAuthLib(String accessToken, String UUID, String userProperties) {
        try {
            classLoader.loadClass("com.mojang.authlib.Agent");
            processArgs.add("--userType=legacy");
            processArgs.add("--accessToken=" + accessToken);
            processArgs.add("--uuid=" + UUID);
            processArgs.add("--userProperties={}");
        } catch (ClassNotFoundException e2) {
            //if AuthLib was not found (Old versions under 1.7.3)
            processArgs.add("--session=" + accessToken);
        }
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
                    processArgs.add("--fml.mcVersion=" + this.gameClient.getServerVersion());
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
                collectLibraries();
                // Adding --tweakclass only on versions under 1.13.3
                if (getIntVer() == 1710 || getIntVer() == 1122) {
                    tweakClassVal = addTweakClass();
                    mainClass = (tweakClassVal != null ? "net.minecraft.launchwrapper.Launch" : "net.minecraft.client.main.Main");
                } else {
                    mainClass = gameClient.getMainClass();
                }

                processArgs.add(mainClass);
                if(this.clientType != ClientType.fabricclient) {
                    loadAuthLib(this.user.getToken(), this.user.getUuid(), "");
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
                        //engine.getSOUND().playSound("exit.ogg");
                        JOptionPane.showMessageDialog(this.engine.getFrame(), "Exit Code - " + exitCode, "FoxesEngine 1.6 crash", JOptionPane.ERROR_MESSAGE, new ImageIcon(ImageUtils.getLocalImage("assets/ui/icons/bug.png")));
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

    public String buildGameDir() {
        return config.getFullPath();
    }

    @Override
    protected void setJre() {
        String gpu = new GPUInfo().getPreferredGPU();
        logger.info("Setting " + gpu + " as preferred card");
        processArgs.add(buildRuntimeDir() + File.separator + this.gameClient.getJreVersion() + File.separator + "bin" + File.separator + "java");
        processArgs.add("-Xmx" + config.getRamAmount() + 'M');
        processArgs.add(JVMHelper.jvmProperty("java.library.path", buildNativesPath()));
        processArgs.add(JVMHelper.jvmProperty("minecraft.launcher.brand", this.engine.getEngineData().getLauncherBrand()));
        processArgs.add(JVMHelper.jvmProperty("minecraft.launcher.version", this.engine.getEngineData().getLauncherVersion()));
        processArgs.add(JVMHelper.jvmProperty("user.language", this.launcher.getConfig().getLang()));
        processArgs.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
        processArgs.add("-Dorg.lwjgl.opengl.Display.neededGPUVendor=" + gpu);
    }

    public String buildVersionDir() {
        return buildGameDir() + "versions" + File.separator + gameClient.getServerVersion();
    }

    public String buildLibrariesPath() {
        return buildVersionDir() + File.separator + "libraries";
    }

    public String buildMinecraftJarPath() {
        return buildVersionDir() + File.separator + gameClient.getServerVersion() + ".jar";
    }

    public String buildNativesPath() {
        return buildVersionDir() + File.separator + "natives";
    }

    @Override
    public String buildClientDir() {
        File clientDir = new File(buildGameDir() + "clients" + File.separator + gameClient.getServerName());
        if (!clientDir.isDirectory()) {
            Engine.getLOGGER().debug("Creating " + gameClient.getServerName() + " directory");
            //noinspection ResultOfMethodCallIgnored
            clientDir.mkdirs();
        }
        return clientDir.toString();
    }

    @Override
    protected String buildAssetsPath() {
        return buildGameDir() + "assets";
    }

    @Override
    public File buildRuntimeDir() {
        File runtimeDir = new File(buildGameDir() + "runtime");
        if (!runtimeDir.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            runtimeDir.mkdirs();
        }
        return runtimeDir;
    }

    @SuppressWarnings("unused")
    public ServerAttributes getGameClient() {
        return gameClient;
    }

    @Override
    public String getCurrentJre() {
        return this.gameClient.getJreVersion();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void setStarted(boolean started) {
        isStarted = started;
        if (!isStarted) {
            executorService.shutdown();
        }
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    @Override
    public void setGameListener(GameListener gameListener) {
        this.gameListener = gameListener;
    }

    @Override
    public int getIntVer() {
        return intVer;
    }

    @Override
    public Engine getEngine() {
        return engine;
    }
}