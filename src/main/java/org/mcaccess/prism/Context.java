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
import java.util.OptionalLong;
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
        if (builder.availabilityListener == null) {
            this.arena = null;
            this.handle = prism_h.prism_init(MemorySegment.NULL);
            if (Backend.isNull(this.handle)) {
                throw new PrismException.NotInitialized("PRISM could not be initialized");
            }
            return;
        }

        this.arena = Arena.ofShared();
        try (Arena configArena = Arena.ofConfined()) {
            MemorySegment configSeg = prism_h.prism_config_init(configArena);

            AvailabilityListener listener = builder.availabilityListener;
            PrismAvailabilityCallback.Function cb = (userdata, backendId, nameSeg, available) -> {
                try {
                    String name = Backend.readCString(nameSeg);
                    BackendId id = BackendId.fromId(backendId).orElse(BackendId.INVALID);
                    listener.onAvailabilityChanged(id, name, available);
                } catch (Throwable ignored) {
                    // Never allow exceptions to escape an FFM upcall stub into native polling thread
                }
            };
            MemorySegment stub = PrismAvailabilityCallback.allocate(cb, this.arena);
            PrismConfig.availability_callback(configSeg, stub);

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
                Runnable baseline = builder.baselineCallback;
                PrismAvailabilityBaselineCallback.Function bcb = (userdata) -> {
                    try {
                        baseline.run();
                    } catch (Throwable ignored) {
                        // Never allow exceptions to escape an FFM upcall stub into native polling thread
                    }
                };
                MemorySegment bStub = PrismAvailabilityBaselineCallback.allocate(bcb, this.arena);
                PrismConfig.availability_baseline_callback(configSeg, bStub);
            }

            this.handle = prism_h.prism_init(configSeg);
            if (Backend.isNull(this.handle)) {
                this.arena.close();
                throw new PrismException.NotInitialized("PRISM could not be initialized");
            }
        }
    }

    /**
     * Creates a new {@link Builder} for configuring a {@link Context}.
     *
     * @return new context builder
     */
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
        return BackendId.fromId(getRawIdOf(index)).orElse(BackendId.INVALID);
    }

    /**
     * Gets the raw 64-bit backend ID for the backend at the specified index.
     *
     * @param index 0-based index in the backend registry
     * @return 64-bit backend ID
     */
    public long getRawIdOf(int index) {
        checkClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Invalid backend index: " + index);
        }
        long id = prism_h.prism_registry_id_at(handle, index);
        if (id == 0) {
            throw new IndexOutOfBoundsException("Invalid backend index: " + index);
        }
        return id;
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
        OptionalLong rawId = findRawIdOf(name);
        if (rawId.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(BackendId.fromId(rawId.getAsLong()).orElse(BackendId.INVALID));
    }

    /**
     * Finds the raw 64-bit backend ID for the backend with the given name.
     *
     * @param name backend name
     * @return optional containing the 64-bit backend ID if found
     */
    public OptionalLong findRawIdOf(String name) {
        checkClosed();
        Objects.requireNonNull(name, "name must not be null");
        if (name.isEmpty() || name.indexOf('\0') >= 0) {
            return OptionalLong.empty();
        }
        try (Arena localArena = Arena.ofConfined()) {
            MemorySegment nameSeg = localArena.allocateFrom(name);
            long id = prism_h.prism_registry_id(handle, nameSeg);
            if (id == 0) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(id);
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
        if (Backend.isNull(ptr)) {
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
     * Retrieves an existing shared backend instance if one is currently alive in the registry.
     * Unlike {@link #acquire(BackendId)}, this does not instantiate a new backend if none is currently active.
     *
     * @param id backend ID
     * @return optional containing the active shared Backend instance if present
     */
    public Optional<Backend> get(BackendId id) {
        Objects.requireNonNull(id, "id must not be null");
        return get(id.getId());
    }

    /**
     * Retrieves an existing shared backend instance by 64-bit ID if one is currently alive in the registry.
     * Unlike {@link #acquire(long)}, this does not instantiate a new backend if none is currently active.
     *
     * @param id 64-bit backend ID
     * @return optional containing the active shared Backend instance if present
     */
    public Optional<Backend> get(long id) {
        checkClosed();
        MemorySegment ptr = prism_h.prism_registry_get(handle, id);
        if (Backend.isNull(ptr)) {
            return Optional.empty();
        }
        return Optional.of(new Backend(ptr, true));
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
        if (Backend.isNull(ptr)) {
            throw new PrismException.InvalidParam("Invalid or unsupported backend: 0x" + Long.toHexString(id));
        }
        return new Backend(ptr, false);
    }

    /**
     * Creates a new owned instance of the highest priority available backend.
     *
     * @return newly created Backend instance
     */
    public Backend createBest() {
        checkClosed();
        MemorySegment ptr = prism_h.prism_registry_create_best(handle);
        if (Backend.isNull(ptr)) {
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
        if (Backend.isNull(ptr)) {
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
        if (Backend.isNull(ptr)) {
            throw new PrismException.BackendNotAvailable("No suitable PRISM backend available on this system");
        }
        return new Backend(ptr, true);
    }

    @Override
    public void close() {
        if (!closed) {
            synchronized (this) {
                if (!closed) {
                    closed = true;
                    if (!Backend.isNull(handle)) {
                        prism_h.prism_shutdown(handle);
                    }
                    if (arena != null) {
                        arena.close();
                    }
                }
            }
        }
    }

    /**
     * Checks whether this context has been closed.
     *
     * @return {@code true} if closed, {@code false} otherwise
     */
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
         * Creates a new context builder with default settings.
         */
        public Builder() {
        }

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
         * @param pollIntervalMs non-negative polling interval in milliseconds
         * @return this builder
         */
        public Builder withPollIntervalMs(int pollIntervalMs) {
            if (pollIntervalMs < 0) {
                throw new IllegalArgumentException("pollIntervalMs must be non-negative: " + pollIntervalMs);
            }
            this.pollIntervalMs = pollIntervalMs;
            return this;
        }

        /**
         * Sets the number of debounce samples for backend availability checks.
         *
         * @param debounceSamples non-negative number of samples
         * @return this builder
         */
        public Builder withDebounceSamples(int debounceSamples) {
            if (debounceSamples < 0) {
                throw new IllegalArgumentException("debounceSamples must be non-negative: " + debounceSamples);
            }
            this.debounceSamples = debounceSamples;
            return this;
        }

        /**
         * Sets the maximum backoff interval in milliseconds for availability polling.
         *
         * @param backoffMaxMs non-negative maximum backoff in milliseconds
         * @return this builder
         */
        public Builder withBackoffMaxMs(int backoffMaxMs) {
            if (backoffMaxMs < 0) {
                throw new IllegalArgumentException("backoffMaxMs must be non-negative: " + backoffMaxMs);
            }
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

        /**
         * Builds and initializes the configured {@link Context}.
         *
         * @return newly initialized PRISM context
         */
        public Context build() {
            return new Context(this);
        }
    }
}
