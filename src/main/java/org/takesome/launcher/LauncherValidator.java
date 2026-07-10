package org.takesome.launcher;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.utils.HashUtils;
import org.takesome.kaylasEngine.utils.helper.JVMHelper;

import javax.swing.*;
import java.net.ServerSocket;
import java.io.IOException;

public class LauncherValidator {
    private final Launcher launcher;
    private static ServerSocket instanceSocket;
    private static final int bindPort = 45678;

    public LauncherValidator(Launcher launcher) {
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

        if (!"IDE".equals(selfMd5)) {
            Launcher.LOGGER.info("Launcher validation request is disabled; backend-managed validation is not implemented yet.");
        }
    }

    private void validateJRE() {
        int launchingWith = Integer.parseInt(JVMHelper.getJavaVersion(System.getProperty("java.home") + "/bin").replaceAll("\\D", ""));
        int expectedJRE = Integer.parseInt(launcher.getEngineData().getProgramRuntime().replaceAll("\\D", ""));

        if (launchingWith != expectedJRE) {
            logJREWarning(expectedJRE);
        }
    }

    private void logJREWarning(int expectedJRE) {
        if (launcher.getLauncherFile().isFile()) {
            Launcher.LOGGER.error("Using incorrect JRE " + expectedJRE);
            showDialog("error.invalidJVM", "Launcher Guard", JOptionPane.WARNING_MESSAGE, false);
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
