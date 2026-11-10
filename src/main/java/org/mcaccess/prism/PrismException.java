package org.mcaccess.prism;

import org.mcaccess.prism.natives.NativeLoader;
import org.mcaccess.prism.natives.prism_h;

import java.lang.foreign.MemorySegment;

/**
 * Base runtime exception for errors returned by the PRISM library.
 */
public sealed class PrismException extends RuntimeException {
    /** Numeric PRISM error code. */
    private final int code;

    /**
     * Constructs a {@link PrismException} with the given error message.
     *
     * @param message error description
     */
    public PrismException(String message) {
        this(message, prism_h.PRISM_ERROR_UNKNOWN());
    }

    /**
     * Constructs a {@link PrismException} with the given error message and numeric PRISM error code.
     *
     * @param message error description
     * @param code    numeric PRISM error code
     */
    public PrismException(String message, int code) {
        super(message);
        this.code = code;
    }

    /**
     * Gets the numeric PRISM error code associated with this exception.
     *
     * @return numeric PRISM error code
     */
    public int getCode() {
        return code;
    }

    /**
     * Throws the corresponding {@link PrismException} subclass if {@code result} is not {@code PRISM_OK}.
     *
     * @param result numeric PRISM error code returned by a native call
     */
    public static void throwIfError(int result) {
        if (result == prism_h.PRISM_OK()) {
            return;
        }
        NativeLoader.load();
        MemorySegment msgPtr = prism_h.prism_error_string(result);
        String message = Backend.readCString(msgPtr);
        if (message.isEmpty()) {
            message = "Unknown PRISM error (code " + result + ")";
        }

        throw switch (result) {
            case 1 -> new NotInitialized(message);
            case 2 -> new InvalidParam(message);
            case 3 -> new NotImplemented(message);
            case 4 -> new NoVoices(message);
            case 5 -> new VoiceNotFound(message);
            case 6 -> new SpeakFailure(message);
            case 7 -> new MemoryFailure(message);
            case 8 -> new RangeOutOfBounds(message);
            case 9 -> new Internal(message);
            case 10 -> new NotSpeaking(message);
            case 11 -> new NotPaused(message);
            case 12 -> new AlreadyPaused(message);
            case 13 -> new InvalidUtf8(message);
            case 14 -> new InvalidOperation(message);
            case 15 -> new AlreadyInitialized(message);
            case 16 -> new BackendNotAvailable(message);
            case 17 -> new Unknown(message);
            case 18 -> new InvalidAudioFormat(message);
            case 19 -> new InternalBackendLimitExceeded(message);
            case 20 -> new BackendEnteredUndefinedState(message);
            case 21 -> new LibraryLoadFailed(message);
            case 22 -> new LibraryInvalid(message);
            case 23 -> new IncompatibleAbi(message);
            default -> new PrismException(message, result);
        };
    }

    /**
     * The backend was not initialized; {@code prism_backend_initialize} was not called or failed.
     */
    public static final class NotInitialized extends PrismException {
        /**
         * Constructs a {@link NotInitialized} exception.
         *
         * @param message error description
         */
        public NotInitialized(String message) {
            super(message, prism_h.PRISM_ERROR_NOT_INITIALIZED());
        }
    }

    /**
     * An invalid parameter was passed to a function.
     */
    public static final class InvalidParam extends PrismException {
        /**
         * Constructs an {@link InvalidParam} exception.
         *
         * @param message error description
         */
        public InvalidParam(String message) {
            super(message, prism_h.PRISM_ERROR_INVALID_PARAM());
        }
    }

    /**
     * The operation is not supported by the selected backend.
     */
    public static final class NotImplemented extends PrismException {
        /**
         * Constructs a {@link NotImplemented} exception.
         *
         * @param message error description
         */
        public NotImplemented(String message) {
            super(message, prism_h.PRISM_ERROR_NOT_IMPLEMENTED());
        }
    }

    /**
     * No voices are available for this backend.
     */
    public static final class NoVoices extends PrismException {
        /**
         * Constructs a {@link NoVoices} exception.
         *
         * @param message error description
         */
        public NoVoices(String message) {
            super(message, prism_h.PRISM_ERROR_NO_VOICES());
        }
    }

    /**
     * The specified voice was not found.
     */
    public static final class VoiceNotFound extends PrismException {
        /**
         * Constructs a {@link VoiceNotFound} exception.
         *
         * @param message error description
         */
        public VoiceNotFound(String message) {
            super(message, prism_h.PRISM_ERROR_VOICE_NOT_FOUND());
        }
    }

    /**
     * Speech synthesis failed.
     */
    public static final class SpeakFailure extends PrismException {
        /**
         * Constructs a {@link SpeakFailure} exception.
         *
         * @param message error description
         */
        public SpeakFailure(String message) {
            super(message, prism_h.PRISM_ERROR_SPEAK_FAILURE());
        }
    }

    /**
     * Memory allocation failed.
     */
    public static final class MemoryFailure extends PrismException {
        /**
         * Constructs a {@link MemoryFailure} exception.
         *
         * @param message error description
         */
        public MemoryFailure(String message) {
            super(message, prism_h.PRISM_ERROR_MEMORY_FAILURE());
        }
    }

    /**
     * A parameter value exceeded its valid range.
     */
    public static final class RangeOutOfBounds extends PrismException {
        /**
         * Constructs a {@link RangeOutOfBounds} exception.
         *
         * @param message error description
         */
        public RangeOutOfBounds(String message) {
            super(message, prism_h.PRISM_ERROR_RANGE_OUT_OF_BOUNDS());
        }
    }

    /**
     * An internal backend error occurred.
     */
    public static final class Internal extends PrismException {
        /**
         * Constructs an {@link Internal} exception.
         *
         * @param message error description
         */
        public Internal(String message) {
            super(message, prism_h.PRISM_ERROR_INTERNAL());
        }
    }

    /**
     * Attempted to stop or pause when not speaking.
     */
    public static final class NotSpeaking extends PrismException {
        /**
         * Constructs a {@link NotSpeaking} exception.
         *
         * @param message error description
         */
        public NotSpeaking(String message) {
            super(message, prism_h.PRISM_ERROR_NOT_SPEAKING());
        }
    }

    /**
     * Attempted to resume when not paused.
     */
    public static final class NotPaused extends PrismException {
        /**
         * Constructs a {@link NotPaused} exception.
         *
         * @param message error description
         */
        public NotPaused(String message) {
            super(message, prism_h.PRISM_ERROR_NOT_PAUSED());
        }
    }

    /**
     * Attempted to pause when already paused.
     */
    public static final class AlreadyPaused extends PrismException {
        /**
         * Constructs an {@link AlreadyPaused} exception.
         *
         * @param message error description
         */
        public AlreadyPaused(String message) {
            super(message, prism_h.PRISM_ERROR_ALREADY_PAUSED());
        }
    }

    /**
     * A string parameter contained invalid UTF-8.
     */
    public static final class InvalidUtf8 extends PrismException {
        /**
         * Constructs an {@link InvalidUtf8} exception.
         *
         * @param message error description
         */
        public InvalidUtf8(String message) {
            super(message, prism_h.PRISM_ERROR_INVALID_UTF8());
        }
    }

    /**
     * The operation is invalid in the current state.
     */
    public static final class InvalidOperation extends PrismException {
        /**
         * Constructs an {@link InvalidOperation} exception.
         *
         * @param message error description
         */
        public InvalidOperation(String message) {
            super(message, prism_h.PRISM_ERROR_INVALID_OPERATION());
        }
    }

    /**
     * Attempted to initialize an already-initialized backend.
     */
    public static final class AlreadyInitialized extends PrismException {
        /**
         * Constructs an {@link AlreadyInitialized} exception.
         *
         * @param message error description
         */
        public AlreadyInitialized(String message) {
            super(message, prism_h.PRISM_ERROR_ALREADY_INITIALIZED());
        }
    }

    /**
     * The backend is not available on this system.
     */
    public static final class BackendNotAvailable extends PrismException {
        /**
         * Constructs a {@link BackendNotAvailable} exception.
         *
         * @param message error description
         */
        public BackendNotAvailable(String message) {
            super(message, prism_h.PRISM_ERROR_BACKEND_NOT_AVAILABLE());
        }
    }

    /**
     * An unspecified error occurred.
     */
    public static final class Unknown extends PrismException {
        /**
         * Constructs an {@link Unknown} exception.
         *
         * @param message error description
         */
        public Unknown(String message) {
            super(message, prism_h.PRISM_ERROR_UNKNOWN());
        }
    }

    /**
     * Either the backend speech engine or backend voice have an audio format that Prism does not know how to handle,
     * or the parameters that the underlying speech engine provided to Prism were nonsensical.
     */
    public static final class InvalidAudioFormat extends PrismException {
        /**
         * Constructs an {@link InvalidAudioFormat} exception.
         *
         * @param message error description
         */
        public InvalidAudioFormat(String message) {
            super(message, prism_h.PRISM_ERROR_INVALID_AUDIO_FORMAT());
        }
    }

    /**
     * The backend possesses an internal hard ceiling as to how many instances may be instantiated at any given time,
     * and this limit would be exceeded were another to be initialized.
     */
    public static final class InternalBackendLimitExceeded extends PrismException {
        /**
         * Constructs an {@link InternalBackendLimitExceeded} exception.
         *
         * @param message error description
         */
        public InternalBackendLimitExceeded(String message) {
            super(message, prism_h.PRISM_ERROR_INTERNAL_BACKEND_LIMIT_EXCEEDED());
        }
    }

    /**
     * An error occurred when the backend was executing a function which has caused the backend to enter an undefined state.
     * The caller should re-initialize the backend from scratch.
     */
    public static final class BackendEnteredUndefinedState extends PrismException {
        /**
         * Constructs a {@link BackendEnteredUndefinedState} exception.
         *
         * @param message error description
         */
        public BackendEnteredUndefinedState(String message) {
            super(message, prism_h.PRISM_ERROR_BACKEND_ENTERED_UNDEFINED_STATE());
        }
    }

    /**
     * Failed to load a native plugin library.
     */
    public static final class LibraryLoadFailed extends PrismException {
        /**
         * Constructs a {@link LibraryLoadFailed} exception.
         *
         * @param message error description
         */
        public LibraryLoadFailed(String message) {
            super(message, prism_h.PRISM_ERROR_LIBRARY_LOAD_FAILED());
        }
    }

    /**
     * A native plugin library is invalid or missing required symbols.
     */
    public static final class LibraryInvalid extends PrismException {
        /**
         * Constructs a {@link LibraryInvalid} exception.
         *
         * @param message error description
         */
        public LibraryInvalid(String message) {
            super(message, prism_h.PRISM_ERROR_LIBRARY_INVALID());
        }
    }

    /**
     * A native plugin has an incompatible ABI version.
     */
    public static final class IncompatibleAbi extends PrismException {
        /**
         * Constructs an {@link IncompatibleAbi} exception.
         *
         * @param message error description
         */
        public IncompatibleAbi(String message) {
            super(message, prism_h.PRISM_ERROR_INCOMPATIBLE_ABI());
        }
    }
}
