package org.foxesworld.engine;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

public final class JVMHelper {

    // MXBeans exports
    public static final RuntimeMXBean RUNTIME_MXBEAN = ManagementFactory.getRuntimeMXBean();
    public static final OperatingSystemMXBean OPERATING_SYSTEM_MXBEAN = ManagementFactory.getOperatingSystemMXBean();
    public final OS OS_TYPE = OS.byName(OPERATING_SYSTEM_MXBEAN.getName());
    public final int OS_BITS = getCorrectOSArch();
    private Engine engine;
    // System properties
    public final String OS_VERSION = OPERATING_SYSTEM_MXBEAN.getVersion();
    public final ARCH ARCH_TYPE = getArch(System.getProperty("os.arch"));
    public final String NATIVE_EXTENSION = getNativeExtension(OS_TYPE);
    public final String NATIVE_PREFIX = getNativePrefix(OS_TYPE);
    public final int JVM_BITS = Integer.parseInt(System.getProperty("sun.arch.data.model"));
    // Public static fields
    public final Runtime RUNTIME = Runtime.getRuntime();
    public final ClassLoader LOADER = ClassLoader.getSystemClassLoader();
    public final int JVM_VERSION = getVersion();
    public final int JVM_BUILD = getBuild();

    static {
        try {
            MethodHandles.publicLookup(); // Just to initialize class
        } catch (Throwable exc) {
            throw new InternalError(exc);
        }
    }

    JVMHelper(Engine engine) {
        this.engine = engine;
    }

    public ARCH getArch(String arch) {
        if (arch.equals("amd64") || arch.equals("x86-64") || arch.equals("x86_64")) return ARCH.X86_64;
        if (arch.equals("i386") || arch.equals("i686") || arch.equals("x86")) return ARCH.X86;
        if (arch.startsWith("armv8") || arch.startsWith("aarch64")) return ARCH.ARM64;
        if (arch.startsWith("arm") || arch.startsWith("aarch32")) return ARCH.ARM32;
        throw new InternalError(String.format("Unsupported arch '%s'", arch));
    }

    public int getVersion() {
        return Runtime.version().feature();
    }

    public int getBuild() {
        return Runtime.version().update();
    }

    public String getNativeExtension(JVMHelper.OS OS_TYPE) {
        switch (OS_TYPE) {
            case WIN:
                return ".dll";
            case LINUX:
                return ".so";
            case MACOSX:
                return ".dylib";
            default:
                throw new InternalError(String.format("Unsupported OS TYPE '%s'", OS_TYPE));
        }
    }

    public String getNativePrefix(JVMHelper.OS OS_TYPE) {
        switch (OS_TYPE) {
            case LINUX:
            case MACOSX:
                return "lib";
            default:
                return "";
        }
    }

    public void appendVars(ProcessBuilder builder, Map<String, String> vars) {
        builder.environment().putAll(vars);
    }

    public Class<?> firstClass(String... names) throws ClassNotFoundException {
        for (String name : names)
            try {
                return Class.forName(name, false, LOADER);
            } catch (ClassNotFoundException ignored) {
                // Expected
            }
        throw new ClassNotFoundException(Arrays.toString(names));
    }

    public void fullGC() {
        RUNTIME.gc();
        engine.getLOGGER().debug("Used heap: %d MiB", RUNTIME.totalMemory() - RUNTIME.freeMemory() >> 20);
    }

    public static String[] getClassPath() {
        return System.getProperty("java.class.path").split(File.pathSeparator);
    }

    public URL[] getClassPathURL() {
        String[] cp = System.getProperty("java.class.path").split(File.pathSeparator);
        URL[] list = new URL[cp.length];

        for (int i = 0; i < cp.length; i++) {
            URL url = null;
            try {
                url = new URL(cp[i]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            list[i] = url;
        }
        return list;
    }

    public X509Certificate[] getCertificates(Class<?> clazz) {
        Object[] signers = clazz.getSigners();
        if (signers == null) return null;
        return Arrays.stream(signers).filter((c) -> c instanceof X509Certificate).map((c) -> (X509Certificate) c).toArray(X509Certificate[]::new);
    }

    public void checkStackTrace(Class<?> mainClass) {
        engine.getLOGGER().debug("Testing stacktrace");
        Exception e = new Exception("Testing stacktrace");
        StackTraceElement[] list = e.getStackTrace();
        if (!list[list.length - 1].getClassName().equals(mainClass.getName())) {
            throw new SecurityException(String.format("Invalid StackTraceElement: %s", list[list.length - 1].getClassName()));
        }
    }

    private int getCorrectOSArch() {
        if (OS_TYPE == OS.WIN)
            return System.getenv("ProgramFiles(x86)") == null ? 32 : 64;

        // Or trust system property (maybe incorrect)
        return System.getProperty("os.arch").contains("64") ? 64 : 32;
    }

    public static String getEnvPropertyCaseSensitive(String name) {
        return System.getenv().get(name);
    }

    public boolean isJVMMatchesSystemArch() {
        return JVM_BITS == OS_BITS;
    }

    public String jvmProperty(String name, String value) {
        return String.format("-D%s=%s", name, value);
    }

    public String systemToJvmProperty(String name) {
        return String.format("-D%s=%s", name, System.getProperties().getProperty(name));
    }

    public static void addSystemPropertyToArgs(Collection<String> args, String name) {
        String property = System.getProperty(name);
        if (property != null)
            args.add(String.format("-D%s=%s", name, property));
    }

    public void verifySystemProperties(Class<?> mainClass, boolean requireSystem) {
        Locale.setDefault(Locale.US);
        // Verify class loader
        engine.getLOGGER().debug("Verifying class loader");
        if (requireSystem && !mainClass.getClassLoader().equals(LOADER))
            throw new SecurityException("ClassLoader should be system");

        // Verify system and java architecture
        engine.getLOGGER().debug("Verifying JVM architecture");
    }

    public enum ARCH {
        X86("x86"), X86_64("x86-64"), ARM64("arm64"), ARM32("arm32");

        public final String name;

        ARCH(String name) {
            this.name = name;
        }
    }

    public enum OS {
        WIN("win"), LINUX("linux"), MACOSX("macosx");

        public final String name;

        OS(String name) {
            this.name = name;
        }

        public static OS byName(String name) {
            if (name.startsWith("Windows"))
                return WIN;
            if (name.startsWith("Linux"))
                return LINUX;
            if (name.startsWith("Mac OS X"))
                return MACOSX;
            throw new RuntimeException(String.format("Not supported: '%s'", name));
        }
    }
}