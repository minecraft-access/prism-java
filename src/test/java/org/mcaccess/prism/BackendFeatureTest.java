package org.mcaccess.prism;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BackendFeatureTest {

    @Test
    void eachFeatureIsNamedByItsOwnBitAlone() {
        for (BackendFeature feature : BackendFeature.values()) {
            assertThat(BackendFeature.decode(feature.getMask()))
                    .as("%s must decode to exactly itself", feature)
                    .containsExactly(feature);
        }
    }

    @Test
    void masksAreDistinctAndSingleBit() {
        long seen = 0L;
        for (BackendFeature feature : BackendFeature.values()) {
            long mask = feature.getMask();
            assertThat(Long.bitCount(mask)).as("%s must occupy one bit", feature).isEqualTo(1);
            assertThat(seen & mask).as("%s must not reuse another feature's bit", feature).isZero();
            seen |= mask;
        }
    }

    @Test
    void bitOneStaysReserved() {
        assertThat(BackendFeature.decode(1L << 1)).isEmpty();
    }

    @Test
    void emptyMaskNamesNothing() {
        assertThat(BackendFeature.decode(0L)).isEmpty();
        for (BackendFeature feature : BackendFeature.values()) {
            assertThat(feature.isSupportedBy(0L)).isFalse();
        }
    }

    @Test
    void fullMaskNamesEverything() {
        long all = 0L;
        for (BackendFeature feature : BackendFeature.values()) {
            all |= feature.getMask();
        }
        assertThat(BackendFeature.decode(all))
                .isEqualTo(EnumSet.allOf(BackendFeature.class));
    }

    @Test
    void unrecognisedBitsAreIgnored() {
        long unknown = 1L << 40;
        assertThat(BackendFeature.decode(unknown)).isEmpty();
        assertThat(BackendFeature.decode(BackendFeature.SUPPORTS_SPEAK.getMask() | unknown))
                .containsExactly(BackendFeature.SUPPORTS_SPEAK);
    }
}
