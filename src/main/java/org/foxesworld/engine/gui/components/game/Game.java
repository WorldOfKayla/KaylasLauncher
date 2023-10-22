package org.foxesworld.engine.gui.components.game;

import org.foxesworld.engine.action.ActionHandler;
import org.foxesworld.launcher.server.ServerAttributes;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class Game {

    private final ActionHandler actionHandler;
    private final ServerAttributes selectedServer;
    private final LibraryScanner libraryScanner;
    private URLClassLoader cl;
    private List<String> params = new ArrayList<>();
    private List<URL> url = new ArrayList<URL>();
    private String tweakClassVal = "";
    boolean tweakClass = false;

    public Game(ActionHandler actionHandler) {
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
        this.setJre();
        this.collectLibraries();
        this.addTweakClasses();
        params.add(tweakClass ? "net.minecraft.launchwrapper.Launch" : "net.minecraft.client.main.Main");
        this.addGameArgs();
        this.launchGame();
        System.out.print(params);
    }

    private void collectLibraries() {
        int num = 0;
        params.add("-cp");
        //params.add(buildMinecraftJarPath());
        StringBuilder sb = new StringBuilder();
        for (String libraryPath : libraryScanner.findLibraryPaths(buildLibrariesPath())) {
            File libraryFile = new File(libraryPath);
            sb.append(libraryFile.getAbsoluteFile() + ";");
            if (libraryFile.isFile()) {
                try {
                    URL libraryURL = libraryFile.toURI().toURL();
                    url.add(libraryURL);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            num++;
        }
        sb.append(buildMinecraftJarPath()+';');
        params.add(sb.toString());

        cl = new URLClassLoader(url.toArray(new URL[url.size()]));
        actionHandler.getEngine().getLOGGER().debug(num + " libraries found");
    }

    private void addGameArgs() {
        params.add("--accessToken=GG");
        params.add("--session");
        params.add("--userType=legacy");
        params.add("--versionType=release");
        params.add("--username="+actionHandler.getEngine().getCONFIG().getCONFIG().get("login"));
        params.add("--version="+selectedServer.serverVersion);
        params.add("--gameDir="+buildGameDir());
        params.add("--assetsDir="+buildAssetsPath());
        params.add(tweakClassVal);
    }

    private void launchGame() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(params);
            processBuilder.redirectErrorStream(true);
            Process process;
            process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                SwingUtilities.invokeLater(() -> {
                    actionHandler.getEngine().getLOGGER().error("Error launching minecraft. Error code: " + exitCode);
                    JOptionPane.showMessageDialog(actionHandler.getEngine().getFrame().getFrame(), "Exitcode - " + exitCode, "Launch error", 0, null);
                });
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private void addTweakClasses() {
        List<TweakClasses> tweakClasses = actionHandler.getEngine().getEngineData().tweakClasses;
        for (int i = 0; i < tweakClasses.size(); i++) {
            String className = tweakClasses.get(i).classPath;

            try {
                cl.loadClass(className);
                tweakClassVal = "--tweakClass="+className;
                tweakClass = true;
                actionHandler.getEngine().getLOGGER().debug("TweakClass " + className + " was found!");
            } catch (ClassNotFoundException classNotFoundException) {
                actionHandler.getEngine().getLOGGER().debug("TweakClass " + className + " not found");
            }
        }
    }


    private String buildGameDir() {
        return actionHandler.getEngine().getCONFIG().getFullPath();
    }

    private String buildClientDir() {
        File clientDir = new File(buildGameDir() + "clients" + File.separator + selectedServer.serverName);
        if (!clientDir.isDirectory()) {
            actionHandler.getEngine().getLOGGER().debug("Creating " + selectedServer.serverName + " directory");
            clientDir.mkdirs();
        }
        return clientDir.toString();
    }

    private void setJre() {
        String javaPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        params.add(javaPath);
        params.add("-Djava.library.path=" + buildNativesPath());
        params.add("-Xmx" + actionHandler.getEngine().getCONFIG().getCONFIG().get("ramAmount") + "m");
        System.setProperty("fml.ignoreInvalidMinecraftCertificates", "true");
        System.setProperty("fml.ignorePatchDiscrepancies", "true");
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

    private String buildAssetsPath() {
        return buildGameDir() + "assets";
    }
}
