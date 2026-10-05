package org.mcaccess.prism;

import org.mcaccess.prism.natives.NativeLoader;
import org.mcaccess.prism.natives.PrismAvailabilityBaselineCallback;
import org.mcaccess.prism.natives.PrismAvailabilityCallback;
import org.mcaccess.prism.natives.PrismConfig;
import org.mcaccess.prism.natives.prism_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The PRISM context manages backend registration and lifecycle.
 */
public final class Context implements AutoCloseable {
    private final MemorySegment handle;
    private final Arena arena;
    private volatile boolean closed = false;

    /**
     * Initializes a new default PRISM context.
     */
    public Context() {
        this(new Builder());
    }

    /**
     * Initializes a new PRISM context configured via the given consumer.
     *
     * @param configurer consumer to configure the context builder
     */
    public Context(Consumer<Builder> configurer) {
        this(buildFromConfigurer(configurer));
    }

    private static Builder buildFromConfigurer(Consumer<Builder> configurer) {
        Objects.requireNonNull(configurer, "configurer must not be null");
        Builder builder = new Builder();
        configurer.accept(builder);
        return builder;
    }

    private Context(Builder builder) {
        NativeLoader.load();
        this.arena = Arena.ofShared();
        MemorySegment configSeg = prism_h.prism_config_init(arena);

        if (builder.availabilityListener != null) {
            PrismAvailabilityCallback.Function cb = (userdata, backendId, nameSeg, available) -> {
                String name = Backend.readCString(nameSeg);
                BackendId id = BackendId.fromId(backendId).orElse(BackendId.INVALID);
                builder.availabilityListener.onAvailabilityChanged(id, name, available);
            };
            MemorySegment stub = PrismAvailabilityCallback.allocate(cb, arena);
            PrismConfig.availability_callback(configSeg, stub);
        }
        if (builder.pollIntervalMs != null) {
            PrismConfig.availability_poll_interval_ms(configSeg, builder.pollIntervalMs);
        }
        if (builder.debounceSamples != null) {
            PrismConfig.availability_debounce_samples(configSeg, builder.debounceSamples);
        }
        if (builder.backoffMaxMs != null) {
            PrismConfig.availability_backoff_max_ms(configSeg, builder.backoffMaxMs);
        }
        if (builder.autoPowerManage != null) {
            PrismConfig.availability_auto_power_manage(configSeg, builder.autoPowerManage);
        }
        if (builder.baselineCallback != null) {
            PrismAvailabilityBaselineCallback.Function bcb = (userdata) -> builder.baselineCallback.run();
            MemorySegment bStub = PrismAvailabilityBaselineCallback.allocate(bcb, arena);
            PrismConfig.availability_baseline_callback(configSeg, bStub);
        }

        this.handle = prism_h.prism_init(configSeg);
        if (this.handle == null || this.handle.equals(MemorySegment.NULL)) {
            arena.close();
            throw new PrismException.NotInitialized("PRISM could not be initialized");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the raw native memory segment handle for this context.
     *
     * @return native context handle
     */
    public MemorySegment handle() {
        checkClosed();
        return handle;
    }

    /**
     * Gets the number of registered backends in this context.
     *
     * @return count of backends
     */
    public int getBackendsCount() {
        checkClosed();
        return (int) prism_h.prism_registry_count(handle);
    }

    /**
     * Gets the backend ID for the backend at the specified index.
     *
     * @param index 0-based index in the backend registry
     * @return backend ID
     */
    public BackendId getIdOf(int index) {
        checkClosed();
        long id = prism_h.prism_registry_id_at(handle, index);
        if (id == 0) {
            throw new IndexOutOfBoundsException("Invalid backend index: " + index);
        }
        return BackendId.fromId(id).orElse(BackendId.INVALID);
    }

    /**
     * Gets the backend ID for the backend with the given name.
     *
     * @param name backend name
     * @return backend ID
     */
    public BackendId getIdOf(String name) {
        return findIdOf(name).orElseThrow(() -> new IllegalArgumentException("No backend named '" + name + "'"));
    }

    /**
     * Finds the backend ID for the backend with the given name.
     *
     * @param name backend name
     * @return optional containing the backend ID if found
     */
    public Optional<BackendId> findIdOf(String name) {
        checkClosed();
        Objects.requireNonNull(name, "name must not be null");
        try (Arena localArena = Arena.ofConfined()) {
            MemorySegment nameSeg = localArena.allocateFrom(name);
            long id = prism_h.prism_registry_id(handle, nameSeg);
            if (id == 0) {
                return Optional.empty();
            }
            return Optional.of(BackendId.fromId(id).orElse(BackendId.INVALID));
        }
    }

    /**
     * Gets the name of the backend identified by the given backend ID.
     *
     * @param id backend ID
     * @return backend name
     */
    public String getNameOf(BackendId id) {
        Objects.requireNonNull(id, "id must not be null");
        return getNameOf(id.getId());
    }

    /**
     * Gets the name of the backend identified by the given 64-bit ID.
     *
     * @param id 64-bit backend ID
     * @return backend name
     */
    public String getNameOf(long id) {
        checkClosed();
        MemorySegment ptr = prism_h.prism_registry_name(handle, id);
        if (ptr == null || ptr.equals(MemorySegment.NULL)) {
            throw new IllegalArgumentException("Backend ID not found: 0x" + Long.toHexString(id));
        }
        return Backend.readCString(ptr);
    }

    /**
     * Gets the priority of the backend with the given ID.
     *
     * @param id backend ID
     * @return priority integer (higher means preferred)
     */
    public int getPriorityOf(BackendId id) {
        Objects.requireNonNull(id, "id must not be null");
        return getPriorityOf(id.getId());
    }

    /**
     * Gets the priority of the backend with the given 64-bit ID.
     *
     * @param id 64-bit backend ID
     * @return priority integer
     */
    public int getPriorityOf(long id) {
        checkClosed();
        return prism_h.prism_registry_priority(handle, id);
    }

    /**
     * Checks if a backend with the given ID exists in the registry.
     *
     * @param id backend ID
     * @return {@code true} if exists, {@code false} otherwise
     */
    public boolean exists(BackendId id) {
        Objects.requireNonNull(id, "id must not be null");
        return exists(id.getId());
    }

    /**
     * Checks if a backend with the given 64-bit ID exists in the registry.
     *
     * @param id 64-bit backend ID
     * @return {@code true} if exists, {@code false} otherwise
     */
    public boolean exists(long id) {
        checkClosed();
        return prism_h.prism_registry_exists(handle, id);
    }

    /**
     * Creates a new owned instance of the backend with the specified ID.
     *
     * @param id backend ID
     * @return newly created Backend instance
     */
    public Backend create(BackendId id) {
        Objects.requireNonNull(id, "id must not be null");
        return create(id.getId());
    }

    /**
     * Creates a new owned instance of the backend with the specified 64-bit ID.
     *
     * @param id 64-bit backend ID
     * @return newly created Backend instance
     */
    public Backend create(long id) {
        checkClosed();
        MemorySegment ptr = prism_h.prism_registry_create(handle, id);
        if (ptr == null || ptr.equals(MemorySegment.NULL)) {
            throw new PrismException.InvalidParam("Invalid or unsupported backend: 0x" + Long.toHexString(id));
        }
        return new Backend(ptr, true);
    }

    /**
     * Creates a new owned instance of the highest priority available backend.
     *
     * @return newly created Backend instance
     */
    public Backend createBest() {
        checkClosed();
        MemorySegment ptr = prism_h.prism_registry_create_best(handle);
        if (ptr == null || ptr.equals(MemorySegment.NULL)) {
            throw new PrismException.BackendNotAvailable("No suitable PRISM backend available on this system");
        }
        return new Backend(ptr, true);
    }

    /**
     * Acquires a shared instance of the backend with the specified ID.
     *
     * @param id backend ID
     * @return acquired Backend instance
     */
    public Backend acquire(BackendId id) {
        Objects.requireNonNull(id, "id must not be null");
        return acquire(id.getId());
    }

    /**
     * Acquires a shared instance of the backend with the specified 64-bit ID.
     *
     * @param id 64-bit backend ID
     * @return acquired Backend instance
     */
    public Backend acquire(long id) {
        checkClosed();
        MemorySegment ptr = prism_h.prism_registry_acquire(handle, id);
        if (ptr == null || ptr.equals(MemorySegment.NULL)) {
            throw new PrismException.InvalidParam("Invalid or unsupported backend: 0x" + Long.toHexString(id));
        }
        return new Backend(ptr, false);
    }

    /**
     * Acquires a shared instance of the highest priority available backend.
     *
     * @return acquired Backend instance
     */
    public Backend acquireBest() {
        checkClosed();
        MemorySegment ptr = prism_h.prism_registry_acquire_best(handle);
        if (ptr == null || ptr.equals(MemorySegment.NULL)) {
            throw new PrismException.BackendNotAvailable("No suitable PRISM backend available on this system");
        }
        return new Backend(ptr, false);
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            if (handle != null && !handle.equals(MemorySegment.NULL)) {
                prism_h.prism_shutdown(handle);
            }
            arena.close();
        }
    }

    public boolean isClosed() {
        return closed;
    }

    /**
     * Pauses background availability polling for this context.
     */
    public void pauseAvailabilityPolling() {
        checkClosed();
        prism_h.prism_availability_poll_pause(handle);
    }

    /**
     * Resumes background availability polling for this context.
     */
    public void resumeAvailabilityPolling() {
        checkClosed();
        prism_h.prism_availability_poll_resume(handle);
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Context is already closed");
        }
    }

    /**
     * Builder for configuring and creating a {@link Context}.
     */
    public static final class Builder {
        private AvailabilityListener availabilityListener;
        private Integer pollIntervalMs;
        private Integer debounceSamples;
        private Integer backoffMaxMs;
        private Boolean autoPowerManage;
        private Runnable baselineCallback;

        /**
         * Sets an availability listener to receive callbacks when backend availability changes.
         *
         * @param listener the availability listener
         * @return this builder
         */
        public Builder withAvailabilityListener(AvailabilityListener listener) {
            this.availabilityListener = listener;
            return this;
        }

        /**
         * Sets the backend availability polling interval in milliseconds.
         *
         * @param pollIntervalMs polling interval in milliseconds
         * @return this builder
         */
        public Builder withPollIntervalMs(int pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
            return this;
        }

        /**
         * Sets the number of debounce samples for backend availability checks.
         *
         * @param debounceSamples number of samples
         * @return this builder
         */
        public Builder withDebounceSamples(int debounceSamples) {
            this.debounceSamples = debounceSamples;
            return this;
        }

        /**
         * Sets the maximum backoff interval in milliseconds for availability polling.
         *
         * @param backoffMaxMs maximum backoff in milliseconds
         * @return this builder
         */
        public Builder withBackoffMaxMs(int backoffMaxMs) {
            this.backoffMaxMs = backoffMaxMs;
            return this;
        }

        /**
         * Enables or disables automatic power management for availability polling.
         *
         * @param autoPowerManage {@code true} to enable auto power management
         * @return this builder
         */
        public Builder withAutoPowerManage(boolean autoPowerManage) {
            this.autoPowerManage = autoPowerManage;
            return this;
        }

        /**
         * Sets a callback invoked when the initial availability baseline has completed.
         *
         * @param callback baseline completion callback
         * @return this builder
         */
        public Builder withBaselineCallback(Runnable callback) {
            this.baselineCallback = callback;
            return this;
        }

        public Context build() {
            return new Context(this);
        }
    }
}
