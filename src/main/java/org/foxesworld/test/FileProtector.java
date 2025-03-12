package org.foxesworld.test;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class FileProtector {

    private static final Logger logger = Logger.getLogger(FileProtector.class.getName());

    // Храним каналы и локи, чтобы не освободить их случайно
    private final Map<Path, FileChannel> openChannels = new ConcurrentHashMap<>();
    private final Map<Path, FileLock> locks = new ConcurrentHashMap<>();

    /**
     * Защищает файл от изменений и удаления.
     */
    public void protectFile(Path path) {
        try {
            if (!Files.exists(path)) {
                logger.warning("File does not exist, skipping protect: " + path);
                return;
            }

            FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
            FileLock lock = channel.tryLock(0L, Long.MAX_VALUE, true); // true = shared, false = exclusive

            if (lock != null) {
                openChannels.put(path, channel);
                locks.put(path, lock);
                logger.info("File locked and protected: " + path);
            } else {
                logger.warning("Failed to acquire lock on: " + path);
            }
        } catch (IOException e) {
            logger.warning("IOException while protecting file: " + path + " due to " + e.getMessage());
        }
    }

    /**
     * Защищает рекурсивно все файлы в директории.
     */
    public void protectDirectory(Path dir) {
        try {
            Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .forEach(this::protectFile);
        } catch (IOException e) {
            logger.warning("Failed to protect directory: " + dir + " due to: " + e.getMessage());
        }
    }

    /**
     * Снимает защиту со всех файлов.
     */
    public void releaseAll() {
        locks.forEach((path, lock) -> {
            try {
                lock.release();
                logger.info("Released lock for: " + path);
            } catch (IOException e) {
                logger.warning("Failed to release lock for: " + path);
            }
        });

        openChannels.forEach((path, channel) -> {
            try {
                channel.close();
            } catch (IOException e) {
                logger.warning("Failed to close channel for: " + path);
            }
        });

        locks.clear();
        openChannels.clear();
    }
}
