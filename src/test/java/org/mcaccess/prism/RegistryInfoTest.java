package org.mcaccess.prism;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistryInfoTest {

    @Test
    void listsEveryRegisteredBackendWithNameAndPriority() {
        try (Context ctx = new Context()) {
            List<BackendInfo> backends = ctx.getRegisteredBackends();
            assertThat(backends).hasSize(ctx.getBackendsCount());
            assertThat(backends).allSatisfy(info -> {
                assertThat(info.name()).isNotBlank();
                assertThat(info.id().isInvalid()).isFalse();
            });
            assertThat(backends).extracting(BackendInfo::name).doesNotHaveDuplicates();
            assertThat(backends).extracting(BackendInfo::id).doesNotHaveDuplicates();
        }
    }

    @Test
    void entriesAgreeWithTheIndividualLookups() {
        try (Context ctx = new Context()) {
            for (BackendInfo info : ctx.getRegisteredBackends()) {
                assertThat(ctx.getNameOf(info.id())).isEqualTo(info.name());
                assertThat(ctx.getPriorityOf(info.id())).isEqualTo(info.priority());
                assertThat(ctx.getIdOf(info.name())).isEqualTo(info.id());
                assertThat(ctx.exists(info.id())).isTrue();
            }
        }
    }

    @Test
    void theListIsImmutable() {
        try (Context ctx = new Context()) {
            List<BackendInfo> backends = ctx.getRegisteredBackends();
            assertThatThrownBy(() -> backends.add(new BackendInfo(BackendId.NVDA, "x", 1)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    void aClosedContextRejectsEnumeration() {
        Context ctx = new Context();
        ctx.close();
        assertThatThrownBy(ctx::getRegisteredBackends).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void loadFailureIsNullWhenTheLibraryLoaded() {
        assertThat(Prism.isAvailable()).isTrue();
        assertThat(Prism.getLoadFailure()).isNull();
    }
}
