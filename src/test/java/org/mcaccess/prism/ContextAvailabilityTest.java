package org.mcaccess.prism;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises availability polling against a single shared context.
 * <p>
 * Deliberately not one context per test: PRISM crashes when a context configured with an availability callback is
 * shut down and another is then created and shut down in the same process, so every polling test here shares one
 * context that is closed once at the end. Contexts without a callback are unaffected and are created freely.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContextAvailabilityTest {

    private static final List<String> EVENTS = new CopyOnWriteArrayList<>();
    private static final CountDownLatch SCANNED = new CountDownLatch(1);

    private ExecutorService executor;
    private Context polling;

    @BeforeAll
    void startPolling() {
        PrismLog.setListener((level, source, message) -> {
            if (message.contains("Scanning instance slot")) {
                SCANNED.countDown();
            }
        });
        PrismLog.setLevel(PrismLog.Level.TRACE);

        executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "availability-dispatch"));
        polling = Context.builder()
                .availabilityListener((id, name, available) -> EVENTS.add(name + "=" + available), executor)
                .pollIntervalMs(0)
                .debounceSamples(1)
                .backoffMaxMs(0)
                .autoPowerManage(true)
                .build();
    }

    @AfterAll
    void stopPolling() throws Exception {
        polling.close();
        assertThat(polling.isClosed()).isTrue();
        executor.shutdownNow();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        PrismLog.clearListener();
        PrismLog.setLevel(PrismLog.Level.NONE);
    }

    @AfterEach
    void resumeIfPaused() {
        polling.resumeAvailabilityPolling();
    }

    @Test
    void configuringAListenerStartsThePollThread() throws Exception {
        assertThat(SCANNED.await(6, TimeUnit.SECONDS))
                .as("PRISM must run its poll thread once an availability callback is configured")
                .isTrue();
    }

    @Test
    void pausingAndResumingIsIdempotent() {
        polling.pauseAvailabilityPolling();
        polling.pauseAvailabilityPolling();
        polling.resumeAvailabilityPolling();
        polling.resumeAvailabilityPolling();
        assertThat(polling.isClosed()).isFalse();
    }

    @Test
    void backendsRemainUsableWhilePolling() {
        try (Backend backend = polling.acquireBest()) {
            assertThat(backend.getName()).isNotBlank();
            assertThat(BackendFeature.IS_SUPPORTED_AT_RUNTIME.isSupportedBy(backend.getFeatures())).isTrue();
        }
    }

    @Test
    void registryQueriesWorkWhilePolling() {
        assertThat(polling.getBackendsCount()).isPositive();
        BackendId first = polling.getIdOf(0);
        assertThat(first.isInvalid()).isFalse();
        assertThat(polling.getNameOf(first)).isNotBlank();
    }

    @Test
    void aContextWithoutAListenerRunsNoPollThreadAndTakesPollingCallsSafely() {
        try (Context plain = new Context()) {
            plain.pauseAvailabilityPolling();
            plain.resumeAvailabilityPolling();
            assertThat(plain.getBackendsCount()).isPositive();
        }
    }

    @Test
    void pollingControlsRejectAClosedContext() {
        Context closed = new Context();
        closed.close();
        assertThatThrownBy(closed::pauseAvailabilityPolling).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(closed::resumeAvailabilityPolling).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void autoPowerManagementSupportIsQueryable() {
        assertThat(Context.isAutoPowerManagementSupported()).isIn(true, false);
    }

    @Test
    void noSpuriousEventsWithoutAnActualTransition() throws Exception {
        int before = EVENTS.size();
        Thread.sleep(2500);
        assertThat(EVENTS.subList(Math.min(before, EVENTS.size()), EVENTS.size()))
                .as("a baseline scan must not fabricate transitions")
                .isEmpty();
    }
}
