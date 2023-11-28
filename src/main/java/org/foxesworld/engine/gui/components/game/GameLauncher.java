package org.foxesworld.engine.gui.components.game;

import org.apache.logging.log4j.Logger;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.action.ActionHandler;
import org.foxesworld.engine.config.Config;
import org.foxesworld.launcher.server.ServerAttributes;
import org.foxesworld.launcher.user.User;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class GameLauncher {
    private final ActionHandler actionHandler;
    private final Engine engine;
    private final Logger logger;
    private final Config config;
    private final User user;
    private final ServerAttributes selectedServer;
    private final String currentJre;
    private final LibraryScanner libraryScanner;
    private final int versionNumbers;
    private URLClassLoader cl;
    private final List<String> processArgs = new ArrayList<>();
    private String tweakClassVal = "";
    private boolean tweakClass = false;
    public boolean isStarted;

    public GameLauncher(ActionHandler actionHandler) {
        this.actionHandler = actionHandler;
        this.config = actionHandler.getEngine().getCONFIG();
        this.selectedServer = actionHandler.getCurrentServer();
        this.currentJre = selectedServer.getJreVersion();
        this.libraryScanner = new LibraryScanner(actionHandler.getEngine());
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
        this.user = actionHandler.getEngine().getUser();
        this.versionNumbers = Integer.parseInt(this.selectedServer.getServerVersion().replace(".", ""));
    }

    private void collectLibraries() {
        int num = 0;
        processArgs.add("-cp");

        StringBuilder sb = new StringBuilder();
        List<URL> libraryURLs = new ArrayList<>();

        for (String libraryPath : libraryScanner.findLibraryPaths(buildLibrariesPath())) {
            File libraryFile = new File(libraryPath);
            sb.append(libraryFile.getAbsoluteFile()).append(File.pathSeparator);

            if (libraryFile.isFile()) {
                try {
                    URL libraryURL = libraryFile.toURI().toURL();
                    libraryURLs.add(libraryURL);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            num++;
        }
        sb.append(buildMinecraftJarPath()).append(File.pathSeparator);
        processArgs.add(sb.toString());

        cl = createClassLoader(libraryURLs);
        this.logger.debug(num + " libraries found");
    }

    private URLClassLoader createClassLoader(List<URL> libraryURLs) {
        URL[] urls = libraryURLs.toArray(new URL[0]);
        return new URLClassLoader(urls, getClass().getClassLoader());
    }

    private void loadAuthLib() {
        try {
            cl.loadClass("com.mojang.authlib.Agent");
            processArgs.add("--accessToken=" + this.user.getToken());
            processArgs.add("--uuid=" + this.user.getUuid());
            processArgs.add("--userProperties={}");
        } catch (ClassNotFoundException e2) {
            e2.printStackTrace();
            //processArgs.add("--session=" + this.user.getToken());
        }
    }

    private void addArgs() {
        processArgs.add("--userType=mojang");
        processArgs.add("--versionType=release");
        processArgs.add("--username=" + this.user.getLogin());
        processArgs.add("--version=" + selectedServer.getServerVersion());
        processArgs.add("--gameDir=" + buildClientDir());
        processArgs.add("--assetsDir=" + buildAssetsPath());
        processArgs.add("--assetIndex=" + selectedServer.getServerVersion());
        if (config.isFullScreen()) {
            processArgs.add("--fullscreen=true");
        }

        if (config.isAutoEnter()) {
            processArgs.add("--server=" + selectedServer.getHost());
            processArgs.add("--port=" + selectedServer.getPort());
        }
        if (versionNumbers > 1133) {
            processArgs.add("--fml.forgeVersion=" + this.selectedServer.getForgeVersion());
            processArgs.add("--fml.mcVersion=" + this.selectedServer.getServerVersion());
            processArgs.add("--launchTarget=" + this.selectedServer.getClient());
            //processArgs.add("--add-exports=java.base/sun.security.util=ALL-UNNAMED");
            //processArgs.add("-XX:+IgnoreUnrecognizedVMOptions");
            //processArgs.add("--add-exports=jdk.naming.dns/com.sun.jndi.dns=java.naming");
            //processArgs.add("--add-opens=java.base/java.util.jar=ALL-UNNAMED");
            processArgs.add("--fml.forgeGroup=net.minecraftforge");
            processArgs.add("--fml.mcpVersion=20210115.111550");
        }

        processArgs.add(tweakClassVal);
    }

    public void launchGame() {
        if (isStarted) throw new IllegalStateException("Process already started");
        Thread gameThread = new Thread(() -> {
            try {
                setJre();
                collectLibraries();

                // Adding --tweakclass only on versions under 1.13.3
                if (versionNumbers < 1133) {
                    addTweakClass();
                }

                processArgs.add(selectedServer.getMainClass());
                loadAuthLib();
                addArgs();

                // Log the command that will be executed
                logger.debug("Launching command: " + String.join(" ", processArgs));
                ProcessBuilder processBuilder = new ProcessBuilder(processArgs);
                processBuilder.directory(new File(this.buildClientDir()));
                processBuilder.redirectErrorStream(true);
                processBuilder.command().add("--illegal-access=warn");

                // Redirect error stream to the standard output
                processBuilder.inheritIO();

                Process process = processBuilder.start();
                engine.getFrame().getFrame().setVisible(false);

                int exitCode = process.waitFor();

                // Using invokeLater for Swing-related actions
                SwingUtilities.invokeLater(() -> {
                    if (exitCode != 0) {
                        logger.error("Error launching minecraft. Error code: " + exitCode);
                        JOptionPane.showMessageDialog(
                                actionHandler.getEngine().getFrame().getFrame(),
                                "Exit Code - " + exitCode,
                                "Launch error",
                                JOptionPane.ERROR_MESSAGE,
                                null
                        );
                    }
                });
            } catch (IOException | InterruptedException | RuntimeException e) {
                // Вывод StackTrace в консоль текущего приложения
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });

        // Add a shutdown hook to clean up threads
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            engine.getDiscord().getRpcThread().interrupt();
            gameThread.interrupt();
        }));
        isStarted = true;
        gameThread.start();
    }

    private void addTweakClass() {
        List<TweakClasses> tweakClasses = this.engine.getEngineData().getTweakClasses();
        for (TweakClasses aClass : tweakClasses) {
            String className = aClass.classPath;
            try {
                cl.loadClass(className);
                tweakClassVal = "--tweakClass=" + className;
                tweakClass = true;
                this.logger.debug("TweakClass " + className + " was found!");
                System.setProperty("fml.ignoreInvalidMinecraftCertificates", "true");
                System.setProperty("fml.ignorePatchDiscrepancies", "true");
                return;
            } catch (ClassNotFoundException classNotFoundException) {
                this.engine.getLOGGER().debug("TweakClass " + className + " not found");
            }
        }
    }

    public String buildGameDir() {
        return config.getFullPath();
    }

    private void setJre() {
        processArgs.add(buildRuntimeDir() + File.separator + currentJre + File.separator + "bin" + File.separator + "java");
        processArgs.add("-Xmx" + config.getRamAmount() + 'M');
        processArgs.add("-Djava.library.path=" + buildNativesPath());
        processArgs.add("-Dminecraft.launcher.brand=" + this.engine.getEngineData().getLauncherBrand());
        processArgs.add("-Dminecraft.launcher.version=" + this.engine.getEngineData().getLauncherVersion());
        processArgs.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
    }

    public String buildVersionDir() {
        return buildGameDir() + "versions" + File.separator + selectedServer.getServerVersion();
    }

    public String buildLibrariesPath() {
        return buildVersionDir() + File.separator + "libraries";
    }

    private String buildMinecraftJarPath() {
        return buildVersionDir() + File.separator + selectedServer.getServerVersion() + ".jar";
    }

    public String buildNativesPath() {
        return buildVersionDir() + File.separator + "natives";
    }

    public String buildClientDir() {
        File clientDir = new File(buildGameDir() + "clients" + File.separator + selectedServer.getServerName());
        if (!clientDir.isDirectory()) {
            this.engine.getLOGGER().debug("Creating " + selectedServer.getServerName() + " directory");
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

    public ServerAttributes getSelectedServer() {
        return selectedServer;
    }

    public String getCurrentJre() {
        return currentJre;
    }

    public ActionHandler getActionHandler() {
        return actionHandler;
    }
}
