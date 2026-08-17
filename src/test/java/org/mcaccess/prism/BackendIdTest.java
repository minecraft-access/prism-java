package org.mcaccess.prism;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BackendIdTest {

    @Test
    void testAllKnownBackendIds() {
        assertThat(BackendId.INVALID.getId()).isEqualTo(0L);
        assertThat(BackendId.SAPI.getId()).isEqualTo(0x1D6DF72422CEEE66L);
        assertThat(BackendId.AV_SPEECH.getId()).isEqualTo(0x28E3429577805C24L);
        assertThat(BackendId.VOICE_OVER.getId()).isEqualTo(0xCB4897961A754BCBL);
        assertThat(BackendId.SPEECH_DISPATCHER.getId()).isEqualTo(0xE3D6F895D949EBFEL);
        assertThat(BackendId.NVDA.getId()).isEqualTo(0x89CC19C5C4AC1A56L);
        assertThat(BackendId.JAWS.getId()).isEqualTo(0xAC3D60E9BD84B53EL);
        assertThat(BackendId.ONE_CORE.getId()).isEqualTo(0x6797D32F0D994CB4L);
        assertThat(BackendId.ORCA.getId()).isEqualTo(0x10AA1FC05A17F96CL);
        assertThat(BackendId.ANDROID_SCREEN_READER.getId()).isEqualTo(0xD199C175AEEC494BL);
        assertThat(BackendId.ANDROID_TTS.getId()).isEqualTo(0xBC175831BFE4E5CCL);
        assertThat(BackendId.WEB_SPEECH.getId()).isEqualTo(0x3572538D44D44A8FL);
        assertThat(BackendId.UIA.getId()).isEqualTo(0x6238F019DB678F8EL);
        assertThat(BackendId.ZDSR.getId()).isEqualTo(0x3D93C56C9E7F2A2EL);
        assertThat(BackendId.ZOOM_TEXT.getId()).isEqualTo(0xAE439D62DC7B1479L);
        assertThat(BackendId.BOY_PC_READER.getId()).isEqualTo(0x285ABA1C16F3300FL);
        assertThat(BackendId.PC_TALKER.getId()).isEqualTo(0x344B951962E3B835L);
        assertThat(BackendId.SENSE_READER.getId()).isEqualTo(0xED4760890B55C2F2L);
        assertThat(BackendId.SYSTEM_ACCESS.getId()).isEqualTo(0x8380F2A37B2C3EB6L);
        assertThat(BackendId.WINDOW_EYES.getId()).isEqualTo(0x9120D89908785C13L);
        assertThat(BackendId.SPIEL.getId()).isEqualTo(0x478B44F14AD3D89CL);
    }

    @Test
    void testFromId() {
        assertThat(BackendId.fromId(0x1D6DF72422CEEE66L)).contains(BackendId.SAPI);
        assertThat(BackendId.fromId(0x89CC19C5C4AC1A56L)).contains(BackendId.NVDA);
        assertThat(BackendId.fromId(0x6797D32F0D994CB4L)).contains(BackendId.ONE_CORE);
        assertThat(BackendId.fromId(0x123456789L)).isEmpty();
    }
}
