package org.mcaccess.prism.natives;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Utility class responsible for extracting and loading the PRISM native libraries.
 */
public final class NativeLoader {
    /** System property naming the directory to extract native libraries into, overriding the platform cache. */
    public static final String NATIVE_DIR_PROPERTY = "org.mcaccess.prism.nativeDir";

    private static volatile boolean loaded = false;
    private static volatile Throwable loadFailure;
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
            try {
                loadInternal();
            } catch (Throwable t) {
                loadFailure = t;
                throw t;
            }
            loaded = true;
            loadFailure = null;
        }
    }

    /**
     * The failure that prevented the native library from loading, or null if it loaded or was never attempted.
     */
    public static Throwable getLoadFailure() {
        return loadFailure;
    }

    /**
     * Where a library is extracted to when it cannot be loaded in place.
     * <p>
     * {@value #NATIVE_DIR_PROPERTY} wins if set, so an application whose home directory is redirected or read-only,
     * as under Flatpak, Snap or the macOS App Sandbox, can place the library itself. Otherwise this is the platform's
     * cache location: extracted libraries are regenerable, and a version-scoped path there is shared by every
     * application using this binding rather than duplicated per process. Windows uses LOCALAPPDATA rather than
     * APPDATA because a native binary for one architecture must not roam to another machine.
     */
    static Path extractionDir(String os, String arch) {
        String override = System.getProperty(NATIVE_DIR_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }

        String home = System.getProperty("user.home", ".");
        Path cacheRoot;
        if ("windows".equals(os)) {
            String localAppData = System.getenv("LOCALAPPDATA");
            cacheRoot = (localAppData != null && !localAppData.isBlank())
                    ? Path.of(localAppData)
                    : Path.of(home, "AppData", "Local");
        } else if ("mac".equals(os)) {
            cacheRoot = Path.of(home, "Library", "Caches");
        } else {
            String xdgCache = System.getenv("XDG_CACHE_HOME");
            cacheRoot = (xdgCache != null && !xdgCache.isBlank())
                    ? Path.of(xdgCache)
                    : Path.of(home, ".cache");
        }
        return cacheRoot.resolve("prism-java").resolve(PrismBuildInfo.PRISM_VERSION).resolve(os + "-" + arch);
    }

    private static void loadInternal() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String osArch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

        String os = osName.contains("win") ? "windows"
                : (osName.contains("mac") || osName.contains("darwin")) ? "mac"
                        : (osName.contains("linux") || osName.contains("unix") || osName.contains("sunos")) ? "linux"
                                : osName;

        String arch = (osArch.contains("aarch64") || osArch.contains("arm64")) ? "aarch64"
                : (osArch.contains("amd64") || osArch.contains("x86_64") || osArch.contains("x64")) ? "amd64"
                        : osArch;

        String fileName = System.mapLibraryName("prism");
        String resourcePath = "mac".equals(os)
                ? "/natives/mac/" + fileName
                : "/natives/" + os + "/" + arch + "/" + fileName;

        try {
            URL resource = NativeLoader.class.getResource(resourcePath);
            if (resource == null) {
                throw new NoSuchFileException("Resource not found in classpath: " + resourcePath);
            }

            if ("file".equals(resource.getProtocol())) {
                System.load(Path.of(resource.toURI()).toAbsolutePath().toString());
                return;
            }

            Path dir = extractionDir(os, arch);
            Path target = dir.resolve(fileName);

            if (!Files.exists(target)) {
                Files.createDirectories(dir);
                Path tmp = Files.createTempFile(dir, fileName, ".tmp");

                // Use resource.openStream() so we don't have to query the classpath a second time
                try (InputStream in = resource.openStream()) {
                    Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);

                    try {
                        Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException e) {
                        // Fallback to non-atomic move
                        Files.move(tmp, target);
                    }
                } catch (FileAlreadyExistsException e) {
                    // Another JVM process won the race and created the file concurrently. Safe to ignore.
                } finally {
                    Files.deleteIfExists(tmp);
                }
            }
            System.load(target.toAbsolutePath().toString());
        } catch (Throwable extractError) {
            // Fallback to system library path if resource extraction failed or wasn't found
            try {
                System.loadLibrary("prism");
            } catch (Throwable fallbackError) {
                UnsatisfiedLinkError error = new UnsatisfiedLinkError(
                        "Failed to load PRISM native library. Extraction error: " + extractError.getMessage());
                error.initCause(extractError); // Preserves original extraction stack trace
                error.addSuppressed(fallbackError);
                throw error;
            }
        }
    }
}
