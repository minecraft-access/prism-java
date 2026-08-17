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
}
