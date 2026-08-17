package org.mcaccess.prism;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum BackendId {
    INVALID(0L),
    SAPI(0x1D6DF72422CEEE66L),
    AV_SPEECH(0x28E3429577805C24L),
    VOICE_OVER(0xCB4897961A754BCBL),
    SPEECH_DISPATCHER(0xE3D6F895D949EBFEL),
    NVDA(0x89CC19C5C4AC1A56L),
    JAWS(0xAC3D60E9BD84B53EL),
    ONE_CORE(0x6797D32F0D994CB4L),
    ORCA(0x10AA1FC05A17F96CL),
    ANDROID_SCREEN_READER(0xD199C175AEEC494BL),
    ANDROID_TTS(0xBC175831BFE4E5CCL),
    WEB_SPEECH(0x3572538D44D44A8FL),
    UIA(0x6238F019DB678F8EL),
    ZDSR(0x3D93C56C9E7F2A2EL),
    ZOOM_TEXT(0xAE439D62DC7B1479L),
    BOY_PC_READER(0x285ABA1C16F3300FL),
    PC_TALKER(0x344B951962E3B835L),
    SENSE_READER(0xED4760890B55C2F2L),
    SYSTEM_ACCESS(0x8380F2A37B2C3EB6L),
    WINDOW_EYES(0x9120D89908785C13L),
    SPIEL(0x478B44F14AD3D89CL);

    private final long id;

    BackendId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    private static final Map<Long, BackendId> BY_ID = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(BackendId::getId, Function.identity()));

    public static Optional<BackendId> fromId(long id) {
        return Optional.ofNullable(BY_ID.get(id));
    }
}