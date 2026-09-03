package org.mcaccess.prism;

import org.mcaccess.prism.natives.NativeLoader;
import org.mcaccess.prism.natives.PrismLogCallback;
import org.mcaccess.prism.natives.PrismLogHandler;
import org.mcaccess.prism.natives.prism_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

/**
 * Exposes prism's logging. All of these are safe to call from any thread, and before a {@link Context} is created or after it is closed.
 */
public final class PrismLog {

    /**
     * Severity levels, ordered from most to least verbose. {@link #NONE} silences logging entirely.
     */
    public enum Level {
        TRACE(0),
        DEBUG(1),
        INFO(2),
        WARN(3),
        ERROR(4),
        NONE(5);

        private final int code;

        Level(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        private static final Level[] BY_CODE = values();

        /**
         * Maps a native {@code PrismLogLevel} to a Level.
         * <p>
         * Codes are contiguous from 0 so a constant's code is its index. If the code is not known, returns None.
         */
        static Level fromCode(int code) {
            return (code >= 0 && code < BY_CODE.length) ? BY_CODE[code] : NONE;
        }
    }

    /**
     * In the Prism documentation it is said that replacing a listener does not immediately stop sending messages that were queued. We also do not have a way to check whether everything drained.
     * Thus, we choose an automatic arena with a list of listener segments to ensure that they are not dropped, crashing the process. This should not grow by any significant magnitude.
     */
    private static final Arena STUB_ARENA = Arena.ofAuto();
    private static final List<MemorySegment> INSTALLED = new ArrayList<>();

    /**
     * Routes PRISM's diagnostic output to {@code listener}.
     * <p>
     * Prism will not log anything without setting a listener.
     * Typical usage:
     *
     * <pre>{@code
     * PrismLog.setListener((level, source, message) -> {
     *     switch (level) {
     *         case ERROR -> LOGGER.error("[prism/{}] {}", source, message);
     *         case WARN  -> LOGGER.warn("[prism/{}] {}", source, message);
     *         default    -> LOGGER.info("[prism/{}] {}", source, message);
     *     }
     * });
     * }</pre>
     *
     * The listener is called from PRISM's logging thread and must not call back into {@link PrismLog}.
     *
     * @param listener Receives each message, or null to stop delivery.
     */
    public static synchronized void setListener(PrismLogListener listener) {
        NativeLoader.load();
        MemorySegment handler = PrismLogHandler.allocate(STUB_ARENA);
        INSTALLED.add(handler);

        if (listener == null) {
            PrismLogHandler.fn(handler, MemorySegment.NULL);
        } else {
            MemorySegment stub = PrismLogCallback.allocate(
                    (userdata, level, source, message) -> deliver(listener, level, source, message), STUB_ARENA);
            INSTALLED.add(stub);
            PrismLogHandler.fn(handler, stub);
        }
        PrismLogHandler.userdata(handler, MemorySegment.NULL);

        // in prism_set_log_handler, The previous handler is returned by value and dropped, so capture it in a temporary discarded arena.
        try (Arena discarded = Arena.ofConfined()) {
            prism_h.prism_set_log_handler(discarded, handler);
        }
    }

    private static void deliver(PrismLogListener listener, int level, MemorySegment source, MemorySegment message) {
        try {
            listener.onLog(Level.fromCode(level), Backend.readCString(source), Backend.readCString(message));
        } catch (Throwable t) {
            // Must not propagate into PRISM's logging thread, and must not be reported through PRISM.
            Thread current = Thread.currentThread();
            Thread.UncaughtExceptionHandler h = current.getUncaughtExceptionHandler();
            if (h != null) {
                h.uncaughtException(current, t);
            }
        }
    }

    /**
     * Stops delivery of PRISM's diagnostic output. Equivalent to {@code setListener(null)}.
     */
    public static void clearListener() {
        setListener(null);
    }

    /**
     * Sets the minimum level PRISM will emit.
     *
     * @return The level that was previously in effect.
     */
    public static Level setLevel(Level level) {
        if (level == null) {
            throw new NullPointerException("level must not be null");
        }
        NativeLoader.load();
        return Level.fromCode(prism_h.prism_set_log_level(level.getCode()));
    }

    /**
     * Writes a message into PRISM's log, so application events interleave with PRISM's own.
     *
     * @param level   The severity to record it at.
     * @param source  A short tag identifying the origin, for example the mod id.
     * @param message The message.
     */
    public static void log(Level level, String source, String message) {
        if (level == null) {
            throw new NullPointerException("level must not be null");
        }
        if (source == null) {
            throw new NullPointerException("source must not be null");
        }
        if (message == null) {
            throw new NullPointerException("message must not be null");
        }
        NativeLoader.load();
        try (Arena arena = Arena.ofConfined()) {
            prism_h.prism_log(level.getCode(), arena.allocateFrom(source), arena.allocateFrom(message));
        }
    }

    /**
     * Blocks until everything already queued has been written. PRISM logs on an internal thread, so a crash can otherwise lose the last few messages.
     */
    public static void flush() {
        NativeLoader.load();
        prism_h.prism_log_flush();
    }

    private PrismLog() {
    }
}
