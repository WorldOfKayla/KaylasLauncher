package org.takesome.launcher.game;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.crash.CrashReportDialog;
import org.takesome.kaylasEngine.game.CommandLineSanitizer;
import org.takesome.kaylasEngine.game.JavaRuntimeLocator;
import org.takesome.kaylasEngine.game.process.ProcessExecution;
import org.takesome.launcher.Core;
import org.takesome.launcher.config.Config;
import org.takesome.launcher.gui.ActionHandler;
import org.takesome.launcher.user.User;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameLauncher extends org.takesome.kaylasEngine.game.GameLauncher {
    private static final String DEFAULT_MAIN_CLASS = "net.minecraft.client.main.Main";
    private static final int MAX_PROCESS_OUTPUT_CHARS = 24_000;

    private final Config config;
    private final ProcessExecution processExecution;
    private String mainClass = DEFAULT_MAIN_CLASS;
    private String tweakClassVal = "";
    public final Launcher launcher;
    protected final User user;
    private final AuthLib authLib;

    public GameLauncher(ActionHandler actionHandler) {
        this.launcher = actionHandler.getLauncher();
        this.config = actionHandler.getLauncher().getConfig();
        this.gameClient = actionHandler.getCurrentServer();
        this.engine = actionHandler.getEngine();
        this.pathBuilders = new PathBuilders(this, config.getHomeDir());
        this.logger = Engine.getLOGGER();
        this.processExecution = new ProcessExecution(this.logger);
        this.printDebug();
        this.user = actionHandler.getLauncher().getUser();
        this.authLib = new AuthLib(this);
        this.intVer = parseVersionNumber(this.gameClient.getServerVersion());
    }

    @Override
    protected void setJreArgs() {
        Path javaExecutable = getJavaExecutablePath();
        if (!Files.isRegularFile(javaExecutable)) {
            throw new IllegalStateException("Required game runtime was not downloaded or unpacked: " + javaExecutable);
        }
        processArgs.add(javaExecutable.toString());
        processArgs.add("-Xmx" + config.getRamAmount() + 'M');
        this.addArgsToProcess(getJvmArgs());
    }

    @Override
    protected void setGameArgs() {
        Engine.getLOGGER().debug("Client version {}", getVersion());
        List<String> gameArgs = getGameArgs();
        logger.debug("Prepared {} game argument(s).", gameArgs.size());
        this.addArgsToProcess(gameArgs);

        if (isAdminUser()) {
            this.processArgs.add("-Dforge.logging.console.level=debug");
            this.processArgs.add("-Dforge.logging.markers=SCAN,REGISTRIES,REGISTRYDUMP,CLASSLOADING");
        }
        if (config.isFullScreen()) {
            processArgs.add("--fullscreen=true");
        }
        if (config.isAutoEnter()) {
            processArgs.add("--server=" + gameClient.getHost());
            processArgs.add("--port=" + gameClient.getPort());
        }
    }

    @Override
    public void launchGame() {
        this.launcher.getExecutorServiceProvider().submitTask(
                this::launchGameInternal,
                "launch-" + safeTaskName(this.gameClient.getServerName())
        );
    }

    private void launchGameInternal() {
        try {
            resetLaunchState();
            checkDangerousParams();
            ensureArgsReaderReady();
            setJreArgs();
            initializeMainClass();
            processArgs.add(mainClass);

            if (this.argsReader.isAuthLib()) {
                authLib.loadAuthLib();
            } else {
                Engine.LOGGER.info("Launching without AuthLib loaded.");
            }
            setGameArgs();

            Engine.LOGGER.debug("Executing command: {}", sanitizedProcessArgs());

            ProcessExecution.Result result = executeGameProcess();
            int exitCode = result.exitCode();
            logger.info("Minecraft process exited with code {} after {} ms", exitCode, result.duration().toMillis());

            if (!result.successful()) {
                Exception launchException = new Exception("Minecraft exited with error code: " + exitCode
                        + "\nProcess output:\n" + result.output());
                reportFailure(launchException, exitCode);
                return;
            }

            notifyGameExit();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            reportFailure(error, 1);
        } catch (IOException | RuntimeException error) {
            reportFailure(error, 1);
        }
    }

    private ProcessExecution.Result executeGameProcess() throws IOException, InterruptedException {
        Path javaHome = javaHomePath();
        ProcessExecution.Request request = new ProcessExecution.Request(
                processArgs,
                getPathBuilders().buildClientDir(),
                Map.of("JAVA_HOME", javaHome.toString()),
                StandardCharsets.UTF_8,
                MAX_PROCESS_OUTPUT_CHARS,
                this::notifyGameStart
        );
        return processExecution.execute(request);
    }

    private void resetLaunchState() {
        resetProcessArgs();
        mainClass = DEFAULT_MAIN_CLASS;
        tweakClassVal = "";
    }

    private void ensureArgsReaderReady() {
        if (this.argsReader == null) {
            throw new IllegalStateException("Minecraft arguments reader is not initialized for " + this.pathBuilders.getArgsFile());
        }
    }

    private void initializeMainClass() {
        if (getIntVer() == 1710 || getIntVer() == 1122) {
            tweakClassVal = addTweakClass();
            mainClass = (tweakClassVal != null && !tweakClassVal.isEmpty())
                    ? "net.minecraft.launchwrapper.Launch"
                    : DEFAULT_MAIN_CLASS;
        }
        if (this.argsReader.getMainClass() != null && !this.argsReader.getMainClass().isBlank()) {
            mainClass = argsReader.getMainClass();
        }
    }

    private Path javaHomePath() {
        Path javaExecutable = getJavaExecutablePath();
        Path binDir = javaExecutable.getParent();
        if (binDir != null && binDir.getParent() != null) {
            return binDir.getParent();
        }
        return getPathBuilders().buildRuntimeDir();
    }

    private void reportFailure(Throwable throwable, int exitCode) {
        int normalizedExitCode = exitCode <= 0 ? 1 : exitCode;
        logger.error("Exception occurred during game launch.", throwable);
        SwingUtilities.invokeLater(() -> {
            try {
                showErrorReport(throwable);
            } catch (RuntimeException dialogError) {
                logger.error("Unable to show crash report dialog.", dialogError);
            } finally {
                notifyGameFailed(throwable, normalizedExitCode);
            }
        });
    }

    private void showErrorReport(Throwable throwable) {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("Crash Source", "Minecraft process");
        context.put("Server", gameClient == null ? "unknown" : gameClient.getServerName());
        context.put("Version", gameClient == null ? "unknown" : gameClient.getServerVersion());
        context.put("Client", gameClient == null ? "unknown" : gameClient.getClient());
        context.put("Main Class", mainClass == null ? "unknown" : mainClass);
        context.put("Game Dir", getPathBuilders().buildClientDir().toAbsolutePath().toString());
        context.put("Runtime", getJavaExecutablePath().toAbsolutePath().toString());

        CrashReportDialog.show(
                engine,
                throwable,
                context,
                getPathBuilders().buildGameDir().resolve("crash-reports")
        );
    }

    @Override
    protected String addTweakClass() {
        String[] tweakClasses = this.engine.getEngineData().getTweakClasses();
        for (String className : tweakClasses) {
            Engine.getLOGGER().debug("Searching tweak class {}", className);
            try {
                getClassLoader().loadClass(className);
                this.logger.debug("TweakClass {} was found.", className);
                System.setProperty("fml.ignoreInvalidMinecraftCertificates", "true");
                System.setProperty("fml.ignorePatchDiscrepancies", "true");
                return "--tweakClass=" + className;
            } catch (ClassNotFoundException classNotFoundException) {
                Engine.getLOGGER().debug("TweakClass {} not found", className);
            }
        }
        return "";
    }

    private List<String> getGameArgs() {
        Map<String, String> values = new HashMap<>();
        values.put("tweakClass", this.tweakClassVal);
        values.put("auth_player_name", this.user.getLogin());
        values.put("version_name", this.getVersion());
        values.put("game_directory", getPathBuilders().buildClientDir().toAbsolutePath().toString());
        values.put("assets_root", getPathBuilders().buildAssetsPath().toAbsolutePath().toString());
        values.put("assets_index_name", assetIndexName());
        values.put("auth_uuid", this.user.getUuid());
        values.put("auth_access_token", this.user.getToken());
        values.put("clientid", "0");
        values.put("auth_xuid", "0");
        values.put("resolution_width", String.valueOf(this.config.getWidth()));
        values.put("resolution_height", String.valueOf(this.config.getHeight()));
        values.put("user_type", "msa");
        values.put("version_type", "release");
        return this.argsReader.replaceMask(this.argsReader.getGameArguments(), values);
    }

    private List<String> getJvmArgs() {
        Map<String, String> values = new HashMap<>();
        values.put("natives_directory", getPathBuilders().buildNativesPath().toAbsolutePath().toString());
        values.put("library_directory", getPathBuilders().buildLibrariesPath().toAbsolutePath().toString());
        values.put("launcher_name", this.engine.getEngineData().getLauncherBrand());
        values.put("launcher_version", this.engine.getEngineData().getLauncherVersion());
        values.put("classpath_separator", File.pathSeparator);
        if (this.argsReader == null) {
            throw new IllegalStateException("Minecraft arguments reader is not initialized for " + this.pathBuilders.getArgsFile());
        }
        if (this.argsReader.getLibraryReader() == null) {
            throw new IllegalStateException("Minecraft library reader is not initialized for " + this.pathBuilders.getArgsFile());
        }
        String cp = this.argsReader.getLibraryReader().getLibrariesAsString(this.pathBuilders.buildLibrariesPath().toAbsolutePath().toString())
                + this.pathBuilders.buildMinecraftJarPath();
        values.put("classpath", cp);
        values.put("version_name", this.gameClient.getServerVersion());
        return this.argsReader.replaceMask(this.argsReader.getJvmArguments(), values);
    }

    public String getJreBin() {
        return getJavaExecutablePath().getParent().toString();
    }

    public Path getJavaExecutablePath() {
        return JavaRuntimeLocator.locate(runtimeVersionDir(), runtimeDirectoryName());
    }

    private String runtimeDirectoryName() {
        return this.gameClient.getJreVersion() + '-' + Core.getOSPrefix() + "-x64";
    }

    private Path runtimeVersionDir() {
        return getPathBuilders().buildRuntimeDir().resolve(this.gameClient.getJreVersion());
    }

    public String buildAssetsPath() {
        return this.getPathBuilders().buildAssetsPath().toString();
    }

    private String assetIndexName() {
        if (this.argsReader != null && this.argsReader.getAssets() != null && !this.argsReader.getAssets().isBlank()) {
            return this.argsReader.getAssets();
        }
        return this.getVersion();
    }

    private boolean isAdminUser() {
        return this.user != null
                && this.user.getUserAttributes() != null
                && "admin".equalsIgnoreCase(String.valueOf(this.user.getUserAttributes().getGroup()));
    }

    private int parseVersionNumber(String version) {
        if (version == null || version.isBlank()) {
            return 0;
        }
        String digits = version.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException error) {
            Engine.LOGGER.warn("Unable to parse Minecraft version number from '{}'.", version);
            return 0;
        }
    }

    private String safeTaskName(String value) {
        String safeValue = value == null || value.isBlank() ? "unknown" : value.trim();
        return safeValue.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private List<String> sanitizedProcessArgs() {
        String token = this.user == null ? null : this.user.getToken();
        return CommandLineSanitizer.sanitize(
                processArgs,
                token == null || token.isBlank() ? List.of() : List.of(token)
        );
    }

    @Override
    public org.takesome.kaylasEngine.game.GameLauncher.PathBuilders getPathBuilders() {
        return this.pathBuilders;
    }
}
