package org.mcaccess.prism;

import org.mcaccess.prism.natives.NativeLoader;
import org.mcaccess.prism.natives.PrismAvailabilityCallback;
import org.mcaccess.prism.natives.PrismConfig;
import org.mcaccess.prism.natives.prism_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * The PRISM context manages backend registration and lifecycle.
 */
public final class Context implements AutoCloseable {
    private final MemorySegment handle;
    private final Arena arena;
    private final boolean polling;
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
        this(new Builder().apply(configurer));
    }

    private Context(Builder builder) {
        NativeLoader.load();
        this.arena = Arena.ofShared();
        this.polling = builder.availabilityListener != null;
        MemorySegment configSeg = prism_h.prism_config_init(arena);

        if (this.polling) {
            AvailabilityListener listener = builder.availabilityListener;
            Executor executor = builder.availabilityExecutor;
            MemorySegment stub = PrismAvailabilityCallback.allocate(
                    (userdata, backend, name, available) -> dispatch(listener, executor, backend, name, available),
                    arena);
            PrismConfig.availability_callback(configSeg, stub);
            PrismConfig.availability_userdata(configSeg, MemorySegment.NULL);
            PrismConfig.availability_poll_interval_ms(configSeg, builder.pollIntervalMs);
            PrismConfig.availability_debounce_samples(configSeg, builder.debounceSamples);
            PrismConfig.availability_backoff_max_ms(configSeg, builder.backoffMaxMs);
            PrismConfig.availability_auto_power_manage(configSeg, builder.autoPowerManage);
        }

        this.handle = prism_h.prism_init(configSeg);
        if (this.handle.address() == 0) {
            arena.close();
            throw new PrismException.NotInitialized("PRISM could not be initialized");
        }
    }

    /**
     * Hands one availability transition to the application.
     * <p>
     * Invoked on PRISM's poll thread, which performs no further scans until this returns and which must never see a
     * Java exception. Both concerns are why the listener is optionally routed through an executor and why every
     * failure is swallowed here.
     */
    private static void dispatch(AvailabilityListener listener, Executor executor, long backend, MemorySegment name,
            boolean available) {
        BackendId id = new BackendId(backend);
        String backendName = Backend.readCString(name);
        Runnable task = () -> listener.onAvailabilityChanged(id, backendName, available);
        try {
            if (executor != null) {
                executor.execute(task);
            } else {
                task.run();
            }
        } catch (Throwable ignored) {
            // A throw here would unwind into native code.
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
     * Lists every backend registered with PRISM, in registry index order.
     * <p>
     * Runtime availability is reported through {@link AvailabilityListener}, and can be read from a live backend through {@link Backend#getFeatures()} and {@link BackendFeature#IS_SUPPORTED_AT_RUNTIME}.
     * Sort by {@link BackendInfo#priority()} for preference order.
     *
     * @return an immutable list of the registered backends
     */
    public List<BackendInfo> getRegisteredBackends() {
        checkClosed();
        return IntStream.range(0, getBackendsCount())
                .mapToObj(i -> {
                    BackendId id = getIdOf(i);
                    return new BackendInfo(id, getNameOf(id), getPriorityOf(id));
                })
                .toList();
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
        return new BackendId(id);
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
            return Optional.of(new BackendId(id));
        }
    }

    /**
     * Gets the name of the backend identified by the given backend ID.
     *
     * @param id backend ID
     * @return backend name
     */
    public String getNameOf(BackendId id) {
        return getNameOf(id.id());
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
        if (ptr.address() == 0) {
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
        return getPriorityOf(id.id());
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
        return exists(id.id());
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
        return create(id.id());
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
        if (ptr.address() == 0) {
            throw new PrismException.InvalidParam("Invalid or unsupported backend: 0x" + Long.toHexString(id));
        }
        return new Backend(ptr, ptr.address());
    }

    /**
     * Creates a new owned instance of the highest priority available backend.
     *
     * @return newly created Backend instance
     */
    public Backend createBest() {
        checkClosed();
        MemorySegment ptr = prism_h.prism_registry_create_best(handle);
        if (ptr.address() == 0) {
            throw new PrismException.BackendNotAvailable("No suitable PRISM backend available on this system");
        }
        return new Backend(ptr, ptr.address());
    }

    /**
     * Acquires a shared instance of the backend with the specified ID.
     *
     * @param id backend ID
     * @return acquired Backend instance
     */
    public Backend acquire(BackendId id) {
        return acquire(id.id());
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
        if (ptr.address() == 0) {
            throw new PrismException.InvalidParam("Invalid or unsupported backend: 0x" + Long.toHexString(id));
        }
        return new Backend(ptr, new BackendId(id));
    }

    /**
     * Acquires a shared instance of the highest priority available backend.
     *
     * @return acquired Backend instance
     */
    public Backend acquireBest() {
        checkClosed();
        MemorySegment ptr = prism_h.prism_registry_acquire_best(handle);
        if (ptr.address() == 0) {
            throw new PrismException.BackendNotAvailable("No suitable PRISM backend available on this system");
        }
        return new Backend(ptr, getIdOf(Backend.readCString(prism_h.prism_backend_name(ptr))));
    }

    /**
     * Pauses the availability poll thread. While paused it performs no scans.
     * <p>
     * A no-op if this context was not configured with an availability listener, or if polling is already paused.
     */
    public void pauseAvailabilityPolling() {
        checkClosed();
        if (polling) {
            prism_h.prism_availability_poll_pause(handle);
        }
    }

    /**
     * Resumes the availability poll thread.
     * <p>
     * On resume PRISM performs an immediate re-synchronising scan rather than waiting for the next interval, and that scan is not debounced: any backend whose availability differs from the state last reported produces a callback at once. A change that occurred and reversed entirely while paused is therefore not reported.
     */
    public void resumeAvailabilityPolling() {
        checkClosed();
        if (polling) {
            prism_h.prism_availability_poll_resume(handle);
        }
    }

    /**
     * Reports whether this build of PRISM honours automatic power management of the poll thread.
     */
    public static boolean isAutoPowerManagementSupported() {
        NativeLoader.load();
        return prism_h.prism_availability_auto_power_supported();
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            prism_h.prism_shutdown(handle);
            arena.close();
        }
    }

    public boolean isClosed() {
        return closed;
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
        AvailabilityListener availabilityListener;
        Executor availabilityExecutor;
        int pollIntervalMs;
        int debounceSamples;
        int backoffMaxMs;
        boolean autoPowerManage = true;

        /**
         * Polls for availability changes and reports each confirmed transition to {@code listener}.
         * <p>
         * Without a listener the context runs no poll thread and incurs no cost. The listener is invoked on PRISM's poll thread, which performs no further scans until it returns, so supply an executor to move any non-trivial work elsewhere.
         *
         * @param listener Invoked on each confirmed availability transition, or null for no polling.
         * @param executor Runs the listener. Pass null to run it directly on PRISM's poll thread.
         */
        public Builder availabilityListener(AvailabilityListener listener, Executor executor) {
            this.availabilityListener = listener;
            this.availabilityExecutor = executor;
            return this;
        }

        /**
         * @param listener Invoked on each confirmed availability transition, or null for no polling.
         */
        public Builder availabilityListener(AvailabilityListener listener) {
            return availabilityListener(listener, null);
        }

        /**
         * @param pollIntervalMs Base interval between scans. 0 selects PRISM's default.
         */
        public Builder pollIntervalMs(int pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
            return this;
        }

        /**
         * @param debounceSamples Consecutive agreeing samples needed before a change is confirmed. 0 selects PRISM's default.
         */
        public Builder debounceSamples(int debounceSamples) {
            this.debounceSamples = debounceSamples;
            return this;
        }

        /**
         * @param backoffMaxMs Upper bound for adaptive backoff of the interval while availability is unchanging. The interval is exponential and returns to the base interval as soon as any change is observed.
         */
        public Builder backoffMaxMs(int backoffMaxMs) {
            this.backoffMaxMs = backoffMaxMs;
            return this;
        }

        /**
         * @param autoPowerManage Pause the poll thread automatically across OS suspend and resume. Ignored on builds and platforms without power-management support; see
         *                        {@link Context#isAutoPowerManagementSupported()}.
         */
        public Builder autoPowerManage(boolean autoPowerManage) {
            this.autoPowerManage = autoPowerManage;
            return this;
        }

        public Builder apply(Consumer<Builder> configurer) {
            configurer.accept(this);
            return this;
        }

        public Context build() {
            return new Context(this);
        }
    }
}
