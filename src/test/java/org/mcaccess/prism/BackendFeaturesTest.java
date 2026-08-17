package org.mcaccess.prism;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BackendFeaturesTest {

    @Test
    void testEmptyFeatures() {
        BackendFeatures features = BackendFeatures.fromBits(0L);
        assertThat(features.isSupportedAtRuntime()).isFalse();
        assertThat(features.supportsSpeak()).isFalse();
        assertThat(features.supportsSpeakToMemory()).isFalse();
        assertThat(features.supportsBraille()).isFalse();
        assertThat(features.supportsOutput()).isFalse();
        assertThat(features.supportsIsSpeaking()).isFalse();
        assertThat(features.supportsStop()).isFalse();
        assertThat(features.supportsPause()).isFalse();
        assertThat(features.supportsResume()).isFalse();
        assertThat(features.supportsSetVolume()).isFalse();
        assertThat(features.supportsGetVolume()).isFalse();
        assertThat(features.supportsSetRate()).isFalse();
        assertThat(features.supportsGetRate()).isFalse();
        assertThat(features.supportsSetPitch()).isFalse();
        assertThat(features.supportsGetPitch()).isFalse();
        assertThat(features.supportsRefreshVoices()).isFalse();
        assertThat(features.supportsCountVoices()).isFalse();
        assertThat(features.supportsGetVoiceName()).isFalse();
        assertThat(features.supportsGetVoiceLanguage()).isFalse();
        assertThat(features.supportsGetVoice()).isFalse();
        assertThat(features.supportsSetVoice()).isFalse();
        assertThat(features.supportsGetChannels()).isFalse();
        assertThat(features.supportsGetSampleRate()).isFalse();
        assertThat(features.supportsGetBitDepth()).isFalse();
        assertThat(features.performsSilenceTrimmingOnSpeak()).isFalse();
        assertThat(features.performsSilenceTrimmingOnSpeakToMemory()).isFalse();
        assertThat(features.supportsSpeakSsml()).isFalse();
        assertThat(features.supportsSpeakToMemorySsml()).isFalse();
        assertThat(features.toBits()).isEqualTo(0L);
    }

    @Test
    void testIndividualFeatureBits() {
        BackendFeatures features = BackendFeatures.fromBits(
                BackendFeatures.BIT_IS_SUPPORTED_AT_RUNTIME |
                BackendFeatures.BIT_SUPPORTS_SPEAK |
                BackendFeatures.BIT_SUPPORTS_SET_VOLUME |
                BackendFeatures.BIT_SUPPORTS_GET_VOLUME
        );

        assertThat(features.isSupportedAtRuntime()).isTrue();
        assertThat(features.supportsSpeak()).isTrue();
        assertThat(features.supportsSetVolume()).isTrue();
        assertThat(features.supportsGetVolume()).isTrue();
        assertThat(features.supportsBraille()).isFalse();
        assertThat(features.supportsStop()).isFalse();

        long expectedBits = BackendFeatures.BIT_IS_SUPPORTED_AT_RUNTIME |
                BackendFeatures.BIT_SUPPORTS_SPEAK |
                BackendFeatures.BIT_SUPPORTS_SET_VOLUME |
                BackendFeatures.BIT_SUPPORTS_GET_VOLUME;

        assertThat(features.toBits()).isEqualTo(expectedBits);
    }

    @Test
    void testAllFeatureBitsRoundTrip() {
        long allBits = (1L << 0) | (1L << 2) | (1L << 3) | (1L << 4) | (1L << 5) |
                (1L << 6) | (1L << 7) | (1L << 8) | (1L << 9) | (1L << 10) |
                (1L << 11) | (1L << 12) | (1L << 13) | (1L << 14) | (1L << 15) |
                (1L << 16) | (1L << 17) | (1L << 18) | (1L << 19) | (1L << 20) |
                (1L << 21) | (1L << 22) | (1L << 23) | (1L << 24) | (1L << 25) |
                (1L << 26) | (1L << 27) | (1L << 28);

        BackendFeatures features = BackendFeatures.fromBits(allBits);
        assertThat(features.isSupportedAtRuntime()).isTrue();
        assertThat(features.supportsSpeak()).isTrue();
        assertThat(features.supportsSpeakToMemory()).isTrue();
        assertThat(features.supportsBraille()).isTrue();
        assertThat(features.supportsOutput()).isTrue();
        assertThat(features.supportsIsSpeaking()).isTrue();
        assertThat(features.supportsStop()).isTrue();
        assertThat(features.supportsPause()).isTrue();
        assertThat(features.supportsResume()).isTrue();
        assertThat(features.supportsSetVolume()).isTrue();
        assertThat(features.supportsGetVolume()).isTrue();
        assertThat(features.supportsSetRate()).isTrue();
        assertThat(features.supportsGetRate()).isTrue();
        assertThat(features.supportsSetPitch()).isTrue();
        assertThat(features.supportsGetPitch()).isTrue();
        assertThat(features.supportsRefreshVoices()).isTrue();
        assertThat(features.supportsCountVoices()).isTrue();
        assertThat(features.supportsGetVoiceName()).isTrue();
        assertThat(features.supportsGetVoiceLanguage()).isTrue();
        assertThat(features.supportsGetVoice()).isTrue();
        assertThat(features.supportsSetVoice()).isTrue();
        assertThat(features.supportsGetChannels()).isTrue();
        assertThat(features.supportsGetSampleRate()).isTrue();
        assertThat(features.supportsGetBitDepth()).isTrue();
        assertThat(features.performsSilenceTrimmingOnSpeak()).isTrue();
        assertThat(features.performsSilenceTrimmingOnSpeakToMemory()).isTrue();
        assertThat(features.supportsSpeakSsml()).isTrue();
        assertThat(features.supportsSpeakToMemorySsml()).isTrue();

        assertThat(features.toBits()).isEqualTo(allBits);
    }
}
