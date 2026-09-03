package org.mcaccess.prism;

import org.mcaccess.prism.natives.NativeLoader;
import org.mcaccess.prism.natives.PrismAudioCallback;
import org.mcaccess.prism.natives.prism_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * Represents an active PRISM speech or screen reader backend instance.
 */
public final class Backend implements AutoCloseable {
    /**
     * the locks use the native instance identity. Keying by address can result in returning new handles to the same cached instance and as the docs state, a backend instance is not thread-safe even for logically independent calls.
     * Only created instance key by address.
     */
    private static final ConcurrentMap<Object, Object> LOCKS = new ConcurrentHashMap<>();

    @FunctionalInterface
    private interface OutCall {
        int invoke(MemorySegment out);
    }

    @FunctionalInterface
    private interface TextCall {
        int invoke(Arena arena, MemorySegment text);
    }

    private final MemorySegment handle;
    private final Object lock;
    private volatile boolean closed = false;

    Backend(MemorySegment handle, Object lockKey) {
        if (handle.address() == 0) {
            throw new IllegalArgumentException("Backend handle must not be NULL");
        }
        NativeLoader.load();
        this.handle = handle;
        this.lock = LOCKS.computeIfAbsent(lockKey, k -> new Object());

        synchronized (lock) {
            int res = prism_h.prism_backend_initialize(handle);
            if (res != prism_h.PRISM_OK() && res != prism_h.PRISM_ERROR_ALREADY_INITIALIZED()) {
                PrismException.throwIfError(res);
            }
        }
    }

    /**
     * Gets the raw native memory segment handle for this backend.
     *
     * @return the backend handle
     */
    public MemorySegment handle() {
        checkClosed();
        return handle;
    }

    /**
     * Gets the name of the backend.
     *
     * @return backend name
     */
    public String getName() {
        return locked(() -> readCString(prism_h.prism_backend_name(handle)));
    }

    /**
     * Gets the feature flags supported by this backend.
     *
     * @return the raw feature bitmask; decode it with {@link BackendFeature}
     */
    public long getFeatures() {
        return locked(() -> prism_h.prism_backend_get_features(handle));
    }

    /**
     * Synthesizes and speaks text out loud through the backend.
     *
     * @param text      the text to speak
     * @param interrupt whether to interrupt ongoing speech
     */
    public void speak(String text, boolean interrupt) {
        callWithText(text, (arena, seg) -> prism_h.prism_backend_speak(handle, seg, interrupt));
    }

    /**
     * Synthesizes and speaks text out loud without interrupting ongoing speech.
     *
     * @param text the text to speak
     */
    public void speak(String text) {
        speak(text, false);
    }

    /**
     * Synthesizes text directly to memory PCM audio samples.
     *
     * @param text     the text to synthesize
     * @param callback consumer called with audio sample data
     */
    public void speakToMemory(String text, AudioCallback callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        PrismAudioCallback.Function onAudio = (userdata, samplesPtr, sampleCount, channels, sampleRate) -> {
            int count = (int) sampleCount;
            float[] samples = new float[count];
            if (count > 0 && samplesPtr.address() != 0) {
                MemorySegment sized = samplesPtr.reinterpret((long) count * Float.BYTES);
                MemorySegment.copy(sized, JAVA_FLOAT, 0, samples, 0, count);
            }
            callback.onAudioData(samples, (int) channels, (int) sampleRate);
        };
        callWithText(text, (arena, seg) -> prism_h.prism_backend_speak_to_memory(
                handle, seg, PrismAudioCallback.allocate(onAudio, arena), MemorySegment.NULL));
    }

    /**
     * Sends text to a connected refreshable Braille display.
     *
     * @param text the text to display in Braille
     */
    public void braille(String text) {
        callWithText(text, (arena, seg) -> prism_h.prism_backend_braille(handle, seg));
    }

    /**
     * Outputs text simultaneously to speech and braille if supported.
     *
     * @param text      the text to output
     * @param interrupt whether to interrupt ongoing speech
     */
    public void output(String text, boolean interrupt) {
        callWithText(text, (arena, seg) -> prism_h.prism_backend_output(handle, seg, interrupt));
    }

    /**
     * Outputs text simultaneously to speech and braille without interrupting.
     *
     * @param text the text to output
     */
    public void output(String text) {
        output(text, false);
    }

    /**
     * Stops current speech output immediately.
     */
    public void stop() {
        call(() -> prism_h.prism_backend_stop(handle));
    }

    /**
     * Pauses ongoing speech synthesis and playback.
     */
    public void pause() {
        call(() -> prism_h.prism_backend_pause(handle));
    }

    /**
     * Resumes paused speech playback.
     */
    public void resume() {
        call(() -> prism_h.prism_backend_resume(handle));
    }

    /**
     * Checks if the backend is currently speaking.
     *
     * @return {@code true} if speaking, {@code false} otherwise
     */
    public boolean isSpeaking() {
        return queryBoolean(out -> prism_h.prism_backend_is_speaking(handle, out));
    }

    /**
     * Sets speech volume level.
     *
     * @param volume volume level between 0.0 and 1.0
     */
    public void setVolume(float volume) {
        if (volume < 0.0f || volume > 1.0f) {
            throw new PrismException.RangeOutOfBounds("Volume must be between 0.0 and 1.0");
        }
        call(() -> prism_h.prism_backend_set_volume(handle, volume));
    }

    /**
     * Gets current speech volume level.
     *
     * @return current volume between 0.0 and 1.0
     */
    public float getVolume() {
        return queryFloat(out -> prism_h.prism_backend_get_volume(handle, out));
    }

    /**
     * Sets speech rate / speed.
     *
     * @param rate speech rate multiplier (e.g. 1.0 is normal rate)
     */
    public void setRate(float rate) {
        if (rate < 0.0f) {
            throw new PrismException.RangeOutOfBounds("Rate must be non-negative");
        }
        call(() -> prism_h.prism_backend_set_rate(handle, rate));
    }

    /**
     * Gets current speech rate.
     *
     * @return current speech rate
     */
    public float getRate() {
        return queryFloat(out -> prism_h.prism_backend_get_rate(handle, out));
    }

    /**
     * Sets speech pitch.
     *
     * @param pitch speech pitch multiplier (e.g. 1.0 is normal pitch)
     */
    public void setPitch(float pitch) {
        if (pitch < 0.0f) {
            throw new PrismException.RangeOutOfBounds("Pitch must be non-negative");
        }
        call(() -> prism_h.prism_backend_set_pitch(handle, pitch));
    }

    /**
     * Gets current speech pitch.
     *
     * @return current speech pitch
     */
    public float getPitch() {
        return queryFloat(out -> prism_h.prism_backend_get_pitch(handle, out));
    }

    /**
     * Refreshes the list of available voices.
     */
    public void refreshVoices() {
        call(() -> prism_h.prism_backend_refresh_voices(handle));
    }

    /**
     * Gets the number of available voices for this backend.
     *
     * @return number of voices
     */
    public int getVoicesCount() {
        return (int) queryLong(out -> prism_h.prism_backend_count_voices(handle, out));
    }

    /**
     * Gets the human-readable name of a voice by its index.
     *
     * @param voiceIndex 0-based voice index
     * @return voice name
     */
    public String getVoiceName(int voiceIndex) {
        return queryString(out -> prism_h.prism_backend_get_voice_name(handle, voiceIndex, out));
    }

    /**
     * Gets the BCP-47 language tag of a voice by its index.
     *
     * @param voiceIndex 0-based voice index
     * @return voice language code (e.g. "en-US")
     */
    public String getVoiceLanguage(int voiceIndex) {
        return queryString(out -> prism_h.prism_backend_get_voice_language(handle, voiceIndex, out));
    }

    /**
     * Sets the active voice by index.
     *
     * @param voiceIndex 0-based voice index
     */
    public void setVoice(int voiceIndex) {
        call(() -> prism_h.prism_backend_set_voice(handle, voiceIndex));
    }

    /**
     * Gets the index of the currently active voice.
     *
     * @return active voice index
     */
    public int getVoice() {
        return (int) queryLong(out -> prism_h.prism_backend_get_voice(handle, out));
    }

    /**
     * Gets the number of audio channels produced by this backend.
     *
     * @return channel count (1 for mono, 2 for stereo)
     */
    public int getChannels() {
        return (int) queryLong(out -> prism_h.prism_backend_get_channels(handle, out));
    }

    /**
     * Gets the audio sample rate produced by this backend.
     *
     * @return sample rate in Hz (e.g. 44100)
     */
    public int getSampleRate() {
        return (int) queryLong(out -> prism_h.prism_backend_get_sample_rate(handle, out));
    }

    /**
     * Gets the bit depth of the audio produced by this backend.
     *
     * @return bit depth (e.g. 16, 32)
     */
    public int getBitDepth() {
        return (int) queryLong(out -> prism_h.prism_backend_get_bit_depth(handle, out));
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (!closed) {
                closed = true;
                prism_h.prism_backend_free(handle);
            }
        }
    }

    public boolean isClosed() {
        return closed;
    }

    private <T> T locked(Supplier<T> action) {
        synchronized (lock) {
            checkClosed();
            return action.get();
        }
    }

    private void call(IntSupplier nativeCall) {
        synchronized (lock) {
            checkClosed();
            PrismException.throwIfError(nativeCall.getAsInt());
        }
    }

    private void callWithText(String text, TextCall nativeCall) {
        validateText(text);
        call(() -> {
            try (Arena arena = Arena.ofConfined()) {
                return nativeCall.invoke(arena, arena.allocateFrom(text));
            }
        });
    }

    private <T> T query(ValueLayout layout, OutCall nativeCall, Function<MemorySegment, T> read) {
        return locked(() -> {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment out = arena.allocate(layout);
                PrismException.throwIfError(nativeCall.invoke(out));
                return read.apply(out);
            }
        });
    }

    private float queryFloat(OutCall nativeCall) {
        return query(JAVA_FLOAT, nativeCall, out -> out.get(JAVA_FLOAT, 0));
    }

    private long queryLong(OutCall nativeCall) {
        return query(JAVA_LONG, nativeCall, out -> out.get(JAVA_LONG, 0));
    }

    private boolean queryBoolean(OutCall nativeCall) {
        return query(JAVA_BOOLEAN, nativeCall, out -> out.get(JAVA_BOOLEAN, 0));
    }

    private String queryString(OutCall nativeCall) {
        return query(ADDRESS, nativeCall, out -> readCString(out.get(ADDRESS, 0)));
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Backend is already closed");
        }
    }

    private static void validateText(String text) {
        if (text == null || text.isEmpty()) {
            throw new PrismException.InvalidParam("Text must not be null or empty");
        }
        if (text.indexOf('\0') >= 0) {
            throw new PrismException.InvalidParam("Text must not contain embedded null characters");
        }
    }

    static String readCString(MemorySegment ptr) {
        if (ptr.address() == 0) {
            return "";
        }
        return ptr.reinterpret(Long.MAX_VALUE).getString(0);
    }
}
