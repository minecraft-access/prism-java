package org.mcaccess.prism;

import org.mcaccess.prism.natives.prism_h;

/**
 * Severity levels for PRISM internal and diagnostic logging.
 */
public enum LogLevel {
    /** Verbose trace-level diagnostics. */
    TRACE(prism_h.PRISM_LOG_LEVEL_TRACE()),
    /** Debug-level diagnostics. */
    DEBUG(prism_h.PRISM_LOG_LEVEL_DEBUG()),
    /** Informational messages. */
    INFO(prism_h.PRISM_LOG_LEVEL_INFO()),
    /** Warning messages. */
    WARN(prism_h.PRISM_LOG_LEVEL_WARN()),
    /** Error messages. */
    ERROR(prism_h.PRISM_LOG_LEVEL_ERROR()),
    /** Disables all log output. */
    NONE(prism_h.PRISM_LOG_LEVEL_NONE());

    private final int level;

    LogLevel(int level) {
        this.level = level;
    }

    /**
     * Gets the numeric C enum value for this log level.
     *
     * @return numeric log level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Converts a numeric C enum value to a {@link LogLevel}.
     *
     * @param level numeric log level
     * @return matching {@link LogLevel}, or {@link #NONE} if out of range
     */
    public static LogLevel fromLevel(int level) {
        return switch (level) {
            case 0 -> TRACE;
            case 1 -> DEBUG;
            case 2 -> INFO;
            case 3 -> WARN;
            case 4 -> ERROR;
            default -> NONE;
        };
    }
}
