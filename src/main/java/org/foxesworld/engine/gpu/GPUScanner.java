package org.foxesworld.engine.gpu;

import org.foxesworld.engine.Engine;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GPUScanner {

    private Engine engine;
    public GPUScanner(Engine engine){
        this.engine = engine;
    }

    public int scanGPUs() {
        int selectedGPUIndex = -1;
        try {
            Process process = Runtime.getRuntime().exec(getGPUInfoCommand());
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            int gpuIndex = 0;
            engine.getLOGGER().info("Available GPUs:");

            List<String> gpuNames = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    System.out.println("  " + gpuIndex + ": " + line.trim());
                    gpuNames.add(line.trim());
                    gpuIndex++;
                }
            }

            if (gpuIndex > 1) {
                selectedGPUIndex = promptUserForGPUSelection(gpuNames);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return selectedGPUIndex;
    }

    private String getGPUInfoCommand() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return "wmic path win32_videocontroller get name";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            return "lspci | grep VGA";
        }

        throw new UnsupportedOperationException("Unsupported operating system: " + os);
    }

    private int promptUserForGPUSelection(List<String> gpuNames) {
        String[] options = gpuNames.toArray(new String[0]);

        String selectedGPU = (String) JOptionPane.showInputDialog(
                null,
                "Multiple GPUs detected. Please select the GPU to use:",
                "GPU Selection",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);
        return gpuNames.indexOf(selectedGPU);
    }
}
