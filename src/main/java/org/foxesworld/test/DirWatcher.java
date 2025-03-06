package org.foxesworld.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DirWatcher следит за изменениями в указанных директориях и пытается предотвратить их модификацию,
 * устанавливая атрибут "только для чтения" для файлов и папок.
 */
public class DirWatcher implements Runnable {

    private final List<Path> directories;
    private final WatchService watchService;
    private volatile boolean running = true;
    private final ExecutorService executor;

    /**
     * Конструктор принимает список директорий для защиты.
     *
     * @param directories список путей к директориям
     * @throws IOException если WatchService не удалось создать
     */
    public DirWatcher(List<Path> directories) throws IOException {
        this.directories = directories;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.executor = Executors.newSingleThreadExecutor();
        registerDirectories();
        // На старте применяем защиту: делаем файлы и каталоги только для чтения
        directories.forEach(this::applyReadOnlyRecursively);
    }

    /**
     * Регистрирует все указанные директории (и их поддиректории) в WatchService.
     */
    private void registerDirectories() throws IOException {
        for (Path dir : directories) {
            registerDirectoryRecursively(dir);
        }
    }

    /**
     * Рекурсивно регистрирует каталог и все его подкаталоги для отслеживания событий.
     *
     * @param start начальный каталог
     */
    private void registerDirectoryRecursively(Path start) throws IOException {
        Files.walk(start)
                .filter(Files::isDirectory)
                .forEach(dir -> {
                    try {
                        dir.register(watchService,
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_DELETE,
                                StandardWatchEventKinds.ENTRY_MODIFY);
                    } catch (IOException e) {
                        System.err.println("Не удалось зарегистрировать каталог: " + dir);
                        e.printStackTrace();
                    }
                });
    }

    /**
     * Основной цикл обработки событий.
     */
    @Override
    public void run() {
        while (running) {
            WatchKey key;
            try {
                key = watchService.take(); // блокирующий вызов
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
            Path dir = (Path) key.watchable();
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                // Пропускаем переполнение
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path filename = ev.context();
                Path fullPath = dir.resolve(filename);
                System.out.println("Обнаружено событие " + kind.name() + " на " + fullPath);
                // При любом событии пытаемся восстановить защиту – установить read-only рекурсивно.
                applyReadOnlyRecursively(fullPath);

                // Если создана новая директория – регистрируем её для отслеживания.
                if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(fullPath)) {
                    try {
                        registerDirectoryRecursively(fullPath);
                    } catch (IOException e) {
                        System.err.println("Не удалось зарегистрировать новую директорию: " + fullPath);
                        e.printStackTrace();
                    }
                }
            }
            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }

    /**
     * Запускает DirWatcher в отдельном потоке.
     */
    public void start() {
        executor.submit(this);
    }

    /**
     * Останавливает наблюдение и освобождает ресурсы.
     */
    public void stop() {
        running = false;
        executor.shutdownNow();
        try {
            watchService.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Рекурсивно устанавливает атрибут «только для чтения» для файла или каталога.
     *
     * @param path путь к файлу или каталогу
     */
    private void applyReadOnlyRecursively(Path path) {
        File file = path.toFile();
        if (file.exists()) {
            // Пробуем установить флаг только для чтения
            if (!file.setReadOnly()) {
                System.err.println("Не удалось установить атрибут только для чтения для: " + file.getAbsolutePath());
            }
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        applyReadOnlyRecursively(child.toPath());
                    }
                }
            }
        }
    }
}