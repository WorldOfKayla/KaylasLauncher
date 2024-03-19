package org.foxesworld;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.jar.JarFile;

public final class LauncherAgent {
    public static Instrumentation inst = null;
    public static Path libraries = null;
    private static boolean isStarted = false;

    public static boolean isAgentStarted() {
        return isStarted;
    }

    public static void premain(String agentArgument, Instrumentation inst) {
        System.out.println("" +
                "  ______ _           _                 \n" +
                " |  ____| |         (_)                \n" +
                " | |__  | |_   _ ___ _ _   _ _ __ ___  \n" +
                " |  __| | | | | / __| | | | | '_ ` _ \\ \n" +
                " | |____| | |_| \\__ \\ | |_| | | | | | |\n" +
                " |______|_|\\__, |___/_|\\__,_|_| |_| |_|\n" +
                "            __/ |                      \n" +
                "           |___/              by Foxesworld");
        LauncherAgent.inst = inst;
        libraries = Paths.get(Optional.ofNullable(agentArgument).map(String::trim).filter(e -> !e.isEmpty()).orElse("libraries"));
        isStarted = true;
        try {
            Files.walkFileTree(libraries, Collections.singleton(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new StarterVisitor());
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    private static final class StarterVisitor extends SimpleFileVisitor<Path> {
        private static final Set<PosixFilePermission> DPERMS;

        static {
            Set<PosixFilePermission> perms = new HashSet<>(Arrays.asList(PosixFilePermission.values()));
            perms.remove(PosixFilePermission.OTHERS_WRITE);
            perms.remove(PosixFilePermission.GROUP_WRITE);
            DPERMS = Collections.unmodifiableSet(perms);
        }

        private final boolean fixLib;

        private StarterVisitor() {
            Path filef = LauncherAgent.libraries.resolve(".libraries_chmoded");
            this.fixLib = !Files.exists(filef) && !Boolean.getBoolean("launcher.noLibrariesPosixPermsFix");
            if (fixLib) {
                try {
                    Files.deleteIfExists(filef);
                    Files.createFile(filef);
                } catch (Throwable ignored) {
                }
            }
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (fixLib && Files.getFileAttributeView(file, PosixFileAttributeView.class) != null)
                Files.setPosixFilePermissions(file, DPERMS);
            if (file.toFile().getName().endsWith(".jar"))
                inst.appendToSystemClassLoaderSearch(new JarFile(file.toFile()));
            return super.visitFile(file, attrs);
        }
    }
}