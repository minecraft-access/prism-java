package org.mcaccess.prism;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BackendConcurrencyTest {

    private static final int THREADS = 8;
    private static final int ITERATIONS = 400;

    @Test
    void oneBackendSurvivesConcurrentIndependentOperations() throws Exception {
        try (Context ctx = new Context(); Backend backend = ctx.acquireBest()) {
            long features = backend.getFeatures();
            runConcurrently(backend, backend, features);
        }
    }

    @Test
    void twoHandlesToTheSameCachedInstanceSurviveConcurrentUse() throws Exception {
        try (Context ctx = new Context();
                Backend a = ctx.acquireBest();
                Backend b = ctx.acquireBest()) {
            assertThat(a.getName()).isEqualTo(b.getName());
            runConcurrently(a, b, a.getFeatures());
        }
    }

    private void runConcurrently(Backend first, Backend second, long features) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        try {
            List<Future<?>> futures = new java.util.ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                Backend target = (t % 2 == 0) ? first : second;
                futures.add(pool.submit((Callable<Void>) () -> {
                    start.await();
                    for (int i = 0; i < ITERATIONS; i++) {
                        try {
                            target.getName();
                            target.getFeatures();
                            if (BackendFeature.SUPPORTS_IS_SPEAKING.isSupportedBy(features)) {
                                target.isSpeaking();
                            }
                            if (BackendFeature.SUPPORTS_GET_VOLUME.isSupportedBy(features)) {
                                target.getVolume();
                            }
                            if (BackendFeature.SUPPORTS_GET_RATE.isSupportedBy(features)) {
                                target.getRate();
                            }
                            if (BackendFeature.SUPPORTS_COUNT_VOICES.isSupportedBy(features)) {
                                target.getVoicesCount();
                            }
                        } catch (PrismException expected) {
                            // A backend may legitimately refuse an operation; only crashes and races matter here.
                        } catch (Throwable t2) {
                            failures.add(t2);
                            return null;
                        }
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(failures).isEmpty();
    }

    @Test
    void closeIsIdempotentUnderConcurrency() throws Exception {
        try (Context ctx = new Context()) {
            Backend backend = ctx.acquireBest();
            ExecutorService pool = Executors.newFixedThreadPool(THREADS);
            CountDownLatch start = new CountDownLatch(1);
            try {
                List<Future<?>> futures = new java.util.ArrayList<>();
                for (int t = 0; t < THREADS; t++) {
                    futures.add(pool.submit((Callable<Void>) () -> {
                        start.await();
                        backend.close();
                        return null;
                    }));
                }
                start.countDown();
                for (Future<?> f : futures) {
                    f.get(30, TimeUnit.SECONDS);
                }
            } finally {
                pool.shutdownNow();
                assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
            }
            assertThat(backend.isClosed()).isTrue();
        }
    }
}
