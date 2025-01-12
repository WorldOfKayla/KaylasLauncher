package org.foxesworld.launcher.auth;

import com.google.gson.Gson;
import org.foxesworld.Launcher;
import org.foxesworld.engine.Engine;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class EncryptionKeyManager {

    private final String filePath;
    private final Engine engine;
    private final Gson gson;

    public EncryptionKeyManager(Engine engine) {
        this.engine = engine;
        this.filePath = engine.getConfig().getFullPath() + "/cache/encryption.json";
        this.gson = new Gson();
        createEncryptionDirectory();
    }

    private void createEncryptionDirectory() {
        File directory = new File("cache");
        if (!directory.exists() && !directory.mkdirs()) {
            Launcher.LOGGER.error("Failed to create encryption directory");
        }
    }

    public String getEncryptionKey(int keyLength) {
        File file = new File(filePath);
        Map<String, Object> cipherFile;

        if (!file.exists()) {
            String newKey = generateRandomString(keyLength);
            cipherFile = new HashMap<>();
            cipherFile.put("hashCode", newKey);
            writeJson(file, cipherFile);
            return newKey;
        }

        try {
            cipherFile = readJson(file);
            return (String) cipherFile.get("hashCode");
        } catch (IOException e) {
            Engine.LOGGER.error("Error reading the encryption key file: {}", e.getMessage());
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
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            Engine.LOGGER.error("Error writing the encryption key file: {}", e.getMessage());
        }
    }

    private Map<String, Object> readJson(File file) throws IOException {
        try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(file.getPath()), StandardCharsets.UTF_8)) {
            return gson.fromJson(bufferedReader, Map.class);
        }
    }
}
