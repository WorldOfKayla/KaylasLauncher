package org.foxesworld.engine.gui.components.game;

import org.apache.logging.log4j.Logger;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.action.ActionHandler;
import org.foxesworld.engine.config.Config;
import org.foxesworld.launcher.server.ServerAttributes;
import org.foxesworld.launcher.user.User;

import javax.swing.*;
import java.io.BufferedReader;
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
    private final List<String> params = new ArrayList<>();
    private String tweakClassVal = "";
    boolean tweakClass = false;

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
        params.add("-cp");

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
        params.add(sb.toString());

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
            params.add("--accessToken=" + this.user.getToken());
            params.add("--uuid=" + this.user.getUuid());
            params.add("--userProperties={}");
        } catch (ClassNotFoundException e2) {
            e2.printStackTrace();
            params.add("--session=" + this.user.getToken());
        }
    }

    private void addArgs() {
        params.add("--userType=mojang");
        params.add("--versionType=release");
        params.add("--username=" + this.user.getLogin());
        params.add("--version=" + selectedServer.getServerVersion());
        params.add("--gameDir=" + buildClientDir());
        params.add("--assetsDir=" + buildAssetsPath());
        params.add("--assetIndex=" + selectedServer.getServerVersion());
        if (config.isFullScreen()) {
            params.add("--fullscreen=true");
        }

        if (config.isAutoEnter()) {
            params.add("--server=" + selectedServer.getHost());
            params.add("--port=" + selectedServer.getPort());
        }
        if (Integer.parseInt(this.selectedServer.getServerVersion().replace(".", "")) > 1133) {
            params.add("--add-opens=java.base/java.util=ALL-UNNAMED");
            params.add("--fml.forgeVersion=" + this.selectedServer.getForgeVersion());
            params.add("--fml.mcVersion=" + this.selectedServer.getServerVersion());
            params.add("--launchTarget=" + this.selectedServer.getClient());
            params.add("--fml.forgeGroup=net.minecraftforge");
            params.add("--fml.mcpVersion=20210115.111550");
        }

        params.add(tweakClassVal);
    }

    public void launchGame() {
        Thread gameThread = new Thread(() -> {
            try {
                this.setJre();
                this.collectLibraries();
                //Adding --tweakclass only on versions under 1.13.3
                if (this.versionNumbers < 1133) {
                    this.addTweakClass();
                }
                params.add(this.selectedServer.getMainClass());
                this.loadAuthLib();
                this.addArgs();
                System.out.println(params.toString().replace(",", ""));
                ProcessBuilder processBuilder = new ProcessBuilder(params);
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();
                this.engine.getFrame().getFrame().setVisible(false);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    SwingUtilities.invokeLater(() -> {
                        this.logger.error("Error launching minecraft. Error code: " + exitCode);
                        JOptionPane.showMessageDialog(this.engine.getFrame().getFrame(), "Exit Code - " + exitCode, "Launch error", JOptionPane.ERROR_MESSAGE, null);
                    });
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.engine.getDiscord().getRpcThread().interrupt();
            gameThread.interrupt();
        }));

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
        params.add(buildRuntimeDir() + File.separator + currentJre + File.separator + "bin" + File.separator + "java");
        params.add("-Xms" + config.getRamAmount() + "m");
        params.add("-Djava.library.path=" + buildNativesPath());
        params.add("-Dminecraft.launcher.brand=" + this.engine.getEngineData().getLauncherBrand());
        params.add("-Dminecraft.launcher.version=" + this.engine.getEngineData().getLauncherVersion());
        params.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
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