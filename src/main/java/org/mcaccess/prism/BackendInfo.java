package org.mcaccess.prism;

/**
 * Describes a backend registered with PRISM.
 * <p>
 * Whether a backend is usable at this moment is runtime availability, which is reported through {@link AvailabilityListener} and can be read from a live backend through {@link Backend#getFeatures()} and {@link BackendFeature#IS_SUPPORTED_AT_RUNTIME}.
 *
 * @param id       The backend's identifier.
 * @param name     The human-readable name, for example "NVDA" or "SAPI".
 * @param priority The backend's priority; higher is more preferred.
 */
public record BackendInfo(BackendId id, String name, int priority) {
}
