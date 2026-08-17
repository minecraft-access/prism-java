package org.mcaccess.prism.natives;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Utility class responsible for extracting and loading the PRISM native libraries.
 */
public final class NativeLoader {
    private static volatile boolean loaded = false;
    private static final Object LOCK = new Object();

    private NativeLoader() {
    }

    /**
     * Loads the PRISM native library if it has not already been loaded.
     */
    public static void load() {
        if (loaded) {
            return;
        }
        synchronized (LOCK) {
            if (loaded) {
                return;
            }
            loadInternal();
            loaded = true;
        }
    }

    private static void loadInternal() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String osArch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

        String os;
        if (osName.contains("win")) {
            os = "windows";
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            os = "mac";
        } else if (osName.contains("linux") || osName.contains("unix") || osName.contains("sunos")) {
            os = "linux";
        } else {
            os = osName;
        }

        String arch;
        if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            arch = "aarch64";
        } else if (osArch.contains("amd64") || osArch.contains("x86_64") || osArch.contains("x64")) {
            arch = "amd64";
        } else {
            arch = osArch;
        }

        try {
            if ("windows".equals(os)) {
                loadWindows(arch);
            } else if ("linux".equals(os)) {
                loadLinux(arch);
            } else if ("mac".equals(os)) {
                loadMac();
            } else {
                System.loadLibrary("prism");
            }
        } catch (Throwable t) {
            try {
                System.loadLibrary("prism");
            } catch (Throwable fallbackError) {
                t.addSuppressed(fallbackError);
                throw new UnsatisfiedLinkError("Failed to load PRISM native library: " + t.getMessage());
            }
        }
    }

    private static void loadWindows(String arch) throws IOException {
        Path tempDir = getTempDir();
        String resourceDir = "/natives/windows/" + arch + "/";

        // Try extracting and loading tolk.dll first if present
        Path tolkPath = extractResource(resourceDir + "tolk.dll", tempDir.resolve("tolk.dll"));
        if (tolkPath != null) {
            try {
                System.load(tolkPath.toAbsolutePath().toString());
            } catch (Throwable ignored) {
            }
        }

        Path prismPath = extractResource(resourceDir + "prism.dll", tempDir.resolve("prism.dll"));
        if (prismPath != null) {
            System.load(prismPath.toAbsolutePath().toString());
        } else {
            System.loadLibrary("prism");
        }
    }

    private static void loadLinux(String arch) throws IOException {
        Path tempDir = getTempDir();
        String resourceDir = "/natives/linux/" + arch + "/";
        Path prismPath = extractResource(resourceDir + "libprism.so", tempDir.resolve("libprism.so"));
        if (prismPath != null) {
            System.load(prismPath.toAbsolutePath().toString());
        } else {
            System.loadLibrary("prism");
        }
    }

    private static void loadMac() throws IOException {
        Path tempDir = getTempDir();
        Path prismPath = extractResource("/natives/mac/libprism.dylib", tempDir.resolve("libprism.dylib"));
        if (prismPath != null) {
            System.load(prismPath.toAbsolutePath().toString());
        } else {
            System.loadLibrary("prism");
        }
    }

    private static Path extractResource(String resourcePath, Path target) throws IOException {
        try (InputStream in = NativeLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        }
    }

    private static Path getTempDir() throws IOException {
        Path tempDir = Files.createTempDirectory("prism_native_");
        tempDir.toFile().deleteOnExit();
        return tempDir;
    }
}
