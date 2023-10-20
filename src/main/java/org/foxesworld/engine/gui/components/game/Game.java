package org.foxesworld.engine.gui.components.game;

import org.foxesworld.engine.Engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Game {

    private final Engine engine;
    private final LibraryScanner libraryScanner;

    private final String absoluteHomePath;

    public Game(Engine engine) {
        this.engine = engine;
        this.libraryScanner = new LibraryScanner(engine);
        this.absoluteHomePath = engine.getCONFIG().getFullPath();
    }

    public void testLaunch() {
        String gameDir = this.absoluteHomePath + "\\1.16.5 Mods";
        String minecraftJarPath = gameDir+ "\\1.16.5 Mods.jar";
        String nativesPath = gameDir + "\\natives";
        String librariesPath = "C:\\Users\\Aiden\\AppData\\Roaming\\.minecraft\\libraries";
        String assetsPath = "C:\\Users\\Aiden\\AppData\\Roaming\\.minecraft\\assets";


        List<String> command = new ArrayList<>();
        String javaPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        command.add(javaPath);

        System.setProperty("fml.ignoreInvalidMinecraftCertificates", "true");
        System.setProperty("fml.ignorePatchDiscrepancies", "true");
        System.setProperty("org.lwjgl.librarypath", nativesPath);
        System.setProperty("net.java.games.input.librarypath", nativesPath);
        System.setProperty("java.library.path", nativesPath);

        // Collecting classpath, libraries
        List<String> libraryPaths = scanLibraries(librariesPath);

        // Forming launch string
        String classpath = String.join(File.pathSeparator, libraryPaths);
        classpath += File.pathSeparator + minecraftJarPath;
        command.add("-Xmx" + engine.getCONFIG().getCONFIG().get("ramAmount") + "m");
        command.add("-cp");
        command.add(classpath);

        // Minecraft params
        command.add("net.minecraft.client.main.Main");
        command.add("--username="+ engine.getCONFIG().getCONFIG().get("login"));
        command.add("--version=1.16.5");
        command.add("--gameDir=" + assetsPath);
        command.add("--assetsDir=" + assetsPath);
        command.add("--accessToken=YourAccessToken");
        command.add("--userProperties={}");

        if(engine.getCONFIG().getCONFIG().get("fullScreen").equals(true)){
            command.add("--fullscreen");
            command.add("true");
        }

        // Logging classpath to console
        System.out.println("Command: " + String.join(" ", command));

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                this.engine.getLOGGER().error("Error launching minecraft. Error code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            this.engine.getLOGGER().error("Minecraft launch exception: " + e.getMessage());
        }
    }


    private List<String> scanLibraries(String librariesPath) {
        List<String> libraryPaths = libraryScanner.findLibraryPaths(librariesPath);
        return libraryPaths;
    }
}
