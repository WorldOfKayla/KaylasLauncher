package org.foxesworld.launcher.auth;

import com.google.gson.Gson;
import org.foxesworld.Launcher;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class EncryptionKeyManager {

    private final String FILE_PATH = "cache/encryption";
    private final int KEY_LENGTH = 16;
    private final Launcher launcher;

    EncryptionKeyManager(Launcher launcher) {
        this.launcher = launcher;
        createEncryptionDirectory(); // Call the method to create the directory
    }

    private void createEncryptionDirectory() {
        File directory = new File("cache");
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                Launcher.LOGGER.error("Failed to create encryption directory");
            }
        }
    }

    String getEncryptionKey() {
        Map<String, Object> cipherFile = new HashMap<>();
        File file = new File(FILE_PATH);

        if (!file.exists()) {
            String newKey = generateRandomString(KEY_LENGTH);
            cipherFile.put("hashCode", newKey);
            writeJson(file, cipherFile);
        }

        try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(FILE_PATH), StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            Map<String, String> json = gson.fromJson(bufferedReader, Map.class);
            return json.get("hashCode");
        } catch (IOException e) {
            Launcher.LOGGER.error("Error reading the encryption key file {}", e);
        }
        return "";
    }

    private String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, length);
    }

    private void writeJson(File file, Map<String, Object> data) {
        try (Writer writer = new FileWriter(file)) {
            Gson gson = new Gson();
            gson.toJson(data, writer);
        } catch (IOException e) {
            Launcher.LOGGER.error("Error writing the encryption key file {}", e);
        }
    }
}
