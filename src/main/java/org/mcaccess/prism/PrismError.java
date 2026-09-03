package org.mcaccess.prism;

import java.util.function.Function;

public enum PrismError {
    OK(0, null),
    NOT_INITIALIZED(1, PrismException.NotInitialized::new),
    INVALID_PARAM(2, PrismException.InvalidParam::new),
    NOT_IMPLEMENTED(3, PrismException.NotImplemented::new),
    NO_VOICES(4, PrismException.NoVoices::new),
    VOICE_NOT_FOUND(5, PrismException.VoiceNotFound::new),
    SPEAK_FAILURE(6, PrismException.SpeakFailure::new),
    MEMORY_FAILURE(7, PrismException.MemoryFailure::new),
    RANGE_OUT_OF_BOUNDS(8, PrismException.RangeOutOfBounds::new),
    INTERNAL(9, PrismException.Internal::new),
    NOT_SPEAKING(10, PrismException.NotSpeaking::new),
    NOT_PAUSED(11, PrismException.NotPaused::new),
    ALREADY_PAUSED(12, PrismException.AlreadyPaused::new),
    INVALID_UTF8(13, PrismException.InvalidUtf8::new),
    INVALID_OPERATION(14, PrismException.InvalidOperation::new),
    ALREADY_INITIALIZED(15, PrismException.AlreadyInitialized::new),
    BACKEND_NOT_AVAILABLE(16, PrismException.BackendNotAvailable::new),
    UNKNOWN(17, PrismException.Unknown::new),
    INVALID_AUDIO_FORMAT(18, PrismException.InvalidAudioFormat::new),
    INTERNAL_BACKEND_LIMIT_EXCEEDED(19, PrismException.InternalBackendLimitExceeded::new),
    BACKEND_ENTERED_UNDEFINED_STATE(20, PrismException.BackendEnteredUndefinedState::new),
    LIBRARY_LOAD_FAILED(21, PrismException.LibraryLoadFailed::new),
    LIBRARY_INVALID(22, PrismException.LibraryInvalid::new),
    INCOMPATIBLE_ABI(23, PrismException.IncompatibleAbi::new);

    private final int code;
    private final Function<String, PrismException> factory;

    PrismError(int code, Function<String, PrismException> factory) {
        this.code = code;
        this.factory = factory;
    }

    /**
     * Gets the raw integer error code corresponding to the native C enum value.
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns true if this error represents successful operation (PRISM_OK).
     */
    public boolean isSuccess() {
        return this == OK;
    }

    /**
     * Creates the exception type that represents this error.
     *
     * @param message The human-readable description, normally from prism_error_string.
     * @return A new exception of the subtype matching this error.
     * @throws IllegalStateException if called on {@link #OK}, which is not an error.
     */
    public PrismException newException(String message) {
        if (factory == null) {
            throw new IllegalStateException("PRISM_OK does not represent an error");
        }
        return factory.apply(message);
    }

    /**
     * Cached because {@link #values()} clones its array on every call, and this is on the path of every native result that is not OK.
     */
    private static final PrismError[] BY_CODE = values();

    /**
     * Converts a raw native integer error code into a type-safe PrismError.
     * <p>
     * the C enum is contiguous, so it can be indexed. Newer values will throw unknown.
     *
     * @param code The integer error code returned from a native C function.
     * @return The corresponding PrismError, or {@link #UNKNOWN} if the code is unrecognised.
     */
    public static PrismError fromCode(int code) {
        return (code >= 0 && code < BY_CODE.length) ? BY_CODE[code] : UNKNOWN;
    }
}
