package org.mcaccess.prism;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrismTest {

    @Test
    void testIsAvailable() {
        assertThat(Prism.isAvailable()).isTrue();
    }

    @Test
    void testGetErrorString() {
        String okMsg = Prism.getErrorString(0);
        assertThat(okMsg).isNotNull();

        String notInitMsg = Prism.getErrorString(1);
        assertThat(notInitMsg).isNotBlank();
    }

    @Test
    void testContextLifecycle() {
        try (Context ctx = Prism.createContext()) {
            assertThat(ctx.isClosed()).isFalse();
            assertThat(ctx.handle()).isNotNull();

            int backendsCount = ctx.getBackendsCount();
            assertThat(backendsCount).isGreaterThanOrEqualTo(0);

            for (int i = 0; i < backendsCount; i++) {
                BackendId id = ctx.getIdOf(i);
                assertThat(id).isNotNull();
                String name = ctx.getNameOf(id);
                assertThat(name).isNotBlank();
                int priority = ctx.getPriorityOf(id);
                assertThat(ctx.exists(id)).isTrue();
            }
        }
    }

    @Test
    void testBackendAcquireAndCreate() {
        try (Context ctx = Prism.createContext()) {
            boolean testedAtLeastOne = false;
            for (int i = 0; i < ctx.getBackendsCount(); i++) {
                BackendId id = ctx.getIdOf(i);
                try (Backend backend = ctx.create(id)) {
                    assertThat(backend.getName()).isNotBlank();
                    BackendFeatures features = backend.getFeatures();
                    assertThat(features).isNotNull();

                    if (features.supportsGetVolume()) {
                        float volume = backend.getVolume();
                        assertThat(volume).isBetween(0.0f, 1.0f);
                    }

                    if (features.supportsCountVoices()) {
                        int voicesCount = backend.getVoicesCount();
                        assertThat(voicesCount).isGreaterThanOrEqualTo(0);
                        if (voicesCount > 0 && features.supportsGetVoiceName()) {
                            String voiceName = backend.getVoiceName(0);
                            assertThat(voiceName).isNotNull();
                        }
                        if (voicesCount > 0 && features.supportsGetVoiceLanguage()) {
                            String voiceLang = backend.getVoiceLanguage(0);
                            assertThat(voiceLang).isNotNull();
                        }
                    }
                    testedAtLeastOne = true;
                    break;
                } catch (PrismException.BackendNotAvailable ignored) {
                    // Backend is registered but not active/installed on this system
                }
            }

            // Also test createBest / acquireBest if an available backend exists
            if (testedAtLeastOne) {
                try (Backend best = ctx.createBest()) {
                    assertThat(best.getName()).isNotBlank();
                }
            }
        }
    }

    @Test
    void testSpeakToMemoryIfSupported() {
        try (Context ctx = Prism.createContext()) {
            for (int i = 0; i < ctx.getBackendsCount(); i++) {
                BackendId id = ctx.getIdOf(i);
                try (Backend backend = ctx.create(id)) {
                    BackendFeatures features = backend.getFeatures();
                    if (features.supportsSpeakToMemory()) {
                        AtomicBoolean received = new AtomicBoolean(false);
                        AtomicInteger totalSamples = new AtomicInteger(0);

                        backend.speakToMemory("Hello from Java PRISM bindings", (samples, channels, sampleRate) -> {
                            received.set(true);
                            totalSamples.addAndGet(samples.length);
                            assertThat(channels).isGreaterThan(0);
                            assertThat(sampleRate).isGreaterThan(0);
                        });

                        assertThat(received.get()).isTrue();
                        assertThat(totalSamples.get()).isGreaterThan(0);
                        break;
                    }
                } catch (PrismException.BackendNotAvailable ignored) {
                    // Backend not installed
                }
            }
        }
    }

    @Test
    void testInvalidTextInput() {
        try (Context ctx = Prism.createContext()) {
            for (int i = 0; i < ctx.getBackendsCount(); i++) {
                BackendId id = ctx.getIdOf(i);
                try (Backend backend = ctx.create(id)) {
                    assertThatThrownBy(() -> backend.speak(""))
                            .isInstanceOf(PrismException.InvalidParam.class);
                    assertThatThrownBy(() -> backend.speak("Hello\0World"))
                            .isInstanceOf(PrismException.InvalidParam.class);
                    break;
                } catch (PrismException.BackendNotAvailable ignored) {
                    // Continue searching for an active backend
                }
            }
        }
    }

    @Test
    void testVersion() {
        assertThat(Prism.getVersion()).isGreaterThan(0);
        assertThat(Prism.getVersionString()).isEqualTo("0.18.3");

        assertThat(Prism.getCompileVersionMajor()).isEqualTo(0);
        assertThat(Prism.getCompileVersionMinor()).isEqualTo(18);
        assertThat(Prism.getCompileVersionPatch()).isEqualTo(3);
        assertThat(Prism.getCompileVersionString()).isEqualTo("0.18.3");
    }

    @Test
    void testAvailabilityAutoPower() {
        // Just verify it can be queried without throwing
        boolean supported = Prism.isAvailabilityAutoPowerSupported();
        assertThat(supported).isIn(true, false);
    }

    @Test
    void testContextBuilderAndAvailability() {
        AtomicBoolean baselineCalled = new AtomicBoolean(false);
        AtomicInteger availabilityEvents = new AtomicInteger(0);

        try (Context ctx = Prism.createContext(builder -> builder
                .withPollIntervalMs(250)
                .withDebounceSamples(2)
                .withBackoffMaxMs(1000)
                .withAutoPowerManage(false)
                .withAvailabilityListener((backend, name, available) -> availabilityEvents.incrementAndGet())
                .withBaselineCallback(() -> baselineCalled.set(true)))) {

            assertThat(ctx.isClosed()).isFalse();

            // Verify polling pause and resume can be called
            ctx.pauseAvailabilityPolling();
            ctx.resumeAvailabilityPolling();
        }
    }

    @Test
    void testAcquireGetAndReferenceCountRelease() {
        try (Context ctx = Prism.createContext()) {
            assertThatThrownBy(() -> ctx.getIdOf(-1))
                    .isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> ctx.getRawIdOf(-1))
                    .isInstanceOf(IndexOutOfBoundsException.class);
            assertThat(ctx.findRawIdOf("nonexistent\0backend")).isEmpty();

            for (int i = 0; i < ctx.getBackendsCount(); i++) {
                BackendId id = ctx.getIdOf(i);
                long rawId = ctx.getRawIdOf(i);
                assertThat(rawId).isEqualTo(id.getId());
                String name = ctx.getNameOf(id);
                assertThat(ctx.findRawIdOf(name)).hasValue(rawId);

                try {
                    assertThat(ctx.get(id)).isEmpty();
                    try (Backend acquired = ctx.acquire(id)) {
                        assertThat(acquired.getName()).isEqualTo(name);
                        try (Backend cached = ctx.get(id).orElseThrow()) {
                            assertThat(cached.getName()).isEqualTo(name);
                        }
                        // Shared instance should still be alive until 'acquired' is closed
                        assertThat(ctx.get(id)).isPresent().get().satisfies(Backend::close);
                    }
                    // Once all acquired/get handles are closed, cached shared instance must be released
                    assertThat(ctx.get(id)).isEmpty();
                    break;
                } catch (PrismException.BackendNotAvailable ignored) {
                    // Backend not active on this system
                }
            }
        }
    }

    @Test
    void testParameterValidationAndCallbackExceptionSafety() {
        try (Context ctx = Prism.createContext()) {
            for (int i = 0; i < ctx.getBackendsCount(); i++) {
                BackendId id = ctx.getIdOf(i);
                try (Backend backend = ctx.create(id)) {
                    assertThatThrownBy(() -> backend.setVolume(-0.1f))
                            .isInstanceOf(PrismException.RangeOutOfBounds.class);
                    assertThatThrownBy(() -> backend.setVolume(1.1f))
                            .isInstanceOf(PrismException.RangeOutOfBounds.class);
                    assertThatThrownBy(() -> backend.setVolume(Float.NaN))
                            .isInstanceOf(PrismException.RangeOutOfBounds.class);

                    assertThatThrownBy(() -> backend.setRate(-0.1f))
                            .isInstanceOf(PrismException.RangeOutOfBounds.class);
                    assertThatThrownBy(() -> backend.setRate(1.1f))
                            .isInstanceOf(PrismException.RangeOutOfBounds.class);
                    assertThatThrownBy(() -> backend.setRate(Float.NaN))
                            .isInstanceOf(PrismException.RangeOutOfBounds.class);

                    assertThatThrownBy(() -> backend.setPitch(-0.1f))
                            .isInstanceOf(PrismException.RangeOutOfBounds.class);
                    assertThatThrownBy(() -> backend.setPitch(1.1f))
                            .isInstanceOf(PrismException.RangeOutOfBounds.class);
                    assertThatThrownBy(() -> backend.setPitch(Float.NaN))
                            .isInstanceOf(PrismException.RangeOutOfBounds.class);

                    assertThatThrownBy(() -> backend.getVoiceName(-1))
                            .isInstanceOf(PrismException.RangeOutOfBounds.class);
                    assertThatThrownBy(() -> backend.getVoiceLanguage(-1))
                            .isInstanceOf(PrismException.RangeOutOfBounds.class);
                    assertThatThrownBy(() -> backend.setVoice(-1))
                            .isInstanceOf(PrismException.RangeOutOfBounds.class);

                    if (backend.getFeatures().supportsSpeakToMemory()) {
                        assertThatThrownBy(() -> backend.speakToMemory("Test exception", (samples, ch, sr) -> {
                            throw new IllegalStateException("Expected callback failure");
                        }))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessage("Expected callback failure");
                    }
                    break;
                } catch (PrismException.BackendNotAvailable ignored) {
                    // Continue searching for an active backend
                }
            }
        }
    }

    @Test
    void testLogging() {
        AtomicBoolean logReceived = new AtomicBoolean(false);
        LogLevel previousLevel = Prism.setLogLevel(LogLevel.DEBUG);
        try {
            Prism.setLogCallback((level, source, message) -> {
                if ("java-test".equals(source) && "hello prism log".equals(message) && level == LogLevel.INFO) {
                    logReceived.set(true);
                }
            });
            Prism.log(LogLevel.INFO, "java-test", "hello prism log");
            Prism.flushLog();
            assertThat(logReceived.get()).isTrue();
        } finally {
            Prism.setLogCallback(null);
            Prism.setLogLevel(previousLevel);
        }
    }
}
