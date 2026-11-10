package org.mcaccess.prism;

/**
 * Callback invoked by PRISM's asynchronous logger when a log message is emitted.
 */
@FunctionalInterface
public interface LogCallback {
    /**
     * Called when a log message is emitted by PRISM.
     *
     * @param level   the severity level of the log entry
     * @param source  the subsystem or component that produced the message
     * @param message the log message text
     */
    void onLog(LogLevel level, String source, String message);
}
