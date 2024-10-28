package org.foxesworld.launcher;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.foxesworld.Launcher;
import org.foxesworld.engine.utils.HashUtils;
import org.foxesworld.engine.utils.helper.JVMHelper;

import javax.swing.*;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class LauncherValidator {
    private final Launcher launcher;

    public LauncherValidator(Launcher launcher) {
        this.launcher = launcher;
    }

    public void validate() {
        CompletableFuture.runAsync(new LauncherFileValidationTask());
        CompletableFuture.runAsync(new JREValidationTask());
    }

    private class LauncherFileValidationTask implements Runnable {
        @Override
        public void run() {
            Launcher.LOGGER.info("Starting launcher validation");

            Map<String, Object> launcherRequest = Map.of("sysRequest", "downloadLatest");
            String selfMd5 = HashUtils.md5(launcher.appPath());
            Launcher.LOGGER.info("Calculated self MD5: " + selfMd5);

            if ("IDE".equals(selfMd5)) {
                return;
            }

            launcher.getPOSTrequest().sendAsync(launcherRequest,
                    response -> {
                        try {
                            Launcher.LauncherAttributes launcherAttributes = new Gson().fromJson(String.valueOf(response), Launcher.LauncherAttributes.class);
                            Launcher.LOGGER.info("Server response MD5: " + launcherAttributes.getFileMd5());

                            boolean isValid = Objects.equals(selfMd5, launcherAttributes.getFileMd5());
                            if (!isValid) {
                                Launcher.LOGGER.info("Launcher validation failed: MD5 mismatch");
                                showDialog("error.invalidLauncher", launcher.getAppTitle() + " Guard", JOptionPane.WARNING_MESSAGE, true);
                            }
                        } catch (JsonSyntaxException e) {
                            Launcher.LOGGER.error("JSON parsing error during launcher validation: " + e.getMessage());
                        }
                    },
                    error -> Launcher.LOGGER.error("Unexpected error during launcher validation: " + error.getMessage())
            );
        }
    }

    private class JREValidationTask implements Runnable {
        @Override
        public void run() {
            int launchingWith = Integer.parseInt(JVMHelper.getJavaVersion(System.getProperty("java.home") + "/bin").replaceAll("\\D", ""));
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
    }

    private synchronized void showDialog(String messageKey, String title, int messageType, boolean isModal) {
        launcher.showDialog(messageKey, title, messageType, isModal);
    }
}
