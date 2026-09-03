package org.mcaccess.prism;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrismLogTest {

    @AfterEach
    void detach() {
        PrismLog.clearListener();
        PrismLog.setLevel(PrismLog.Level.NONE);
    }

    @Test
    void levelCodesMatchOrdinals() {
        for (PrismLog.Level level : PrismLog.Level.values()) {
            assertThat(level.getCode()).isEqualTo(level.ordinal());
        }
    }

    @Test
    void unrecognisedLevelDegradesToNone() {
        assertThat(PrismLog.Level.fromCode(-1)).isSameAs(PrismLog.Level.NONE);
        assertThat(PrismLog.Level.fromCode(99)).isSameAs(PrismLog.Level.NONE);
        for (PrismLog.Level level : PrismLog.Level.values()) {
            assertThat(PrismLog.Level.fromCode(level.getCode())).isSameAs(level);
        }
    }

    @Test
    void setLevelReturnsThePreviousLevel() {
        PrismLog.setLevel(PrismLog.Level.WARN);
        assertThat(PrismLog.setLevel(PrismLog.Level.DEBUG)).isSameAs(PrismLog.Level.WARN);
        assertThat(PrismLog.setLevel(PrismLog.Level.NONE)).isSameAs(PrismLog.Level.DEBUG);
    }

    @Test
    void messagesRoundTripThroughTheListener() throws Exception {
        CountDownLatch seen = new CountDownLatch(1);
        List<String> captured = new CopyOnWriteArrayList<>();
        PrismLog.setListener((level, source, message) -> {
            if ("junit".equals(source)) {
                captured.add(level + "|" + source + "|" + message);
                seen.countDown();
            }
        });
        PrismLog.setLevel(PrismLog.Level.TRACE);

        PrismLog.log(PrismLog.Level.WARN, "junit", "hello from the test");
        PrismLog.flush();

        assertThat(seen.await(5, TimeUnit.SECONDS)).as("listener must receive the message").isTrue();
        assertThat(captured).contains("WARN|junit|hello from the test");
    }

    @Test
    void clearListenerStopsDelivery() throws Exception {
        CountDownLatch first = new CountDownLatch(1);
        PrismLog.setListener((level, source, message) -> {
            if ("junit".equals(source)) {
                first.countDown();
            }
        });
        PrismLog.setLevel(PrismLog.Level.TRACE);
        PrismLog.log(PrismLog.Level.INFO, "junit", "before");
        PrismLog.flush();
        assertThat(first.await(5, TimeUnit.SECONDS)).isTrue();

        List<String> afterClear = new CopyOnWriteArrayList<>();
        PrismLog.clearListener();
        PrismLog.log(PrismLog.Level.INFO, "junit", "after");
        PrismLog.flush();
        Thread.sleep(200);
        assertThat(afterClear).isEmpty();
    }

    @Test
    void aThrowingListenerDoesNotEscapeIntoPrism() throws Exception {
        CountDownLatch called = new CountDownLatch(1);
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> { });
        try {
            PrismLog.setListener((level, source, message) -> {
                called.countDown();
                throw new IllegalStateException("listener blew up");
            });
            PrismLog.setLevel(PrismLog.Level.TRACE);
            PrismLog.log(PrismLog.Level.ERROR, "junit", "boom");
            PrismLog.flush();
            assertThat(called.await(5, TimeUnit.SECONDS)).isTrue();

            // PRISM's logging thread must still be alive and delivering.
            PrismLog.log(PrismLog.Level.INFO, "junit", "still here");
            PrismLog.flush();
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous);
        }
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThatThrownBy(() -> PrismLog.setLevel(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PrismLog.log(null, "s", "m")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PrismLog.log(PrismLog.Level.INFO, null, "m")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PrismLog.log(PrismLog.Level.INFO, "s", null)).isInstanceOf(NullPointerException.class);
    }
}
