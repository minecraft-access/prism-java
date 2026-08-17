package org.mcaccess.prism;

/**
 * Represents the feature flags supported by a PRISM backend.
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
    public static final long BIT_IS_SUPPORTED_AT_RUNTIME = 1L << 0;
    public static final long BIT_SUPPORTS_SPEAK = 1L << 2;
    public static final long BIT_SUPPORTS_SPEAK_TO_MEMORY = 1L << 3;
    public static final long BIT_SUPPORTS_BRAILLE = 1L << 4;
    public static final long BIT_SUPPORTS_OUTPUT = 1L << 5;
    public static final long BIT_SUPPORTS_IS_SPEAKING = 1L << 6;
    public static final long BIT_SUPPORTS_STOP = 1L << 7;
    public static final long BIT_SUPPORTS_PAUSE = 1L << 8;
    public static final long BIT_SUPPORTS_RESUME = 1L << 9;
    public static final long BIT_SUPPORTS_SET_VOLUME = 1L << 10;
    public static final long BIT_SUPPORTS_GET_VOLUME = 1L << 11;
    public static final long BIT_SUPPORTS_SET_RATE = 1L << 12;
    public static final long BIT_SUPPORTS_GET_RATE = 1L << 13;
    public static final long BIT_SUPPORTS_SET_PITCH = 1L << 14;
    public static final long BIT_SUPPORTS_GET_PITCH = 1L << 15;
    public static final long BIT_SUPPORTS_REFRESH_VOICES = 1L << 16;
    public static final long BIT_SUPPORTS_COUNT_VOICES = 1L << 17;
    public static final long BIT_SUPPORTS_GET_VOICE_NAME = 1L << 18;
    public static final long BIT_SUPPORTS_GET_VOICE_LANGUAGE = 1L << 19;
    public static final long BIT_SUPPORTS_GET_VOICE = 1L << 20;
    public static final long BIT_SUPPORTS_SET_VOICE = 1L << 21;
    public static final long BIT_SUPPORTS_GET_CHANNELS = 1L << 22;
    public static final long BIT_SUPPORTS_GET_SAMPLE_RATE = 1L << 23;
    public static final long BIT_SUPPORTS_GET_BIT_DEPTH = 1L << 24;
    public static final long BIT_PERFORMS_SILENCE_TRIMMING_ON_SPEAK = 1L << 25;
    public static final long BIT_PERFORMS_SILENCE_TRIMMING_ON_SPEAK_TO_MEMORY = 1L << 26;
    public static final long BIT_SUPPORTS_SPEAK_SSML = 1L << 27;
    public static final long BIT_SUPPORTS_SPEAK_TO_MEMORY_SSML = 1L << 28;

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
