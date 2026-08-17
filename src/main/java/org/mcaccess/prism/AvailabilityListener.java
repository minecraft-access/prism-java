package org.mcaccess.prism;

/**
 * Listener invoked when a backend's availability changes at runtime.
 */
@FunctionalInterface
public interface AvailabilityListener {
    /**
     * Called when a backend availability status changes.
     *
     * @param backend   the backend ID (or {@link BackendId#INVALID} if custom/unknown)
     * @param name      the backend name
     * @param available {@code true} if the backend is currently available, {@code false} otherwise
     */
    void onAvailabilityChanged(BackendId backend, String name, boolean available);
}
