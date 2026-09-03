package org.mcaccess.prism;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BackendIdTest {

    @Test
    void knownIdentifiersMatchPrism() {
        assertThat(BackendId.INVALID.id()).isEqualTo(0L);
        assertThat(BackendId.SAPI.id()).isEqualTo(0x1D6DF72422CEEE66L);
        assertThat(BackendId.AV_SPEECH.id()).isEqualTo(0x28E3429577805C24L);
        assertThat(BackendId.VOICE_OVER.id()).isEqualTo(0xCB4897961A754BCBL);
        assertThat(BackendId.SPEECH_DISPATCHER.id()).isEqualTo(0xE3D6F895D949EBFEL);
        assertThat(BackendId.NVDA.id()).isEqualTo(0x89CC19C5C4AC1A56L);
        assertThat(BackendId.JAWS.id()).isEqualTo(0xAC3D60E9BD84B53EL);
        assertThat(BackendId.ONE_CORE.id()).isEqualTo(0x6797D32F0D994CB4L);
        assertThat(BackendId.ORCA.id()).isEqualTo(0x10AA1FC05A17F96CL);
        assertThat(BackendId.ANDROID_SCREEN_READER.id()).isEqualTo(0xD199C175AEEC494BL);
        assertThat(BackendId.ANDROID_TTS.id()).isEqualTo(0xBC175831BFE4E5CCL);
        assertThat(BackendId.WEB_SPEECH.id()).isEqualTo(0x3572538D44D44A8FL);
        assertThat(BackendId.UIA.id()).isEqualTo(0x6238F019DB678F8EL);
        assertThat(BackendId.ZDSR.id()).isEqualTo(0x3D93C56C9E7F2A2EL);
        assertThat(BackendId.ZOOM_TEXT.id()).isEqualTo(0xAE439D62DC7B1479L);
        assertThat(BackendId.BOY_PC_READER.id()).isEqualTo(0x285ABA1C16F3300FL);
        assertThat(BackendId.PC_TALKER.id()).isEqualTo(0x344B951962E3B835L);
        assertThat(BackendId.SENSE_READER.id()).isEqualTo(0xED4760890B55C2F2L);
        assertThat(BackendId.SYSTEM_ACCESS.id()).isEqualTo(0x8380F2A37B2C3EB6L);
        assertThat(BackendId.WINDOW_EYES.id()).isEqualTo(0x9120D89908785C13L);
        assertThat(BackendId.SPIEL.id()).isEqualTo(0x478B44F14AD3D89CL);
    }

    @Test
    void unknownIdentifiersSurviveIntact() {
        long custom = 0x1122334455667788L;
        assertThat(new BackendId(custom).id()).isEqualTo(custom);
        assertThat(new BackendId(custom)).isNotEqualTo(BackendId.INVALID);
    }

    @Test
    void equalityIsByValue() {
        assertThat(new BackendId(0x89CC19C5C4AC1A56L)).isEqualTo(BackendId.NVDA);
        assertThat(new BackendId(0x89CC19C5C4AC1A56L)).hasSameHashCodeAs(BackendId.NVDA);
    }

    @Test
    void invalidIsTheZeroIdentifier() {
        assertThat(BackendId.INVALID.isInvalid()).isTrue();
        assertThat(new BackendId(0L).isInvalid()).isTrue();
        assertThat(BackendId.NVDA.isInvalid()).isFalse();
    }

    @Test
    void toStringIsFixedWidthHex() {
        assertThat(BackendId.NVDA).hasToString("0x89CC19C5C4AC1A56");
        assertThat(BackendId.INVALID).hasToString("0x0000000000000000");
    }
}
