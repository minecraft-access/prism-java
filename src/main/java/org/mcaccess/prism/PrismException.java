package org.mcaccess.prism;

import org.mcaccess.prism.natives.prism_h;

public sealed class PrismException extends RuntimeException {
    private PrismException(String message) {
        super(message);
    }

    static void throwIfError(int result) {
        if (result == prism_h.PRISM_OK()) {
            return;
        }
        String message = prism_h.prism_error_string(result).getString(0);
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
            default -> new PrismException(message);
        };
    }

    /**
     * The backend was not initialized; {@code prism_backend_initialize} was not called or failed.
     */
    public static final class NotInitialized extends PrismException {
        private NotInitialized(String message) {
            super(message);
        }
    }

    /**
     * An invalid parameter was passed to a function.
     */
    public static final class InvalidParam extends PrismException {
        private InvalidParam(String message) {
            super(message);
        }
    }

    /**
     * The operation is not supported by the selected backend.
     */
    public static final class NotImplemented extends PrismException {
        private NotImplemented(String message) {
            super(message);
        }
    }

    /**
     * No voices are available for this backend.
     */
    public static final class NoVoices extends PrismException {
        private NoVoices(String message) {
            super(message);
        }
    }

    /**
     * The specified voice was not found.
     */
    public static final class VoiceNotFound extends PrismException {
        private VoiceNotFound(String message) {
            super(message);
        }
    }

    /**
     * Speech synthesis failed.
     */
    public static final class SpeakFailure extends PrismException {
        private SpeakFailure(String message) {
            super(message);
        }
    }

    /**
     * Memory allocation failed.
     */
    public static final class MemoryFailure extends PrismException {
        private MemoryFailure(String message) {
            super(message);
        }
    }

    /**
     * A parameter value exceeded its valid range.
     */
    public static final class RangeOutOfBounds extends PrismException {
        private RangeOutOfBounds(String message) {
            super(message);
        }
    }

    /**
     * An internal backend error occurred.
     */
    public static final class Internal extends PrismException {
        private Internal(String message) {
            super(message);
        }
    }

    /**
     * Attempted to stop or pause when not speaking.
     */
    public static final class NotSpeaking extends PrismException {
        private NotSpeaking(String message) {
            super(message);
        }
    }

    /**
     * Attempted to resume when not paused.
     */
    public static final class NotPaused extends PrismException {
        private NotPaused(String message) {
            super(message);
        }
    }

    /**
     * Attempted to pause when already paused.
     */
    public static final class AlreadyPaused extends PrismException {
        private AlreadyPaused(String message) {
            super(message);
        }
    }

    /**
     * A string parameter contained invalid UTF-8.
     */
    public static final class InvalidUtf8 extends PrismException {
        private InvalidUtf8(String message) {
            super(message);
        }
    }

    /**
     * The operation is invalid in the current state.
     */
    public static final class InvalidOperation extends PrismException {
        private InvalidOperation(String message) {
            super(message);
        }
    }

    /**
     * Attempted to initialize an already-initialized backend.
     */
    public static final class AlreadyInitialized extends PrismException {
        private AlreadyInitialized(String message) {
            super(message);
        }
    }

    /**
     * The backend is not available on this system.
     */
    public static final class BackendNotAvailable extends PrismException {
        private BackendNotAvailable(String message) {
            super(message);
        }
    }

    /**
     * An unspecified error occurred.
     */
    public static final class Unknown extends PrismException {
        private Unknown(String message) {
            super(message);
        }
    }

    /**
     * Either the backend speech engine or backend voice have an audio format that Prism does not know how to handle,
     * or the parameters that the underlying speech engine provided to Prism were nonsensical.
     */
    public static final class InvalidAudioFormat extends PrismException {
        private InvalidAudioFormat(String message) {
            super(message);
        }
    }

    /**
     * The backend possesses an internal hard ceiling as to how many instances may be instantiated at any given time,
     * and this limit would be exceeded were another to be initialized.
     */
    public static final class InternalBackendLimitExceeded extends PrismException {
        private InternalBackendLimitExceeded(String message) {
            super(message);
        }
    }

    /**
     * An error occurred when the backend was executing a function which has caused the backend to enter an undefined state.
     * The caller should re-initialize the backend from scratch.
     */
    public static final class BackendEnteredUndefinedState extends PrismException {
        private BackendEnteredUndefinedState(String message) {
            super(message);
        }
    }
}
