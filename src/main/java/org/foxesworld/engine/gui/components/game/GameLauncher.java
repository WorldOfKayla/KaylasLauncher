package org.foxesworld.engine.gui.components.game;

import org.foxesworld.engine.action.ActionHandler;
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

    private final User user;
    private final ServerAttributes selectedServer;
    private final LibraryScanner libraryScanner;
    private URLClassLoader cl;
    private final List<String> params = new ArrayList<>();
    private String tweakClassVal = "";
    boolean tweakClass = false;

    public GameLauncher(ActionHandler actionHandler) {
        this.actionHandler = actionHandler;
        this.selectedServer = actionHandler.getCurrentServer();
        this.libraryScanner = new LibraryScanner(actionHandler.getEngine());
        actionHandler.getEngine().getLOGGER().debug("#############################");
        actionHandler.getEngine().getLOGGER().debug("GameDir " + buildGameDir());
        actionHandler.getEngine().getLOGGER().debug("ClientDir " + buildClientDir());
        actionHandler.getEngine().getLOGGER().debug("VersionsDir " + buildVersionDir());
        actionHandler.getEngine().getLOGGER().debug("JarFile " + buildMinecraftJarPath());
        actionHandler.getEngine().getLOGGER().debug("Natives " + buildNativesPath());
        actionHandler.getEngine().getLOGGER().debug("Libraries " + buildLibrariesPath());
        actionHandler.getEngine().getLOGGER().debug("Assets " + buildAssetsPath());
        actionHandler.getEngine().getLOGGER().debug("#############################");
        this.user = actionHandler.getEngine().getUser();

        this.setJre();
        this.collectLibraries();
        this.addTweakClass();
        params.add(tweakClass ? "net.minecraft.launchwrapper.Launch" : "net.minecraft.client.main.Main");
        this.loadAuthLib();
        this.addArgs();
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
        actionHandler.getEngine().getLOGGER().debug(num + " libraries found");
    }

    private URLClassLoader createClassLoader(List<URL> libraryURLs) {
        URL[] urls = libraryURLs.toArray(new URL[0]);
        return new URLClassLoader(urls, getClass().getClassLoader());
    }

    private void loadAuthLib(){
        try {
            cl.loadClass("com.mojang.authlib.Agent");
            params.add("--accessToken="+this.user.getToken());
            params.add("--uuid="+this.user.getUuid());
            params.add("--userProperties={}"); //WIP
            params.add("--assetIndex="+selectedServer.serverVersion);
        } catch (ClassNotFoundException e2) {
            e2.printStackTrace();
            params.add("--session=65");
        }
    }

    private void addArgs() {
        params.add("--userType=legacy");
        params.add("--versionType=release");
        params.add("--username="+this.user.getLogin());
        params.add("--version="+selectedServer.serverVersion);
        params.add("--gameDir="+buildClientDir());
        params.add("--assetsDir="+buildAssetsPath());
        if((boolean) actionHandler.getEngine().getCONFIG().getCONFIG().get("fullScreen")) {
            params.add("--fullscreen=true");
        }
        params.add(tweakClassVal);
    }

    public void launchGame() {
        Thread gameThread = new Thread(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(params);
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();
                actionHandler.getEngine().getFrame().getFrame().setVisible(false);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    SwingUtilities.invokeLater(() -> {
                        actionHandler.getEngine().getLOGGER().error("Error launching minecraft. Error code: " + exitCode);
                        JOptionPane.showMessageDialog(actionHandler.getEngine().getFrame().getFrame(), "Exitcode - " + exitCode, "Launch error", JOptionPane.ERROR_MESSAGE, null);
                    });
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        gameThread.start();
    }


    private void addTweakClass() {
        List<TweakClasses> tweakClasses = actionHandler.getEngine().getEngineData().tweakClasses;
        for (TweakClasses aClass : tweakClasses) {
            String className = aClass.classPath;
            try {
                cl.loadClass(className);
                tweakClassVal = "--tweakClass=" + className;
                tweakClass = true;
                actionHandler.getEngine().getLOGGER().debug("TweakClass " + className + " was found!");
                System.setProperty("fml.ignoreInvalidMinecraftCertificates", "true");
                System.setProperty("fml.ignorePatchDiscrepancies", "true");
                return;
            } catch (ClassNotFoundException classNotFoundException) {
                actionHandler.getEngine().getLOGGER().debug("TweakClass " + className + " not found");
            }
        }
    }


    private String buildGameDir() {
        return actionHandler.getEngine().getCONFIG().getFullPath();
    }



    private void setJre() {
        params.add(buildRuntimeDir() + File.separator + "jre-8-271-x64" + File.separator + "bin" + File.separator + "java");
        params.add("-Xmx" + actionHandler.getEngine().getCONFIG().getCONFIG().get("ramAmount") + "m");
        params.add("-Djava.library.path=" + buildNativesPath());
        params.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
    }

    private String buildVersionDir() {
        return buildGameDir() + "versions" + File.separator + selectedServer.serverVersion;
    }

    private String buildLibrariesPath() {
        return buildVersionDir() + File.separator + "libraries";
    }

    private String buildMinecraftJarPath() {
        return buildVersionDir() + File.separator + selectedServer.serverVersion + ".jar";
    }

    private String buildNativesPath() {
        return buildVersionDir() + File.separator + "natives";
    }

    private String buildClientDir() {
        File clientDir = new File(buildGameDir() + "clients" + File.separator + selectedServer.serverName);
        if (!clientDir.isDirectory()) {
            actionHandler.getEngine().getLOGGER().debug("Creating " + selectedServer.serverName + " directory");
            clientDir.mkdirs();
        }
        return clientDir.toString();
    }

    private String buildAssetsPath() {
        return buildGameDir() + "assets";
    }

    private File buildRuntimeDir(){
        File runtimeDir =  new File(buildGameDir() + "runtime");
        if(!runtimeDir.isDirectory()){
            runtimeDir.mkdirs();
        }
        return runtimeDir;
    }
}
