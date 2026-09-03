package org.mcaccess.prism;

/**
 * Notified when a backend's runtime availability changes.
 * A call is a notification that the application's cached choice of backend may be stale. It does not change any backend instance the application already holds
 */
@FunctionalInterface
public interface AvailabilityListener {
    /**
     * @param backend   The identifier of the backend whose availability changed.
     * @param name      The backend name, for example "SAPI", "NVDA" or "OneCore".
     * @param available True if the backend became available, false if it became unavailable.
     */
    void onAvailabilityChanged(BackendId backend, String name, boolean available);
}
