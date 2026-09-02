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
     * @return the load failure or {@code null}
     */
    public static Throwable getLoadFailure() {
        return NativeLoader.getLoadFailure();
    }
}
