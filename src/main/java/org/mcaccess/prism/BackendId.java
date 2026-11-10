package org.mcaccess.prism;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Identifiers for built-in PRISM speech and screen reader backends.
 */
public enum BackendId {
    /** Invalid or unknown backend ID. */
    INVALID(0L),
    /** Microsoft Speech API (SAPI 5) backend. */
    SAPI(0x1D6DF72422CEEE66L),
    /** Apple AVSpeechSynthesizer backend. */
    AV_SPEECH(0x28E3429577805C24L),
    /** Apple VoiceOver screen reader backend. */
    VOICE_OVER(0xCB4897961A754BCBL),
    /** Linux Speech Dispatcher backend. */
    SPEECH_DISPATCHER(0xE3D6F895D949EBFEL),
    /** NVDA screen reader backend. */
    NVDA(0x89CC19C5C4AC1A56L),
    /** JAWS screen reader backend. */
    JAWS(0xAC3D60E9BD84B53EL),
    /** Windows OneCore speech synthesis backend. */
    ONE_CORE(0x6797D32F0D994CB4L),
    /** Linux Orca screen reader backend. */
    ORCA(0x10AA1FC05A17F96CL),
    /** Android Accessibility / TalkBack screen reader backend. */
    ANDROID_SCREEN_READER(0xD199C175AEEC494BL),
    /** Android TextToSpeech engine backend. */
    ANDROID_TTS(0xBC175831BFE4E5CCL),
    /** Web Speech API synthesis backend. */
    WEB_SPEECH(0x3572538D44D44A8FL),
    /** Windows UI Automation (UIA) notification backend. */
    UIA(0x6238F019DB678F8EL),
    /** Zhengdu Screen Reader (ZDSR) backend. */
    ZDSR(0x3D93C56C9E7F2A2EL),
    /** ZoomText screen magnifier and reader backend. */
    ZOOM_TEXT(0xAE439D62DC7B1479L),
    /** BoyPCReader screen reader backend. */
    BOY_PC_READER(0x285ABA1C16F3300FL),
    /** PC-Talker screen reader backend. */
    PC_TALKER(0x344B951962E3B835L),
    /** SenseReader screen reader backend. */
    SENSE_READER(0xED4760890B55C2F2L),
    /** System Access screen reader backend. */
    SYSTEM_ACCESS(0x8380F2A37B2C3EB6L),
    /** Window-Eyes screen reader backend. */
    WINDOW_EYES(0x9120D89908785C13L),
    /** Spiel speech synthesis backend. */
    SPIEL(0x478B44F14AD3D89CL);

    private final long id;

    BackendId(long id) {
        this.id = id;
    }

    /**
     * Gets the 64-bit native PRISM backend identifier.
     *
     * @return 64-bit backend ID
     */
    public long getId() {
        return id;
    }

    private static final Map<Long, BackendId> BY_ID = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(BackendId::getId, Function.identity()));

    /**
     * Looks up a known {@link BackendId} by its 64-bit native identifier.
     *
     * @param id 64-bit backend ID
     * @return optional containing the matching {@link BackendId} if known
     */
    public static Optional<BackendId> fromId(long id) {
        return Optional.ofNullable(BY_ID.get(id));
    }
}
