package org.mcaccess.prism;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrismExceptionTest {

    @Test
    void testNoErrorOnZero() {
        // Should not throw
        PrismException.throwIfError(0);
    }

    @Test
    void testThrowIfErrorCases() {
        assertThatThrownBy(() -> PrismException.throwIfError(1))
                .isInstanceOf(PrismException.NotInitialized.class);
        assertThatThrownBy(() -> PrismException.throwIfError(2))
                .isInstanceOf(PrismException.InvalidParam.class);
        assertThatThrownBy(() -> PrismException.throwIfError(3))
                .isInstanceOf(PrismException.NotImplemented.class);
        assertThatThrownBy(() -> PrismException.throwIfError(4))
                .isInstanceOf(PrismException.NoVoices.class);
        assertThatThrownBy(() -> PrismException.throwIfError(5))
                .isInstanceOf(PrismException.VoiceNotFound.class);
        assertThatThrownBy(() -> PrismException.throwIfError(6))
                .isInstanceOf(PrismException.SpeakFailure.class);
        assertThatThrownBy(() -> PrismException.throwIfError(7))
                .isInstanceOf(PrismException.MemoryFailure.class);
        assertThatThrownBy(() -> PrismException.throwIfError(8))
                .isInstanceOf(PrismException.RangeOutOfBounds.class);
        assertThatThrownBy(() -> PrismException.throwIfError(9))
                .isInstanceOf(PrismException.Internal.class);
        assertThatThrownBy(() -> PrismException.throwIfError(10))
                .isInstanceOf(PrismException.NotSpeaking.class);
        assertThatThrownBy(() -> PrismException.throwIfError(11))
                .isInstanceOf(PrismException.NotPaused.class);
        assertThatThrownBy(() -> PrismException.throwIfError(12))
                .isInstanceOf(PrismException.AlreadyPaused.class);
        assertThatThrownBy(() -> PrismException.throwIfError(13))
                .isInstanceOf(PrismException.InvalidUtf8.class);
        assertThatThrownBy(() -> PrismException.throwIfError(14))
                .isInstanceOf(PrismException.InvalidOperation.class);
        assertThatThrownBy(() -> PrismException.throwIfError(15))
                .isInstanceOf(PrismException.AlreadyInitialized.class);
        assertThatThrownBy(() -> PrismException.throwIfError(16))
                .isInstanceOf(PrismException.BackendNotAvailable.class);
        assertThatThrownBy(() -> PrismException.throwIfError(17))
                .isInstanceOf(PrismException.Unknown.class);
        assertThatThrownBy(() -> PrismException.throwIfError(18))
                .isInstanceOf(PrismException.InvalidAudioFormat.class);
        assertThatThrownBy(() -> PrismException.throwIfError(19))
                .isInstanceOf(PrismException.InternalBackendLimitExceeded.class);
        assertThatThrownBy(() -> PrismException.throwIfError(20))
                .isInstanceOf(PrismException.BackendEnteredUndefinedState.class);
        assertThatThrownBy(() -> PrismException.throwIfError(21))
                .isInstanceOf(PrismException.LibraryLoadFailed.class);
        assertThatThrownBy(() -> PrismException.throwIfError(22))
                .isInstanceOf(PrismException.LibraryInvalid.class);
        assertThatThrownBy(() -> PrismException.throwIfError(23))
                .isInstanceOf(PrismException.IncompatibleAbi.class);
    }
}
