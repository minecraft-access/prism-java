package org.mcaccess.prism;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrismErrorTest {

    @Test
    void codeMatchesOrdinal() {
        for (PrismError error : PrismError.values()) {
            assertThat(error.getCode())
                    .as("%s must keep code == ordinal, since fromCode indexes by code", error)
                    .isEqualTo(error.ordinal());
        }
    }

    @Test
    void fromCodeRoundTripsEveryConstant() {
        for (PrismError error : PrismError.values()) {
            assertThat(PrismError.fromCode(error.getCode())).isSameAs(error);
        }
    }

    @Test
    void fromCodeClampsUnrecognisedCodes() {
        assertThat(PrismError.fromCode(-1)).isSameAs(PrismError.UNKNOWN);
        assertThat(PrismError.fromCode(PrismError.values().length)).isSameAs(PrismError.UNKNOWN);
        assertThat(PrismError.fromCode(9999)).isSameAs(PrismError.UNKNOWN);
    }

    @Test
    void everyErrorProducesADistinctExceptionType() {
        for (PrismError error : PrismError.values()) {
            if (error.isSuccess()) {
                continue;
            }
            PrismException thrown = error.newException("boom");
            assertThat(thrown.getMessage()).isEqualTo("boom");
            assertThat(thrown.getClass())
                    .as("%s must map to its own exception subtype", error)
                    .isNotSameAs(PrismException.class);
        }
    }

    @Test
    void okIsNotAnError() {
        assertThat(PrismError.OK.isSuccess()).isTrue();
        assertThatThrownBy(() -> PrismError.OK.newException("boom"))
                .isInstanceOf(IllegalStateException.class);
    }
}
