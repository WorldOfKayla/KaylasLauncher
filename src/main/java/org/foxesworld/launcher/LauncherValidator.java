package org.foxesworld.launcher;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.foxesworld.Launcher;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.HTTP.HttpParam;
import org.foxesworld.engine.utils.HashUtils;
import org.foxesworld.engine.utils.helper.JVMHelper;

import javax.swing.*;
import java.util.Map;
import java.util.Objects;
import java.net.ServerSocket;
import java.io.IOException;

public class LauncherValidator extends HTTPrequest {
    private final Launcher launcher;
    @HttpParam
    private final String sysRequest = "downloadLatest";
    private static ServerSocket instanceSocket;
    private static final int bindPort = 45678;

    public LauncherValidator(Launcher launcher) {
        super(launcher, "POST");
        this.launcher = launcher;
    }

    public void validate() {
        checkSingleInstance();
        validateLauncherFile();
        validateJRE();
    }

    /**
     * Checks that no other instance of the application is running by attempting to bind
     * a server socket to a specific port. If the binding fails, an error message is displayed
     * and the application terminates.
     */
    private void checkSingleInstance() {
        try {
            instanceSocket = new ServerSocket(bindPort);
            Launcher.LOGGER.info("Successfully bound to port {}. No other instances detected.", bindPort);
        } catch (IOException e) {
            Launcher.LOGGER.error("Another instance of the launcher is already running: " + e.getMessage());
            showDialog("error.alreadyRunning", launcher.getAppTitle() + " Guard", JOptionPane.WARNING_MESSAGE, true);
        }
    }

    private void validateLauncherFile() {
        Launcher.LOGGER.info("Starting launcher validation");
        String selfMd5 = HashUtils.md5(launcher.appPath());
        Launcher.LOGGER.info("Calculated self MD5: " + selfMd5);

        if ("IDE".equals(selfMd5)) {
            return;
        }

        sendAsyncCF(Map.of()).thenAccept(response -> {
                    try {
                        Launcher.LauncherAttributes launcherAttributes = new Gson()
                                .fromJson(response, Launcher.LauncherAttributes.class);
                        Launcher.LOGGER.info("Server response MD5: " + launcherAttributes.getFileMd5());

                        boolean isValid = Objects.equals(selfMd5, launcherAttributes.getFileMd5());
                        if (!isValid) {
                            Launcher.LOGGER.info("Launcher validation failed: MD5 mismatch");
                            showDialog("error.invalidLauncher", launcher.getAppTitle() + " Guard",
                                    JOptionPane.WARNING_MESSAGE, true);
                        }
                    } catch (JsonSyntaxException e) {
                        Launcher.LOGGER.error("JSON parsing error during launcher validation: " + e.getMessage());
                    }
                })
                .exceptionally(error -> {
                    Launcher.LOGGER.error("Unexpected error during launcher validation: " + error.getMessage());
                    return null;
                });
    }

    private void validateJRE() {
        int launchingWith = Integer.parseInt(JVMHelper.getJavaVersion(System.getProperty("java.home") + "/bin")
                .replaceAll("\\D", ""));
        int expectedJRE = Integer.parseInt(launcher.getEngineData().getProgramRuntime().replaceAll("\\D", ""));

        if (launchingWith != expectedJRE) {
            logJREWarning(expectedJRE);
        }
    }

    private void logJREWarning(int expectedJRE) {
        if (launcher.getLauncherFile().isFile()) {
            Launcher.LOGGER.error("Using incorrect JRE " + expectedJRE);
            showDialog("error.invalidJVM", "Launcher Guard", JOptionPane.WARNING_MESSAGE, true);
        } else if (launcher.getLauncherFile().isDirectory()) {
            Launcher.LOGGER.error("Using a JRE different from " + launcher.getEngineData().getProgramRuntime());
        } else {
            Launcher.LOGGER.error("Launcher path is neither a file nor a directory. 0_0");
        }
    }

    private synchronized void showDialog(String messageKey, String title, int messageType, boolean isModal) {
        launcher.showDialog(messageKey, title, messageType, isModal);
    }

    public static void closeSocket(){
        try {
            instanceSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
