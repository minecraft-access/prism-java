package org.mcaccess.prism;

/**
 * The identifier of a backend registered with PRISM.
 */
public record BackendId(long id) {
    /** The identifier PRISM reserves to mean "no backend". */
    public static final BackendId INVALID = new BackendId(0L);

    public static final BackendId SAPI = new BackendId(0x1D6DF72422CEEE66L);
    public static final BackendId AV_SPEECH = new BackendId(0x28E3429577805C24L);
    public static final BackendId VOICE_OVER = new BackendId(0xCB4897961A754BCBL);
    public static final BackendId SPEECH_DISPATCHER = new BackendId(0xE3D6F895D949EBFEL);
    public static final BackendId NVDA = new BackendId(0x89CC19C5C4AC1A56L);
    public static final BackendId JAWS = new BackendId(0xAC3D60E9BD84B53EL);
    public static final BackendId ONE_CORE = new BackendId(0x6797D32F0D994CB4L);
    public static final BackendId ORCA = new BackendId(0x10AA1FC05A17F96CL);
    public static final BackendId ANDROID_SCREEN_READER = new BackendId(0xD199C175AEEC494BL);
    public static final BackendId ANDROID_TTS = new BackendId(0xBC175831BFE4E5CCL);
    public static final BackendId WEB_SPEECH = new BackendId(0x3572538D44D44A8FL);
    public static final BackendId UIA = new BackendId(0x6238F019DB678F8EL);
    public static final BackendId ZDSR = new BackendId(0x3D93C56C9E7F2A2EL);
    public static final BackendId ZOOM_TEXT = new BackendId(0xAE439D62DC7B1479L);
    public static final BackendId BOY_PC_READER = new BackendId(0x285ABA1C16F3300FL);
    public static final BackendId PC_TALKER = new BackendId(0x344B951962E3B835L);
    public static final BackendId SENSE_READER = new BackendId(0xED4760890B55C2F2L);
    /** Only available if enabled explicitly at build time. */
    public static final BackendId SYSTEM_ACCESS = new BackendId(0x8380F2A37B2C3EB6L);
    public static final BackendId WINDOW_EYES = new BackendId(0x9120D89908785C13L);
    public static final BackendId SPIEL = new BackendId(0x478B44F14AD3D89CL);

    /**
     * Returns true if this is the reserved "no backend" identifier.
     */
    public boolean isInvalid() {
        return id == 0L;
    }

    @Override
    public String toString() {
        return String.format("0x%016X", id);
    }
}
