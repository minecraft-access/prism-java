package org.mcaccess.prism;

import org.mcaccess.prism.natives.NativeLoader;
import org.mcaccess.prism.natives.PrismAudioCallback;
import org.mcaccess.prism.natives.prism_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/**
 * Represents an active PRISM speech or screen reader backend instance.
 */
public final class Backend implements AutoCloseable {
    private final MemorySegment handle;
    private volatile boolean closed = false;

    Backend(MemorySegment handle, boolean alreadyInitialized) {
        Objects.requireNonNull(handle, "Backend handle must not be null");
        if (isNull(handle)) {
            throw new IllegalArgumentException("Backend handle must not be NULL");
        }
        NativeLoader.load();
        this.handle = handle;

        if (!alreadyInitialized) {
            int res = prism_h.prism_backend_initialize(handle);
            if (res != prism_h.PRISM_OK() && res != prism_h.PRISM_ERROR_ALREADY_INITIALIZED()) {
                prism_h.prism_backend_free(handle);
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
        checkClosed();
        MemorySegment namePtr = prism_h.prism_backend_name(handle);
        return readCString(namePtr);
    }

    /**
     * Gets the feature flags supported by this backend.
     *
     * @return backend features
     */
    public BackendFeatures getFeatures() {
        checkClosed();
        long features = prism_h.prism_backend_get_features(handle);
        return BackendFeatures.fromBits(features);
    }

    /**
     * Synthesizes and speaks text out loud through the backend.
     *
     * @param text      the text to speak
     * @param interrupt whether to interrupt ongoing speech
     */
    public void speak(String text, boolean interrupt) {
        checkClosed();
        validateText(text);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment textSeg = arena.allocateFrom(text);
            int res = prism_h.prism_backend_speak(handle, textSeg, interrupt);
            PrismException.throwIfError(res);
        }
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
        checkClosed();
        validateText(text);
        Objects.requireNonNull(callback, "callback must not be null");

        Throwable[] callbackError = new Throwable[1];
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment textSeg = arena.allocateFrom(text);

            PrismAudioCallback.Function callbackFunc = (userdata, samplesPtr, sampleCount, channels, sampleRate) -> {
                if (callbackError[0] != null) {
                    return;
                }
                try {
                    int count = Math.toIntExact(sampleCount);
                    float[] samples = new float[count];
                    if (count > 0 && !isNull(samplesPtr)) {
                        long byteCount = (long) count * Float.BYTES;
                        MemorySegment src = samplesPtr.byteSize() >= byteCount
                                ? samplesPtr
                                : samplesPtr.reinterpret(byteCount);
                        MemorySegment.copy(src, ValueLayout.JAVA_FLOAT, 0, samples, 0, count);
                    }
                    callback.onAudioData(samples, (int) channels, (int) sampleRate);
                } catch (Throwable t) {
                    callbackError[0] = t;
                }
            };

            MemorySegment callbackStub = PrismAudioCallback.allocate(callbackFunc, arena);
            int res = prism_h.prism_backend_speak_to_memory(handle, textSeg, callbackStub, MemorySegment.NULL);
            if (callbackError[0] != null) {
                switch (callbackError[0]) {
                    case RuntimeException re -> throw re;
                    case Error err -> throw err;
                    default -> throw new RuntimeException(callbackError[0]);
                }
            }
            PrismException.throwIfError(res);
        }
    }

    /**
     * Sends text to a connected refreshable Braille display.
     *
     * @param text the text to display in Braille
     */
    public void braille(String text) {
        checkClosed();
        validateText(text);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment textSeg = arena.allocateFrom(text);
            int res = prism_h.prism_backend_braille(handle, textSeg);
            PrismException.throwIfError(res);
        }
    }

    /**
     * Outputs text simultaneously to speech and braille if supported.
     *
     * @param text      the text to output
     * @param interrupt whether to interrupt ongoing speech
     */
    public void output(String text, boolean interrupt) {
        checkClosed();
        validateText(text);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment textSeg = arena.allocateFrom(text);
            int res = prism_h.prism_backend_output(handle, textSeg, interrupt);
            PrismException.throwIfError(res);
        }
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
        checkClosed();
        int res = prism_h.prism_backend_stop(handle);
        PrismException.throwIfError(res);
    }

    /**
     * Pauses ongoing speech synthesis and playback.
     */
    public void pause() {
        checkClosed();
        int res = prism_h.prism_backend_pause(handle);
        PrismException.throwIfError(res);
    }

    /**
     * Resumes paused speech playback.
     */
    public void resume() {
        checkClosed();
        int res = prism_h.prism_backend_resume(handle);
        PrismException.throwIfError(res);
    }

    /**
     * Checks if the backend is currently speaking.
     *
     * @return {@code true} if speaking, {@code false} otherwise
     */
    public boolean isSpeaking() {
        checkClosed();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outSpeaking = arena.allocate(ValueLayout.JAVA_BOOLEAN);
            int res = prism_h.prism_backend_is_speaking(handle, outSpeaking);
            PrismException.throwIfError(res);
            return outSpeaking.get(ValueLayout.JAVA_BOOLEAN, 0);
        }
    }

    /**
     * Sets speech volume level.
     *
     * @param volume normalized volume level between 0.0 and 1.0
     */
    public void setVolume(float volume) {
        checkClosed();
        if (!Float.isFinite(volume) || volume < 0.0f || volume > 1.0f) {
            throw new PrismException.RangeOutOfBounds("Volume must be between 0.0 and 1.0");
        }
        int res = prism_h.prism_backend_set_volume(handle, volume);
        PrismException.throwIfError(res);
    }

    /**
     * Gets current speech volume level.
     *
     * @return current normalized volume between 0.0 and 1.0
     */
    public float getVolume() {
        checkClosed();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outVolume = arena.allocate(ValueLayout.JAVA_FLOAT);
            int res = prism_h.prism_backend_get_volume(handle, outVolume);
            PrismException.throwIfError(res);
            return outVolume.get(ValueLayout.JAVA_FLOAT, 0);
        }
    }

    /**
     * Sets speech rate / speed.
     *
     * @param rate normalized speech rate between 0.0 and 1.0 (0.5 is normal rate)
     */
    public void setRate(float rate) {
        checkClosed();
        if (!Float.isFinite(rate) || rate < 0.0f || rate > 1.0f) {
            throw new PrismException.RangeOutOfBounds("Rate must be between 0.0 and 1.0");
        }
        int res = prism_h.prism_backend_set_rate(handle, rate);
        PrismException.throwIfError(res);
    }

    /**
     * Gets current speech rate.
     *
     * @return current normalized speech rate between 0.0 and 1.0
     */
    public float getRate() {
        checkClosed();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outRate = arena.allocate(ValueLayout.JAVA_FLOAT);
            int res = prism_h.prism_backend_get_rate(handle, outRate);
            PrismException.throwIfError(res);
            return outRate.get(ValueLayout.JAVA_FLOAT, 0);
        }
    }

    /**
     * Sets speech pitch.
     *
     * @param pitch normalized speech pitch between 0.0 and 1.0 (0.5 is normal pitch)
     */
    public void setPitch(float pitch) {
        checkClosed();
        if (!Float.isFinite(pitch) || pitch < 0.0f || pitch > 1.0f) {
            throw new PrismException.RangeOutOfBounds("Pitch must be between 0.0 and 1.0");
        }
        int res = prism_h.prism_backend_set_pitch(handle, pitch);
        PrismException.throwIfError(res);
    }

    /**
     * Gets current speech pitch.
     *
     * @return current normalized speech pitch between 0.0 and 1.0
     */
    public float getPitch() {
        checkClosed();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outPitch = arena.allocate(ValueLayout.JAVA_FLOAT);
            int res = prism_h.prism_backend_get_pitch(handle, outPitch);
            PrismException.throwIfError(res);
            return outPitch.get(ValueLayout.JAVA_FLOAT, 0);
        }
    }

    /**
     * Refreshes the list of available voices.
     */
    public void refreshVoices() {
        checkClosed();
        int res = prism_h.prism_backend_refresh_voices(handle);
        PrismException.throwIfError(res);
    }

    /**
     * Gets the number of available voices for this backend.
     *
     * @return number of voices
     */
    public int getVoicesCount() {
        checkClosed();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outCount = arena.allocate(ValueLayout.JAVA_LONG);
            int res = prism_h.prism_backend_count_voices(handle, outCount);
            PrismException.throwIfError(res);
            return (int) outCount.get(ValueLayout.JAVA_LONG, 0);
        }
    }

    /**
     * Gets the human-readable name of a voice by its index.
     *
     * @param voiceIndex 0-based voice index
     * @return voice name
     */
    public String getVoiceName(int voiceIndex) {
        checkClosed();
        if (voiceIndex < 0) {
            throw new PrismException.RangeOutOfBounds("Voice index must be non-negative: " + voiceIndex);
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outName = arena.allocate(prism_h.C_POINTER);
            int res = prism_h.prism_backend_get_voice_name(handle, voiceIndex, outName);
            PrismException.throwIfError(res);
            MemorySegment ptr = outName.get(prism_h.C_POINTER, 0);
            return readCString(ptr);
        }
    }

    /**
     * Gets the BCP-47 language tag of a voice by its index.
     *
     * @param voiceIndex 0-based voice index
     * @return voice language code (e.g. "en-US")
     */
    public String getVoiceLanguage(int voiceIndex) {
        checkClosed();
        if (voiceIndex < 0) {
            throw new PrismException.RangeOutOfBounds("Voice index must be non-negative: " + voiceIndex);
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outLang = arena.allocate(prism_h.C_POINTER);
            int res = prism_h.prism_backend_get_voice_language(handle, voiceIndex, outLang);
            PrismException.throwIfError(res);
            MemorySegment ptr = outLang.get(prism_h.C_POINTER, 0);
            return readCString(ptr);
        }
    }

    /**
     * Sets the active voice by index.
     *
     * @param voiceIndex 0-based voice index
     */
    public void setVoice(int voiceIndex) {
        checkClosed();
        if (voiceIndex < 0) {
            throw new PrismException.RangeOutOfBounds("Voice index must be non-negative: " + voiceIndex);
        }
        int res = prism_h.prism_backend_set_voice(handle, voiceIndex);
        PrismException.throwIfError(res);
    }

    /**
     * Gets the index of the currently active voice.
     *
     * @return active voice index
     */
    public int getVoice() {
        checkClosed();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outVoice = arena.allocate(ValueLayout.JAVA_LONG);
            int res = prism_h.prism_backend_get_voice(handle, outVoice);
            PrismException.throwIfError(res);
            return (int) outVoice.get(ValueLayout.JAVA_LONG, 0);
        }
    }

    /**
     * Gets the number of audio channels produced by this backend.
     *
     * @return channel count (1 for mono, 2 for stereo)
     */
    public int getChannels() {
        checkClosed();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outChannels = arena.allocate(ValueLayout.JAVA_LONG);
            int res = prism_h.prism_backend_get_channels(handle, outChannels);
            PrismException.throwIfError(res);
            return (int) outChannels.get(ValueLayout.JAVA_LONG, 0);
        }
    }

    /**
     * Gets the audio sample rate produced by this backend.
     *
     * @return sample rate in Hz (e.g. 44100)
     */
    public int getSampleRate() {
        checkClosed();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outSampleRate = arena.allocate(ValueLayout.JAVA_LONG);
            int res = prism_h.prism_backend_get_sample_rate(handle, outSampleRate);
            PrismException.throwIfError(res);
            return (int) outSampleRate.get(ValueLayout.JAVA_LONG, 0);
        }
    }

    /**
     * Gets the bit depth of the audio produced by this backend.
     *
     * @return bit depth (e.g. 16, 32)
     */
    public int getBitDepth() {
        checkClosed();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outBitDepth = arena.allocate(ValueLayout.JAVA_LONG);
            int res = prism_h.prism_backend_get_bit_depth(handle, outBitDepth);
            PrismException.throwIfError(res);
            return (int) outBitDepth.get(ValueLayout.JAVA_LONG, 0);
        }
    }

    @Override
    public void close() {
        if (!closed) {
            synchronized (this) {
                if (!closed) {
                    closed = true;
                    prism_h.prism_backend_free(handle);
                }
            }
        }
    }

    /**
     * Checks whether this backend instance has been closed.
     *
     * @return {@code true} if closed, {@code false} otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Backend is already closed");
        }
    }

    private void validateText(String text) {
        if (text == null || text.isEmpty()) {
            throw new PrismException.InvalidParam("Text must not be null or empty");
        }
        if (text.indexOf('\0') >= 0) {
            throw new PrismException.InvalidParam("Text must not contain embedded null characters");
        }
    }

    static boolean isNull(MemorySegment ptr) {
        return ptr == null || ptr.address() == 0;
    }

    static String readCString(MemorySegment ptr) {
        if (isNull(ptr)) {
            return "";
        }
        if (ptr.byteSize() == 0) {
            ptr = ptr.reinterpret(Long.MAX_VALUE);
        }
        return ptr.getString(0);
    }
}
