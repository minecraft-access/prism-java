package org.mcaccess.prism;

import org.mcaccess.prism.natives.NativeLoader;
import org.mcaccess.prism.natives.PrismLogCallback;
import org.mcaccess.prism.natives.PrismLogHandler;
import org.mcaccess.prism.natives.prism_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Main entry point and utility methods for the PRISM speech and screen reader library.
 */
public final class Prism {
    private static volatile LogCallback logCallback;
    private static MemorySegment logStub;

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

    /**
     * Sets the minimum severity level for PRISM log messages.
     *
     * @param level minimum log level to emit (or {@link LogLevel#NONE} to disable logging)
     * @return the previous log level
     */
    public static LogLevel setLogLevel(LogLevel level) {
        Objects.requireNonNull(level, "level must not be null");
        NativeLoader.load();
        int prev = prism_h.prism_set_log_level(level.getLevel());
        return LogLevel.fromLevel(prev);
    }

    /**
     * Sets a custom callback to receive asynchronous PRISM log messages, or {@code null} to clear it.
     *
     * @param callback log callback, or {@code null} to disable custom log handling
     */
    public static synchronized void setLogCallback(LogCallback callback) {
        NativeLoader.load();
        logCallback = callback;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment handler = PrismLogHandler.allocate(arena);
            if (callback != null) {
                if (logStub == null) {
                    PrismLogCallback.Function fn = (userdata, level, sourcePtr, messagePtr) -> {
                        LogCallback cb = logCallback;
                        if (cb != null) {
                            try {
                                cb.onLog(
                                        LogLevel.fromLevel(level),
                                        Backend.readCString(sourcePtr),
                                        Backend.readCString(messagePtr)
                                );
                            } catch (Throwable ignored) {
                                // Never allow exceptions to escape an FFM upcall stub into the native logger thread
                            }
                        }
                    };
                    logStub = PrismLogCallback.allocate(fn, Arena.global());
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try {
                            flushLog();
                            shutdownLog();
                        } catch (Throwable ignored) {
                        }
                    }, "prism-log-shutdown"));
                }
                PrismLogHandler.fn(handler, logStub);
            } else {
                PrismLogHandler.fn(handler, MemorySegment.NULL);
            }
            PrismLogHandler.userdata(handler, MemorySegment.NULL);
            prism_h.prism_set_log_handler(arena, handler);
        }
    }

    /**
     * Submits a log message through PRISM's asynchronous logging subsystem.
     *
     * @param level   log severity level
     * @param source  component or subsystem name
     * @param message log message text
     */
    public static void log(LogLevel level, String source, String message) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(message, "message must not be null");
        if (source.isEmpty() || message.isEmpty()) {
            throw new PrismException.InvalidParam("Log source and message must not be empty");
        }
        if (source.indexOf('\0') >= 0 || message.indexOf('\0') >= 0) {
            throw new PrismException.InvalidParam("Log source and message must not contain embedded null characters");
        }
        NativeLoader.load();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment sourceSeg = arena.allocateFrom(source);
            MemorySegment messageSeg = arena.allocateFrom(message);
            prism_h.prism_log(level.getLevel(), sourceSeg, messageSeg);
        }
    }

    /**
     * Flushes all queued PRISM log messages and blocks until they have been delivered to the log handler.
     */
    public static void flushLog() {
        NativeLoader.load();
        prism_h.prism_log_flush();
    }

    /**
     * Shuts down PRISM's background logging thread after draining remaining queued messages.
     */
    public static void shutdownLog() {
        NativeLoader.load();
        prism_h.prism_log_shutdown();
    }
}
