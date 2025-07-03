package org.foxesworld.launcher.game;

import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.game.argsReader.ArgsReader;
import org.foxesworld.launcher.config.Config;
import org.foxesworld.launcher.gui.ActionHandler;
import org.foxesworld.launcher.user.User;

import javax.swing.*;
import java.awt.*;
import java.io.*;
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
        this.pathBuilders = new PathBuilders(this, config.getHomeDir());
        this.logger = Engine.getLOGGER();
        this.printDebug();
        this.user = actionHandler.getLauncher().getUser();
        this.authLib = new AuthLib(this);
        //this.clientType = ClientType.getType(this.gameClient.getClient());
        this.intVer = Integer.parseInt(this.gameClient.getServerVersion().replaceAll("\\D", ""));
        if(this.pathBuilders.getArgsFile() != null){
            boolean checkLib = actionHandler.getCurrentServer().isCheckLib();
            if(!checkLib) { logger.warn("LIBRARY HASH IS IGNORED!!! That may be insecure!!!"); }
            argsReader = new ArgsReader(this, checkLib);
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
        if (this.user.getUserAttributes().getGroup().equals("admin")) {
            this.processArgs.add("-Dforge.logging.console.level=debug");
            this.processArgs.add("-Dforge.logging.markers=SCAN,REGISTRIES,REGISTRYDUMP,CLASSLOADING");
        }
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
        processArgs.add("--width=" + this.config.getWidth());
        processArgs.add("--height=" + this.config.getHeight());

        //if(this.User.re) Adding multiplayer only to an online User
        //processArgs.add("--disableMultiplayer");
        //processArgs.add("--disableChat");
        //processArgs.add(tweakClassVal);
    }

    @Override
    public void launchGame() {
        this.launcher.getExecutorServiceProvider().submitTask(() -> {
            this.checkDangerousParams();
            setJreArgs();
            try {
                if (getIntVer() < 173) {
                    Engine.LOGGER.info("LEGACY!1!1!11!");
                }
                // Добавляем --tweakClass для определённых версий
                if (getIntVer() == 1710 || getIntVer() == 1122) {
                    tweakClassVal = addTweakClass();
                    mainClass = (tweakClassVal != null && !tweakClassVal.isEmpty()
                            ? "net.minecraft.launchwrapper.Launch" : "net.minecraft.client.main.Main");
                }
                if (this.argsReader.getMainClass() != null) {
                    mainClass = argsReader.getMainClass();
                }
                processArgs.add(mainClass);
                if (Boolean.valueOf(this.argsReader.isAuthLib())) {
                    authLib.loadAuthLib();
                } else {
                    Engine.LOGGER.info("Launching without AuthLib loaded!");
                }
                setGameArgs();

                // Логируем сформированную команду для отладки
                Engine.LOGGER.debug("Executing command: " + processArgs.toString());

                ProcessBuilder processBuilder = new ProcessBuilder(processArgs);
                processBuilder.directory(new File(getPathBuilders().buildClientDir().toUri()));
                processBuilder.redirectErrorStream(true);
                processBuilder.environment().put("JAVA_HOME", getPathBuilders().buildRuntimeDir().toString());
                // Перенаправляем вывод в PIPE для его захвата
                processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);

                Process process = processBuilder.start();
                if (process.isAlive()) {
                    gameListener.onGameStart(gameClient);
                }

                StringBuilder processOutput = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        processOutput.append(line).append("\n");
                    }
                }

                if (process.isAlive()) {
                    gameListener.onGameStart(gameClient);
                }
                engine.getFrame().setVisible(false);

                int exitCode = process.waitFor();
                System.out.println("Process exit code: " + exitCode);

                if (exitCode != 0) {
                    logger.error("Error launching minecraft. Error code: " + exitCode);
                    // Создаем исключение с текстом, включающим код завершения и вывод процесса
                    Exception launchException = new Exception("Minecraft exited with error code: " + exitCode +
                            "\nProcess output:\n" + processOutput);
                    SwingUtilities.invokeLater(() -> {
                        showErrorReport(launchException);
                        gameListener.onGameExit(this.gameClient);
                        System.exit(1);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        logger.info("Task completed, stopping the game.");
                        gameListener.onGameExit(this.gameClient);
                        System.exit(0);
                    });
                }
            } catch (IOException | InterruptedException | RuntimeException e) {
                logger.error("Exception occurred during game launch: ", e);
                SwingUtilities.invokeLater(() -> {
                    showErrorReport(e);
                    gameListener.onGameExit(this.gameClient);
                });
            }
        }, "launch-" + this.gameClient.getServerName());
    }


    private void showErrorReport(Throwable throwable) {
        StringBuilder header = new StringBuilder();
        header.append("Crash Report\n");
        header.append("============\n");
        header.append("Date: ").append(java.time.LocalDateTime.now()).append("\n");
        header.append("OC: ").append(System.getProperty("os.name")).append(" ")
                .append(System.getProperty("os.version")).append("\n");
        header.append("Java: ").append(System.getProperty("java.version")).append("\n");
        header.append("User: ").append(System.getProperty("user.name")).append("\n");
        header.append("\n");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        String errorText = header + stackTrace;

        JTextArea textArea = new JTextArea(errorText);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setBackground(new Color(30, 30, 30));
        textArea.setForeground(new Color(200, 200, 200));
        textArea.setCaretColor(new Color(200, 200, 200));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(700, 400));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("CoxpaHutb");

        saveButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Report");
            fileChooser.setSelectedFile(new File("crash_report.log"));
            int userSelection = fileChooser.showSaveDialog(engine.getFrame());
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                if (!fileToSave.getName().toLowerCase().endsWith(".log")) {
                    fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".log");
                }
                try (FileWriter fw = new FileWriter(fileToSave)) {
                    fw.write(errorText);

                } catch (IOException ioException) {
                    JOptionPane.showMessageDialog(engine.getFrame(), "Не удалось сохранить отчет:\n" + ioException.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            gameListener.onGameExit(this.gameClient);
        });

        buttonPanel.add(saveButton);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(
                engine.getFrame(),
                mainPanel,
                this.launcher.getAppTitle() + " Crash Report",
                JOptionPane.ERROR_MESSAGE,
                this.launcher.getIconUtils().getVectorIcon("assets/ui/icons/bug.svg", 64, 64)
        );
        gameListener.onGameExit(this.gameClient);
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
        this.replaceValues.put("game_directory", getPathBuilders().buildClientDir().toAbsolutePath().toString());
        this.replaceValues.put("assets_root", getPathBuilders().buildAssetsPath().toAbsolutePath().toString());
        this.replaceValues.put("assets_index_name", this.getVersion());
        this.replaceValues.put("auth_uuid", this.user.getUuid());
        this.replaceValues.put("auth_access_token", this.user.getToken());
        this.replaceValues.put("user_type", "legacy");
        this.replaceValues.put("version_type", "release");
        return this.argsReader.replaceMask(this.argsReader.getGameArguments(), this.replaceValues);
    }
    private List<String> getJvmArgs() {
        this.replaceValues = new HashMap<>();
        this.replaceValues.put("natives_directory", getPathBuilders().buildNativesPath().toAbsolutePath().toString());
        this.replaceValues.put("library_directory", getPathBuilders().buildLibrariesPath().toAbsolutePath().toString());
        this.replaceValues.put("launcher_name", this.engine.getEngineData().getLauncherBrand());
        this.replaceValues.put("launcher_version", this.engine.getEngineData().getLauncherVersion());
        this.replaceValues.put("classpath_separator", File.pathSeparator);
        String cp = this.argsReader.getLibraryReader().getLibrariesAsString(this.pathBuilders.buildLibrariesPath().toAbsolutePath().toString()) + this.pathBuilders.buildMinecraftJarPath();
        this.replaceValues.put("classpath", cp);
        this.replaceValues.put("version_name", this.gameClient.getServerVersion());
        return this.argsReader.replaceMask(this.argsReader.getJvmArguments(), this.replaceValues);
    }

    public String getJreBin() {
        return getPathBuilders().buildRuntimeDir().toString() + File.separator + getCurrentJre() + File.separator + "bin";
    }

    public String buildAssetsPath() {
        return this.getPathBuilders().buildGameDir() + "assets" + this.gameClient.getServerVersion();
    }

    @Override
    public org.foxesworld.engine.game.GameLauncher.PathBuilders getPathBuilders() {
        return this.pathBuilders;
    }
}