package org.foxesworld.engine.gui.components.game;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class TransformClassLoader extends ClassLoader {
    private final File[] classpath;

    public TransformClassLoader(File[] classpath, ClassLoader parent) {
        super(parent);
        this.classpath = classpath;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] classBytes = loadClassBytes(name);
        if (classBytes == null) {
            throw new ClassNotFoundException(name);
        } else {
            //logger.info("Loaded class: " + name);
            return defineClass(name, classBytes, 0, classBytes.length);
        }
    }

    private byte[] loadClassBytes(String className) {
        String path = className.replace('.', File.separatorChar) + ".class";
        for (File entry : classpath) {
            File classFile = new File(entry, path);
            if (classFile.exists()) {
                try {
                    //logger.debug("Loading class file: " + classFile.getAbsolutePath());
                    return loadFileBytes(classFile);
                } catch (IOException e) {
                    //logger.error("Error loading class file: " + classFile.getAbsolutePath(), e);
                }
            }
        }
        return null;
    }

    private byte[] loadFileBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            long length = file.length();
            if (length > Integer.MAX_VALUE) {
                throw new IOException("File " + file.getName() + " is too large");
            }

            byte[] bytes = new byte[(int) length];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead = fis.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            if (offset < bytes.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }

            return bytes;
        }
    }
}
