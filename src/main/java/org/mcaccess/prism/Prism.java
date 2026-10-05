package org.mcaccess.prism;

import org.mcaccess.prism.natives.NativeLoader;
import org.mcaccess.prism.natives.prism_h;

import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;

/**
 * Main entry point and utility methods for the PRISM speech and screen reader library.
 */
public final class Prism {
    private Prism() {
    }

    /**
     * Ensures that the native PRISM library has been unpacked and loaded into the JVM.
     */
    public static void load() {
        NativeLoader.load();
    }

    /**
     * Creates and initializes a new default {@link Context}.
     *
     * @return newly initialized PRISM context
     */
    public static Context createContext() {
        return new Context();
    }

    /**
     * Creates and initializes a new {@link Context} configured using the given builder action.
     *
     * @param configurer consumer configuring the context builder
     * @return newly initialized PRISM context
     */
    public static Context createContext(Consumer<Context.Builder> configurer) {
        return new Context(configurer);
    }

    /**
     * Gets the human-readable description for a PRISM error code.
     *
     * @param errorCode the numeric error code
     * @return human-readable error description
     */
    public static String getErrorString(int errorCode) {
        NativeLoader.load();
        MemorySegment ptr = prism_h.prism_error_string(errorCode);
        String msg = Backend.readCString(ptr);
        return msg.isEmpty() ? "Unknown error (" + errorCode + ")" : msg;
    }

    /**
     * Checks whether PRISM native libraries can be successfully loaded on this system.
     *
     * @return {@code true} if PRISM is available, {@code false} otherwise
     */
    public static boolean isAvailable() {
        try {
            NativeLoader.load();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Gets the runtime version integer of the loaded native PRISM library.
     *
     * @return runtime version packed integer
     */
    public static int getVersion() {
        NativeLoader.load();
        return prism_h.prism_version();
    }

    /**
     * Gets the runtime version string of the loaded native PRISM library (e.g. "0.18.3").
     *
     * @return human-readable version string
     */
    public static String getVersionString() {
        NativeLoader.load();
        MemorySegment ptr = prism_h.prism_version_string();
        return Backend.readCString(ptr);
    }

    /**
     * Gets the compile-time major version of the PRISM C API headers.
     *
     * @return compile-time major version
     */
    public static int getCompileVersionMajor() {
        return prism_h.PRISM_VERSION_MAJOR();
    }

    /**
     * Gets the compile-time minor version of the PRISM C API headers.
     *
     * @return compile-time minor version
     */
    public static int getCompileVersionMinor() {
        return prism_h.PRISM_VERSION_MINOR();
    }

    /**
     * Gets the compile-time patch version of the PRISM C API headers.
     *
     * @return compile-time patch version
     */
    public static int getCompileVersionPatch() {
        return prism_h.PRISM_VERSION_PATCH();
    }

    /**
     * Gets the compile-time version string of the PRISM C API headers.
     *
     * @return compile-time version string
     */
    public static String getCompileVersionString() {
        return Backend.readCString(prism_h.PRISM_VERSION_STRING());
    }

    /**
     * Checks whether automatic power management for availability polling is supported on this platform.
     *
     * @return {@code true} if supported, {@code false} otherwise
     */
    public static boolean isAvailabilityAutoPowerSupported() {
        NativeLoader.load();
        return prism_h.prism_availability_auto_power_supported();
    }
}
