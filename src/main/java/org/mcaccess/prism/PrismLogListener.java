package org.mcaccess.prism;

/**
 * Receives PRISM's internal diagnostic messages.
 * <p>
 * PRISM discards its own log output until a listener is installed, so this is the only way to see what it is logging. Install one with {@link PrismLog#setListener(PrismLogListener)}.
 * <p>
 * Invoked from PRISM's logging thread. Avoid invoking it concurrently with itself, or from a thread the application owns.
 * Implementations must synchronise any shared state they touch, and must not call back into {@link PrismLog} or any other PRISM logging function.
 */
@FunctionalInterface
public interface PrismLogListener {
    /**
     * @param level   Severity of the message.
     * @param source  The PRISM subsystem that emitted it.
     * @param message The message text.
     */
    void onLog(PrismLog.Level level, String source, String message);
}
