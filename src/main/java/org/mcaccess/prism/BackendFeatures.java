package org.mcaccess.prism;

/**
 * Represents the feature flags supported by a PRISM backend.
 *
 * @param isSupportedAtRuntime                   whether the backend is supported at runtime on the current platform
 * @param supportsSpeak                          whether the backend supports speaking text aloud
 * @param supportsSpeakToMemory                  whether the backend supports synthesizing speech to memory PCM samples
 * @param supportsBraille                        whether the backend supports Braille output
 * @param supportsOutput                         whether the backend supports combined speech and Braille output
 * @param supportsIsSpeaking                     whether the backend supports querying speaking status
 * @param supportsStop                           whether the backend supports stopping speech
 * @param supportsPause                          whether the backend supports pausing speech
 * @param supportsResume                         whether the backend supports resuming paused speech
 * @param supportsSetVolume                      whether the backend supports setting speech volume
 * @param supportsGetVolume                      whether the backend supports getting speech volume
 * @param supportsSetRate                        whether the backend supports setting speech rate
 * @param supportsGetRate                        whether the backend supports getting speech rate
 * @param supportsSetPitch                       whether the backend supports setting speech pitch
 * @param supportsGetPitch                       whether the backend supports getting speech pitch
 * @param supportsRefreshVoices                  whether the backend supports refreshing the voice list
 * @param supportsCountVoices                    whether the backend supports counting available voices
 * @param supportsGetVoiceName                   whether the backend supports querying voice names
 * @param supportsGetVoiceLanguage               whether the backend supports querying voice languages
 * @param supportsGetVoice                       whether the backend supports getting the active voice index
 * @param supportsSetVoice                       whether the backend supports setting the active voice index
 * @param supportsGetChannels                    whether the backend supports querying audio channel count
 * @param supportsGetSampleRate                  whether the backend supports querying audio sample rate
 * @param supportsGetBitDepth                    whether the backend supports querying audio bit depth
 * @param performsSilenceTrimmingOnSpeak         whether the backend trims silence when speaking aloud
 * @param performsSilenceTrimmingOnSpeakToMemory whether the backend trims silence when synthesizing to memory
 * @param supportsSpeakSsml                      whether the backend supports SSML markup in {@code speak}
 * @param supportsSpeakToMemorySsml              whether the backend supports SSML markup in {@code speakToMemory}
 */
public record BackendFeatures(
        boolean isSupportedAtRuntime,
        boolean supportsSpeak,
        boolean supportsSpeakToMemory,
        boolean supportsBraille,
        boolean supportsOutput,
        boolean supportsIsSpeaking,
        boolean supportsStop,
        boolean supportsPause,
        boolean supportsResume,
        boolean supportsSetVolume,
        boolean supportsGetVolume,
        boolean supportsSetRate,
        boolean supportsGetRate,
        boolean supportsSetPitch,
        boolean supportsGetPitch,
        boolean supportsRefreshVoices,
        boolean supportsCountVoices,
        boolean supportsGetVoiceName,
        boolean supportsGetVoiceLanguage,
        boolean supportsGetVoice,
        boolean supportsSetVoice,
        boolean supportsGetChannels,
        boolean supportsGetSampleRate,
        boolean supportsGetBitDepth,
        boolean performsSilenceTrimmingOnSpeak,
        boolean performsSilenceTrimmingOnSpeakToMemory,
        boolean supportsSpeakSsml,
        boolean supportsSpeakToMemorySsml
) {
    /** Bitmask indicating the backend is supported at runtime. */
    public static final long BIT_IS_SUPPORTED_AT_RUNTIME = 1L << 0;
    /** Bitmask indicating the backend supports speaking aloud. */
    public static final long BIT_SUPPORTS_SPEAK = 1L << 2;
    /** Bitmask indicating the backend supports synthesizing speech to memory. */
    public static final long BIT_SUPPORTS_SPEAK_TO_MEMORY = 1L << 3;
    /** Bitmask indicating the backend supports Braille output. */
    public static final long BIT_SUPPORTS_BRAILLE = 1L << 4;
    /** Bitmask indicating the backend supports combined speech and Braille output. */
    public static final long BIT_SUPPORTS_OUTPUT = 1L << 5;
    /** Bitmask indicating the backend supports querying speaking status. */
    public static final long BIT_SUPPORTS_IS_SPEAKING = 1L << 6;
    /** Bitmask indicating the backend supports stopping speech. */
    public static final long BIT_SUPPORTS_STOP = 1L << 7;
    /** Bitmask indicating the backend supports pausing speech. */
    public static final long BIT_SUPPORTS_PAUSE = 1L << 8;
    /** Bitmask indicating the backend supports resuming paused speech. */
    public static final long BIT_SUPPORTS_RESUME = 1L << 9;
    /** Bitmask indicating the backend supports setting volume. */
    public static final long BIT_SUPPORTS_SET_VOLUME = 1L << 10;
    /** Bitmask indicating the backend supports getting volume. */
    public static final long BIT_SUPPORTS_GET_VOLUME = 1L << 11;
    /** Bitmask indicating the backend supports setting speech rate. */
    public static final long BIT_SUPPORTS_SET_RATE = 1L << 12;
    /** Bitmask indicating the backend supports getting speech rate. */
    public static final long BIT_SUPPORTS_GET_RATE = 1L << 13;
    /** Bitmask indicating the backend supports setting speech pitch. */
    public static final long BIT_SUPPORTS_SET_PITCH = 1L << 14;
    /** Bitmask indicating the backend supports getting speech pitch. */
    public static final long BIT_SUPPORTS_GET_PITCH = 1L << 15;
    /** Bitmask indicating the backend supports refreshing voices. */
    public static final long BIT_SUPPORTS_REFRESH_VOICES = 1L << 16;
    /** Bitmask indicating the backend supports counting voices. */
    public static final long BIT_SUPPORTS_COUNT_VOICES = 1L << 17;
    /** Bitmask indicating the backend supports querying voice names. */
    public static final long BIT_SUPPORTS_GET_VOICE_NAME = 1L << 18;
    /** Bitmask indicating the backend supports querying voice languages. */
    public static final long BIT_SUPPORTS_GET_VOICE_LANGUAGE = 1L << 19;
    /** Bitmask indicating the backend supports getting the active voice. */
    public static final long BIT_SUPPORTS_GET_VOICE = 1L << 20;
    /** Bitmask indicating the backend supports setting the active voice. */
    public static final long BIT_SUPPORTS_SET_VOICE = 1L << 21;
    /** Bitmask indicating the backend supports querying audio channel count. */
    public static final long BIT_SUPPORTS_GET_CHANNELS = 1L << 22;
    /** Bitmask indicating the backend supports querying audio sample rate. */
    public static final long BIT_SUPPORTS_GET_SAMPLE_RATE = 1L << 23;
    /** Bitmask indicating the backend supports querying audio bit depth. */
    public static final long BIT_SUPPORTS_GET_BIT_DEPTH = 1L << 24;
    /** Bitmask indicating the backend trims silence when speaking aloud. */
    public static final long BIT_PERFORMS_SILENCE_TRIMMING_ON_SPEAK = 1L << 25;
    /** Bitmask indicating the backend trims silence when synthesizing to memory. */
    public static final long BIT_PERFORMS_SILENCE_TRIMMING_ON_SPEAK_TO_MEMORY = 1L << 26;
    /** Bitmask indicating the backend supports SSML in {@code speak}. */
    public static final long BIT_SUPPORTS_SPEAK_SSML = 1L << 27;
    /** Bitmask indicating the backend supports SSML in {@code speakToMemory}. */
    public static final long BIT_SUPPORTS_SPEAK_TO_MEMORY_SSML = 1L << 28;

    /**
     * Constructs a {@link BackendFeatures} instance from a 64-bit native feature bitmask.
     *
     * @param bits 64-bit feature flags bitmask
     * @return decoded {@link BackendFeatures}
     */
    public static BackendFeatures fromBits(long bits) {
        return new BackendFeatures(
                (bits & BIT_IS_SUPPORTED_AT_RUNTIME) != 0,
                (bits & BIT_SUPPORTS_SPEAK) != 0,
                (bits & BIT_SUPPORTS_SPEAK_TO_MEMORY) != 0,
                (bits & BIT_SUPPORTS_BRAILLE) != 0,
                (bits & BIT_SUPPORTS_OUTPUT) != 0,
                (bits & BIT_SUPPORTS_IS_SPEAKING) != 0,
                (bits & BIT_SUPPORTS_STOP) != 0,
                (bits & BIT_SUPPORTS_PAUSE) != 0,
                (bits & BIT_SUPPORTS_RESUME) != 0,
                (bits & BIT_SUPPORTS_SET_VOLUME) != 0,
                (bits & BIT_SUPPORTS_GET_VOLUME) != 0,
                (bits & BIT_SUPPORTS_SET_RATE) != 0,
                (bits & BIT_SUPPORTS_GET_RATE) != 0,
                (bits & BIT_SUPPORTS_SET_PITCH) != 0,
                (bits & BIT_SUPPORTS_GET_PITCH) != 0,
                (bits & BIT_SUPPORTS_REFRESH_VOICES) != 0,
                (bits & BIT_SUPPORTS_COUNT_VOICES) != 0,
                (bits & BIT_SUPPORTS_GET_VOICE_NAME) != 0,
                (bits & BIT_SUPPORTS_GET_VOICE_LANGUAGE) != 0,
                (bits & BIT_SUPPORTS_GET_VOICE) != 0,
                (bits & BIT_SUPPORTS_SET_VOICE) != 0,
                (bits & BIT_SUPPORTS_GET_CHANNELS) != 0,
                (bits & BIT_SUPPORTS_GET_SAMPLE_RATE) != 0,
                (bits & BIT_SUPPORTS_GET_BIT_DEPTH) != 0,
                (bits & BIT_PERFORMS_SILENCE_TRIMMING_ON_SPEAK) != 0,
                (bits & BIT_PERFORMS_SILENCE_TRIMMING_ON_SPEAK_TO_MEMORY) != 0,
                (bits & BIT_SUPPORTS_SPEAK_SSML) != 0,
                (bits & BIT_SUPPORTS_SPEAK_TO_MEMORY_SSML) != 0
        );
    }

    /**
     * Converts this {@link BackendFeatures} record back into a 64-bit native feature bitmask.
     *
     * @return 64-bit feature flags bitmask
     */
    public long toBits() {
        long bits = 0;
        if (isSupportedAtRuntime) bits |= BIT_IS_SUPPORTED_AT_RUNTIME;
        if (supportsSpeak) bits |= BIT_SUPPORTS_SPEAK;
        if (supportsSpeakToMemory) bits |= BIT_SUPPORTS_SPEAK_TO_MEMORY;
        if (supportsBraille) bits |= BIT_SUPPORTS_BRAILLE;
        if (supportsOutput) bits |= BIT_SUPPORTS_OUTPUT;
        if (supportsIsSpeaking) bits |= BIT_SUPPORTS_IS_SPEAKING;
        if (supportsStop) bits |= BIT_SUPPORTS_STOP;
        if (supportsPause) bits |= BIT_SUPPORTS_PAUSE;
        if (supportsResume) bits |= BIT_SUPPORTS_RESUME;
        if (supportsSetVolume) bits |= BIT_SUPPORTS_SET_VOLUME;
        if (supportsGetVolume) bits |= BIT_SUPPORTS_GET_VOLUME;
        if (supportsSetRate) bits |= BIT_SUPPORTS_SET_RATE;
        if (supportsGetRate) bits |= BIT_SUPPORTS_GET_RATE;
        if (supportsSetPitch) bits |= BIT_SUPPORTS_SET_PITCH;
        if (supportsGetPitch) bits |= BIT_SUPPORTS_GET_PITCH;
        if (supportsRefreshVoices) bits |= BIT_SUPPORTS_REFRESH_VOICES;
        if (supportsCountVoices) bits |= BIT_SUPPORTS_COUNT_VOICES;
        if (supportsGetVoiceName) bits |= BIT_SUPPORTS_GET_VOICE_NAME;
        if (supportsGetVoiceLanguage) bits |= BIT_SUPPORTS_GET_VOICE_LANGUAGE;
        if (supportsGetVoice) bits |= BIT_SUPPORTS_GET_VOICE;
        if (supportsSetVoice) bits |= BIT_SUPPORTS_SET_VOICE;
        if (supportsGetChannels) bits |= BIT_SUPPORTS_GET_CHANNELS;
        if (supportsGetSampleRate) bits |= BIT_SUPPORTS_GET_SAMPLE_RATE;
        if (supportsGetBitDepth) bits |= BIT_SUPPORTS_GET_BIT_DEPTH;
        if (performsSilenceTrimmingOnSpeak) bits |= BIT_PERFORMS_SILENCE_TRIMMING_ON_SPEAK;
        if (performsSilenceTrimmingOnSpeakToMemory) bits |= BIT_PERFORMS_SILENCE_TRIMMING_ON_SPEAK_TO_MEMORY;
        if (supportsSpeakSsml) bits |= BIT_SUPPORTS_SPEAK_SSML;
        if (supportsSpeakToMemorySsml) bits |= BIT_SUPPORTS_SPEAK_TO_MEMORY_SSML;
        return bits;
    }
}
