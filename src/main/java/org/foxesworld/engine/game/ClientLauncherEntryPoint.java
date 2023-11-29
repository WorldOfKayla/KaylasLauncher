package org.foxesworld.engine.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ClientLauncherEntryPoint {
    public static List<Path> resolveClassPath(Path workDir) {
        try {
            return Files.walk(workDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".class"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            // Обработка ошибок, если не удалось получить пути
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // Пример метода main
    public static void main(String[] args) {


        // Ваши дальнейшие действия с systemClassPath
        // Например, создание URLClassLoader и использование его для загрузки классов
    }
}
