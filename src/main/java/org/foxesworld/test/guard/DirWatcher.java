package org.foxesworld.test.guard;

import org.foxesworld.Launcher;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * DirWatcher monitors specified directories for file system changes and attempts to prevent modifications
 * by reapplying the "read-only" attribute on files and folders.
 * Additionally, it automatically deletes unauthorized creations, reverts modifications, and restores deleted files.
 */
public class DirWatcher implements Runnable, AutoCloseable {

    private static final Logger logger = Logger.getLogger(DirWatcher.class.getName());
    private static final String IGN_OVERFLOW_PROP = "dirwatcher.ignoreOverflows";
    private static final boolean IGN_OVERFLOW = Boolean.parseBoolean(System.getProperty(IGN_OVERFLOW_PROP, "true"));
    private static final WatchEvent.Kind<?>[] EVENT_KINDS = {
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE
    };

    private final List<Path> directories;
    private final WatchService watchService;
    private final ExecutorService executor;
    private final Consumer<Event> eventConsumer;
    private final Map<Path, byte[]> fileBackups = new HashMap<>();
    private volatile boolean running = true;

    /**
     * Constructs a DirWatcher to monitor and protect the specified directories.
     *
     * @param directories   list of directory paths to monitor and protect
     * @param eventConsumer consumer to process file system events; if null, the default action (protection) is used
     * @throws IOException if an I/O error occurs while initializing the WatchService
     */
    public DirWatcher(List<Path> directories, Consumer<Event> eventConsumer) throws IOException {
        this.directories = Objects.requireNonNull(directories, "directories");
        this.eventConsumer = eventConsumer;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.executor = Executors.newSingleThreadExecutor();

        // Register directories recursively and apply read-only protection.
        for (Path dir : directories) {
            registerDirectoryRecursively(dir);
        }

        // Backup original files for recovery.
        backupFiles();

        // Apply initial read-only protection.
        directories.forEach(this::applyReadOnlyRecursively);
        logger.info("DirWatcher initialized for directories: " + directories);
    }

    /**
     * Starts the DirWatcher in a separate thread.
     */
    public void start() {
        executor.submit(this);
    }

    /**
     * Stops monitoring and releases resources.
     */
    public void stop() {
        running = false;
        executor.shutdownNow();
        try {
            watchService.close();
        } catch (IOException e) {
            logger.severe("Error closing WatchService: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        logger.info("DirWatcher event processing started.");
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.take(); // blocking call
                processKey(key);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("DirWatcher interrupted.");
        } catch (ClosedWatchServiceException cwse) {
            logger.info("WatchService closed.");
        } catch (IOException e) {
            logger.severe("Error processing events: " + e.getMessage());
        } finally {
            stop();
        }
    }

    private void processKey(WatchKey key) throws IOException {
        Path watchDir = (Path) key.watchable();

        for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();

            if (kind == StandardWatchEventKinds.OVERFLOW) {
                if (IGN_OVERFLOW) {
                    continue;
                } else {
                    throw new IOException("Event overflow occurred");
                }
            }

            @SuppressWarnings("unchecked")
            WatchEvent<Path> ev = (WatchEvent<Path>) event;
            Path relativePath = ev.context();
            Path fullPath = watchDir.resolve(relativePath);

            Launcher.LOGGER.info("Detected event: " + kind.name() + " -> " + fullPath);

            if (eventConsumer != null) {
                eventConsumer.accept(new Event(kind, fullPath));
            } else {
                switch (kind.name()) {
                    case "ENTRY_CREATE" -> handleCreate(fullPath);
                    case "ENTRY_MODIFY" -> handleModify(fullPath);
                    case "ENTRY_DELETE" -> handleDelete(fullPath);
                }
            }

            if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(fullPath)) {
                registerDirectoryRecursively(fullPath);
            }
        }
        key.reset();
    }

    private void handleCreate(Path path) {
        try {
            if (Files.exists(path)) {
                if (Files.isDirectory(path)) {
                    deleteRecursively(path);
                    Launcher.LOGGER.info("Blocked and deleted unauthorized directory: " + path);
                } else {
                    Files.delete(path);
                    Launcher.LOGGER.info("Blocked and deleted unauthorized file: " + path);
                }
            }
        } catch (IOException e) {
            Launcher.LOGGER.warn("Failed to delete unauthorized creation: " + path + " due to: " + e.getMessage());
        }
    }

    private void handleModify(Path path) {
        try {
            Launcher.LOGGER.info("Modification detected. Reapplying read-only: " + path);
            // You can restore from backup if stricter control is needed
            restoreFile(path);
            applyReadOnlyRecursively(path);
        } catch (Exception e) {
            Launcher.LOGGER.warn("Failed to handle modification: " + path + " due to: " + e.getMessage());
        }
    }

    private void handleDelete(Path path) {
        try {
            Launcher.LOGGER.warn("Deletion detected! Attempting to restore: " + path);
            restoreFile(path);
            applyReadOnlyRecursively(path);
        } catch (IOException e) {
            Launcher.LOGGER.warn("Failed to restore deleted file: " + path + " due to: " + e.getMessage());
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        Launcher.LOGGER.warn("Failed to delete: " + p + " due to: " + e.getMessage());
                    }
                });
    }

    private void backupFiles() {
        for (Path dir : directories) {
            try {
                Files.walk(dir)
                        .filter(Files::isRegularFile)
                        .forEach(file -> {
                            try {
                                fileBackups.put(file, Files.readAllBytes(file));
                            } catch (IOException e) {
                                Launcher.LOGGER.warn("Failed to backup file: " + file);
                            }
                        });
                Launcher.LOGGER.info("Backup completed for directory: " + dir);
            } catch (IOException e) {
                Launcher.LOGGER.warn("Failed to backup directory: " + dir);
            }
        }
    }

    private void restoreFile(Path path) throws IOException {
        byte[] backup = fileBackups.get(path);
        if (backup != null) {
            Files.createDirectories(path.getParent());
            Files.write(path, backup, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Launcher.LOGGER.info("Restored file from backup: " + path);
        } else {
            Launcher.LOGGER.warn("No backup available to restore: " + path);
        }
    }

    private void registerDirectoryRecursively(Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                try {
                    dir.register(watchService, EVENT_KINDS);
                    Launcher.LOGGER.info("Registered directory for monitoring: " + dir);
                } catch (IOException e) {
                    Launcher.LOGGER.warn("Failed to register directory: " + dir);
                    throw e;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void applyReadOnlyRecursively(Path path) {
        try {
            if (Files.isDirectory(path)) {
                Files.walk(path).forEach(this::setReadOnlyAcl);
            } else {
                setReadOnlyAcl(path);
            }
        } catch (IOException e) {
            Launcher.LOGGER.warn("Failed to apply read-only recursively for: " + path + " due to: " + e.getMessage());
        }
    }

    private void setReadOnlyAcl(Path path) {
        try {
            AclFileAttributeView aclView = Files.getFileAttributeView(path, AclFileAttributeView.class);
            if (aclView != null) {
                UserPrincipal everyone = path.getFileSystem()
                        .getUserPrincipalLookupService()
                        .lookupPrincipalByName("Everyone");

                AclEntry denyAll = AclEntry.newBuilder()
                        .setType(AclEntryType.DENY)
                        .setPrincipal(everyone)
                        .setPermissions(
                                AclEntryPermission.WRITE_DATA,
                                AclEntryPermission.APPEND_DATA,
                                AclEntryPermission.DELETE,
                                AclEntryPermission.DELETE_CHILD,
                                AclEntryPermission.WRITE_ATTRIBUTES,
                                AclEntryPermission.WRITE_ACL,
                                AclEntryPermission.WRITE_OWNER
                        )
                        .build();

                List<AclEntry> acl = new ArrayList<>(aclView.getAcl());
                acl.add(0, denyAll); // DENY должен быть первым
                aclView.setAcl(acl);

                Launcher.LOGGER.info("Read-only ACL applied to: " + path);
            } else {
                Launcher.LOGGER.warn("ACL view is not supported for: " + path);
            }
        } catch (IOException e) {
            Launcher.LOGGER.warn("Failed to set read-only ACL for: " + path + " due to: " + e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        stop();
        watchService.close();
    }

    /**
     * Class representing a file system event.
     */
    public static final class Event {
        private final WatchEvent.Kind<?> kind;
        private final Path path;

        public Event(WatchEvent.Kind<?> kind, Path path) {
            this.kind = kind;
            this.path = path;
        }

        public WatchEvent.Kind<?> getKind() {
            return kind;
        }

        public Path getPath() {
            return path;
        }

        @Override
        public String toString() {
            return "Event{kind=" + kind + ", path=" + path + "}";
        }
    }
}
