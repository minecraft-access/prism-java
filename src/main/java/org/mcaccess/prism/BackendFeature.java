package org.mcaccess.prism;

import java.util.EnumSet;

public enum BackendFeature {
    IS_SUPPORTED_AT_RUNTIME(1L << 0), SUPPORTS_SPEAK(1L << 2), SUPPORTS_SPEAK_TO_MEMORY(1L << 3),
    SUPPORTS_BRAILLE(1L << 4), SUPPORTS_OUTPUT(1L << 5), SUPPORTS_IS_SPEAKING(1L << 6), SUPPORTS_STOP(1L << 7),
    SUPPORTS_PAUSE(1L << 8), SUPPORTS_RESUME(1L << 9), SUPPORTS_SET_VOLUME(1L << 10), SUPPORTS_GET_VOLUME(1L << 11),
    SUPPORTS_SET_RATE(1L << 12), SUPPORTS_GET_RATE(1L << 13), SUPPORTS_SET_PITCH(1L << 14),
    SUPPORTS_GET_PITCH(1L << 15), SUPPORTS_REFRESH_VOICES(1L << 16), SUPPORTS_COUNT_VOICES(1L << 17),
    SUPPORTS_GET_VOICE_NAME(1L << 18), SUPPORTS_GET_VOICE_LANGUAGE(1L << 19), SUPPORTS_GET_VOICE(1L << 20),
    SUPPORTS_SET_VOICE(1L << 21), SUPPORTS_GET_CHANNELS(1L << 22), SUPPORTS_GET_SAMPLE_RATE(1L << 23),
    SUPPORTS_GET_BIT_DEPTH(1L << 24), PERFORMS_SILENCE_TRIMMING_ON_SPEAK(1L << 25),
    PERFORMS_SILENCE_TRIMMING_ON_SPEAK_TO_MEMORY(1L << 26), SUPPORTS_SPEAK_SSML(1L << 27),
    SUPPORTS_SPEAK_TO_MEMORY_SSML(1L << 28);

    private final long mask;

    BackendFeature(long mask) {
        this.mask = mask;
    }

    /**
     * Gets the raw bitmask value of this feature.
     */
    public long getMask() {
        return mask;
    }

    /**
     * Helper method to check if a backend's returned feature-set includes this feature.
     * 
     * @param backendFeatures The raw 64-bit integer returned from prism_backend_get_features.
     * @return true if the feature is present.
     */
    public boolean isSupportedBy(long backendFeatures) {
        return (backendFeatures & this.mask) == this.mask;
    }

    /**
     * Expands a raw feature bitmask into the set of features it names. Bits this binding does not recognise are ignored.
     *
     * @param backendFeatures The raw 64-bit value from {@link Backend#getFeatures()}.
     * @return A new, caller-owned set.
     */
    public static EnumSet<BackendFeature> decode(long backendFeatures) {
        EnumSet<BackendFeature> features = EnumSet.noneOf(BackendFeature.class);
        for (BackendFeature feature : values()) {
            if (feature.isSupportedBy(backendFeatures)) {
                features.add(feature);
            }
        }
        return features;
    }
}